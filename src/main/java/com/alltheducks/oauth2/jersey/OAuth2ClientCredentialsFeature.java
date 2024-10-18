package com.alltheducks.oauth2.jersey;

import com.alltheducks.oauth2.jersey.cache.ExpiringUserCache;
import com.alltheducks.oauth2.jersey.cache.InMemoryUserCache;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class OAuth2ClientCredentialsFeature implements Feature {

    private final VertxOAuth2Client vertxOAuth2Client;
    private final ExpiringUserCache userCache;

    public OAuth2ClientCredentialsFeature(
            final String clientId,
            final String clientSecret,
            final URI tokenUri,
            final List<String> scopes,
            final ExpiringUserCache userCache
            ) {
        this.vertxOAuth2Client = new VertxOAuth2Client(tokenUri.toString(), clientId, clientSecret, scopes, OAuth2FlowType.CLIENT);
        this.userCache = userCache != null ? userCache : new InMemoryUserCache();

    }

    public OAuth2ClientCredentialsFeature(
            final String clientId,
            final String clientSecret,
            final URI tokenUri
    ) {
        this(clientId, clientSecret, tokenUri,
                Collections.emptyList(),
                null
        );
    }
    @Override
    public boolean configure(final FeatureContext context) {
        final var userContext = new UserContext(this.vertxOAuth2Client, this.userCache);
        context.register(new OAuth2ClientRequestFilter(userContext));
        context.register(new OAuth2ClientResponseFilter(userContext));
        return true;
    }
}
