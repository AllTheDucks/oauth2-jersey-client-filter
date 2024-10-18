package com.alltheducks.oauth2.jersey;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OAuth2ClientRequestFilter implements ClientRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(OAuth2ClientRequestFilter.class);

    private final UserContext userContext;

    public OAuth2ClientRequestFilter(final UserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        final var user = this.userContext.fetchUser();
        if(user.isPresent()) {
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + user.get().principal().getString("access_token"));
        } else {
            logger.warn("No token found, therefore the request with be sent without the Authorization header.");
        }
    }

}
