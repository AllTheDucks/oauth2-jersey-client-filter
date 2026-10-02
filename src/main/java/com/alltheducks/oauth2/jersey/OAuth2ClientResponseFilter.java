package com.alltheducks.oauth2.jersey;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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

            try (final var retried = this.resend(requestContext)) {
                // Buffer the body before the retried response closes, then make this response the retried one.
                final var body = retried.readEntity(byte[].class);
                responseContext.setStatus(retried.getStatus());
                responseContext.getHeaders().clear();
                retried.getStringHeaders().forEach((name, values) -> responseContext.getHeaders().addAll(name, values));
                responseContext.setEntityStream(new ByteArrayInputStream(body == null ? new byte[0] : body));
            }
        }
    }

    /** The same request again (headers and body included); the request filter adds the new token. */
    private Response resend(final ClientRequestContext requestContext) {
        final var headers = new MultivaluedHashMap<String, Object>(requestContext.getHeaders());
        headers.remove(HttpHeaders.AUTHORIZATION);
        final var invocation = requestContext.getClient().target(requestContext.getUri())
                .request()
                .headers(headers)
                .property(TOKEN_RETRY_REQUEST_PROPERTY_KEY, true);
        if (requestContext.hasEntity()) {
            final var entity = Entity.entity(requestContext.getEntity(), requestContext.getMediaType());
            return invocation.build(requestContext.getMethod(), entity).invoke();
        }
        return invocation.build(requestContext.getMethod()).invoke();
    }
}