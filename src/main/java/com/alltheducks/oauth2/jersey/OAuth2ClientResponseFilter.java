package com.alltheducks.oauth2.jersey;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class OAuth2ClientResponseFilter implements ClientResponseFilter {

    private final Logger logger = LoggerFactory.getLogger(OAuth2ClientResponseFilter.class);

    private static final String TOKEN_RETRY_REQUEST_PROPERTY_KEY = "tokenretryrequest";

    private final UserContext userContext;

    public OAuth2ClientResponseFilter(final UserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) throws IOException {
        final Boolean retryRequestProperty = (Boolean) requestContext.getProperty(TOKEN_RETRY_REQUEST_PROPERTY_KEY);
        final boolean isRetryRequest = retryRequestProperty != null && retryRequestProperty;

        if (responseContext.getStatus() == 401 && !isRetryRequest) {
            logger.debug("401 Unauthorized received, attempting to fetch new token and retrying request");
            this.userContext.clearUser();
            this.userContext.fetchUser();

            final var client = requestContext.getClient();

            try (final var response = client.target(requestContext.getUri())
                    .request(responseContext.getMediaType())
                    .property(TOKEN_RETRY_REQUEST_PROPERTY_KEY, true)
                    .build(requestContext.getMethod())
                    .invoke()) {

                final var entityStream = response.readEntity(InputStream.class);
                responseContext.setEntityStream(entityStream);
            }
        }
    }
}