package com.alltheducks.oauth2.jersey;

import io.vertx.ext.auth.User;
import jakarta.ws.rs.client.ClientRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UserContext {

    private final Logger logger = LoggerFactory.getLogger(UserContext.class);
    private final VertxOAuth2Client vertxOAuth2Client;

    public UserContext(final VertxOAuth2Client vertxOAuth2Client) {
        this.vertxOAuth2Client = vertxOAuth2Client;
    }

    public Optional<User> fetchUser(final ClientRequestContext requestContext) {
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
