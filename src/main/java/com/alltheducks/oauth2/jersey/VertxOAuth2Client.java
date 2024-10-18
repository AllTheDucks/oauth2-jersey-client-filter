package com.alltheducks.oauth2.jersey;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;

import java.util.List;

public class VertxOAuth2Client {

    private final OAuth2Auth oauth2;
    private final List<String> scopes;
    private final OAuth2FlowType flowType;
    private User user;

    public VertxOAuth2Client(final String tokenUri, final String clientId, final String clientSecret, final List<String> scopes, final OAuth2FlowType flowType) {
        final var vertx = Vertx.vertx();

        final var options = new OAuth2Options()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setTokenPath(tokenUri);

        this.scopes = scopes;
        this.flowType = flowType;
        this.oauth2 = OAuth2Auth.create(vertx, options);
    }

    public void getUser(final Handler<AsyncResult<User>> handler) {
        if (this.user == null) {
            authenticateNewUser(handler);
            return;
        }

        if (this.user.expired()) {
            this.oauth2.refresh(this.user)
                    .onSuccess(refreshedUser -> {
                        this.user = refreshedUser;
                        handler.handle(Future.succeededFuture(this.user));
                    })
                    .onFailure(err -> handler.handle(Future.failedFuture(err)));
            return;
        }

        handler.handle(Future.succeededFuture(this.user));
    }

    private void authenticateNewUser(final Handler<AsyncResult<User>> handler) {
        final var credentials = createCredentials();

        this.oauth2.authenticate(credentials, result -> {
            if (result.succeeded()) {
                this.user = result.result();
                handler.handle(Future.succeededFuture(this.user));
            } else {
                handler.handle(Future.failedFuture(result.cause()));
            }
        });
    }

    private Oauth2Credentials createCredentials() {
        switch (this.flowType) {
            case CLIENT:
                return new Oauth2Credentials()
                        .setScopes(this.scopes)
                        .setFlow(this.flowType);

            // TODO: Extend this and implement PASSWORD & AUTH_CODE flows
            case PASSWORD:
            case AUTH_CODE:
                throw new RuntimeException("Not yet implemented");
            default:
                return null;
        }
    }
}
