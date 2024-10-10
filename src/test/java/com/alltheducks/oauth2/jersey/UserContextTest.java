package com.alltheducks.oauth2.jersey;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import jakarta.ws.rs.client.ClientRequestContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserContextTest {

    @Test
    public void testFetchUser_whenUserNotCached_expectUserFromVertxClient() throws ExecutionException, InterruptedException {
        final var clientRequestContext = mock(ClientRequestContext.class);
        final var vertxOAuth2Client = mock(VertxOAuth2Client.class);

        final var user = mock(User.class);
        when(user.principal()).thenReturn(JsonObject.of(
                "access_token", "newtoken",
                "expires_in", 300L // Mock as Long, 300 seconds (5 minutes)
        ));

        doAnswer(invocation -> {
            Handler<Future<User>> handler = invocation.getArgument(0);
            handler.handle(Future.succeededFuture(user));
            return null;
        }).when(vertxOAuth2Client).getUser(any());

        final var userContext = new UserContext(vertxOAuth2Client);

        // Call fetchUser and check the result
        final var returnedUser = userContext.fetchUser(clientRequestContext);

        assertTrue(returnedUser.isPresent());
        assertEquals("newtoken", returnedUser.get().principal().getString("access_token"));
        assertEquals(300L, returnedUser.get().principal().getLong("expires_in"));
    }

    @Test
    public void testFetchUser_whenVertxOAuth2ClientFails_expectEmptyOptional() {
        final var clientRequestContext = mock(ClientRequestContext.class);
        final var vertxOAuth2Client = mock(VertxOAuth2Client.class);

        doAnswer(invocation -> {
            Handler<Future<User>> handler = invocation.getArgument(0);
            handler.handle(Future.failedFuture("Failed to fetch user"));
            return null;
        }).when(vertxOAuth2Client).getUser(any());

        final var userContext = new UserContext(vertxOAuth2Client);

        final var returnedUser = userContext.fetchUser(clientRequestContext);

        assertFalse(returnedUser.isPresent());
    }
}
