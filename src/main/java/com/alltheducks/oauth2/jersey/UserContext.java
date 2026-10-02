package com.alltheducks.oauth2.jersey;

import com.alltheducks.oauth2.jersey.cache.ExpiringUserCache;
import com.alltheducks.oauth2.jersey.cache.InMemoryUserCache;
import io.vertx.ext.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UserContext {

    /** How long a request waits for a token before it is sent without one, rather than blocking indefinitely. */
    public static final Duration DEFAULT_TOKEN_TIMEOUT = Duration.ofSeconds(30);

    private final Logger logger = LoggerFactory.getLogger(UserContext.class);
    private final VertxOAuth2Client vertxOAuth2Client;
    private final ExpiringUserCache userCache;
    private final Duration tokenTimeout;


    public UserContext(final VertxOAuth2Client vertxOAuth2Client, final ExpiringUserCache userCache) {
        this(vertxOAuth2Client, userCache, DEFAULT_TOKEN_TIMEOUT);
    }

    public UserContext(final VertxOAuth2Client vertxOAuth2Client, final ExpiringUserCache userCache, final Duration tokenTimeout) {
        this.vertxOAuth2Client = vertxOAuth2Client;
        this.userCache = userCache != null ? userCache : new InMemoryUserCache();
        this.tokenTimeout = tokenTimeout;
    }

    public void clearUser() {
        this.userCache.clearUser();
        this.vertxOAuth2Client.clearUser();
    }

    public Optional<User> fetchUser() {
        final var user = this.userCache.getUser();
        if (user.isPresent()) {
            return user;
        }
        final var newUser = this.fetchNewUser();
        if(newUser.isEmpty()) {
            return Optional.empty();
        }
        this.userCache.cacheUser(newUser.get());
        return newUser;
    }

    private Optional<User> fetchNewUser() {
        CompletableFuture<User> userFuture = new CompletableFuture<>();

        this.vertxOAuth2Client.getUser(result -> {
            if (result.succeeded()) {
                userFuture.complete(result.result());
            } else {
                logger.error("Failed to fetch user", result.cause());
                userFuture.completeExceptionally(result.cause());
            }
        });

        try {
            return Optional.of(userFuture.get(this.tokenTimeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (final TimeoutException e) {
            logger.error("No OAuth2 token after {} ms; sending the request without one", this.tokenTimeout.toMillis());
            return Optional.empty();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching an OAuth2 token", e);
            return Optional.empty();
        } catch (final Exception e) {
            logger.error("Error fetching user", e);
            return Optional.empty();
        }
    }

}
