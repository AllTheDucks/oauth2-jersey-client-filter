package com.alltheducks.oauth2.jersey;

import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

import java.net.URI;
import java.util.List;

public class OAuth2ClientCredentialsFeature implements Feature {

    private final VertxOAuth2Client vertxOAuth2Client;

    public OAuth2ClientCredentialsFeature(
            final String clientId,
            final String clientSecret,
            final URI tokenUri,
            final List<String> scopes) {
        this.vertxOAuth2Client = new VertxOAuth2Client(tokenUri.toString(), clientId, clientSecret, scopes, OAuth2FlowType.CLIENT);
    }

    @Override
    public boolean configure(final FeatureContext context) {
        final var tokenContext = new UserContext(this.vertxOAuth2Client);
        context.register(new OAuth2ClientRequestFilter(tokenContext));
        context.register(new OAuth2ClientResponseFilter(tokenContext));
        return true;
    }
}
