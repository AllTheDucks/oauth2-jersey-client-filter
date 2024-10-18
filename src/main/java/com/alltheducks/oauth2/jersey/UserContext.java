package com.alltheducks.oauth2.jersey;

import com.alltheducks.oauth2.jersey.cache.ExpiringUserCache;
import com.alltheducks.oauth2.jersey.cache.InMemoryUserCache;
import io.vertx.ext.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UserContext {

    private final Logger logger = LoggerFactory.getLogger(UserContext.class);
    private final VertxOAuth2Client vertxOAuth2Client;
    private final ExpiringUserCache userCache;


    public UserContext(final VertxOAuth2Client vertxOAuth2Client, final ExpiringUserCache userCache) {
        this.vertxOAuth2Client = vertxOAuth2Client;
        this.userCache = userCache != null ? userCache : new InMemoryUserCache();
    }

    public void clearUser() {
        this.userCache.clearUser();
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
            return Optional.of(userFuture.get());
        } catch (Exception e) {
            logger.error("Error fetching user", e);
            return Optional.empty();
        }
    }

}
