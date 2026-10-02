package com.alltheducks.oauth2.jersey;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the feature end to end over real HTTP: a local server plays both the OAuth2 token endpoint and a
 * protected API, so the test covers the Vert.x OAuth2 client's token request as well as the Jersey filters.
 */
public class OAuth2ClientCredentialsFeatureTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";

    private HttpServer server;
    private Client client;
    private final AtomicInteger tokensIssued = new AtomicInteger();
    private final AtomicBoolean rejectNextApiCall = new AtomicBoolean();
    private final List<String> tokenRequestBodies = new CopyOnWriteArrayList<>();
    private final List<String> tokenRequestAuthorizations = new CopyOnWriteArrayList<>();

    @BeforeEach
    public void startServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/token", this::handleToken);
        this.server.createContext("/api", this::handleApi);
        this.server.start();

        final var tokenUri = URI.create("http://127.0.0.1:" + this.server.getAddress().getPort() + "/token");
        this.client = ClientBuilder.newClient()
                .register(new OAuth2ClientCredentialsFeature(CLIENT_ID, CLIENT_SECRET, tokenUri));
    }

    @AfterEach
    public void stopServer() {
        this.client.close();
        this.server.stop(0);
    }

    @Test
    public void testRequest_withClientCredentials_expectBearerTokenFromTokenEndpoint() {
        final var body = this.client.target(this.apiUri()).request().get(String.class);

        assertEquals("ok Bearer token-1", body);
        assertEquals(1, this.tokensIssued.get());
        assertTrue(this.tokenRequestBodies.getFirst().contains("grant_type=client_credentials"),
                "token request body: " + this.tokenRequestBodies.getFirst());
        final var credentialsSent = this.tokenRequestAuthorizations.getFirst().equals(this.basicCredentials())
                || this.tokenRequestBodies.getFirst().contains("client_id=" + CLIENT_ID);
        assertTrue(credentialsSent, "the token request must carry the client credentials");
    }

    @Test
    public void testSecondRequest_withUnexpiredToken_expectCachedTokenReused() {
        this.client.target(this.apiUri()).request().get(String.class);
        final var body = this.client.target(this.apiUri()).request().get(String.class);

        assertEquals("ok Bearer token-1", body);
        assertEquals(1, this.tokensIssued.get());
    }

    @Test
    public void testRequest_whenApiRejectsToken_expectNewTokenAndRetry() {
        this.client.target(this.apiUri()).request().get(String.class);
        this.rejectNextApiCall.set(true);

        final var body = this.client.target(this.apiUri()).request().get(String.class);

        assertEquals("ok Bearer token-2", body);
        assertEquals(2, this.tokensIssued.get());
    }

    @Test
    public void testPost_whenApiRejectsToken_expectRetrySendsSameHeadersAndBody() {
        this.client.target(this.apiUri()).request().get(String.class);
        this.rejectNextApiCall.set(true);

        final var body = this.client.target(this.apiUri()).request()
                .header("X-Trace", "trace-1")
                .post(Entity.text("payload"), String.class);

        assertEquals("ok Bearer token-2 trace=trace-1 body=payload", body);
        assertEquals(2, this.tokensIssued.get());
    }

    private URI apiUri() {
        return URI.create("http://127.0.0.1:" + this.server.getAddress().getPort() + "/api");
    }

    private String basicCredentials() {
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
    }

    private void handleToken(final HttpExchange exchange) throws IOException {
        this.tokenRequestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        final var authorization = exchange.getRequestHeaders().getFirst("Authorization");
        this.tokenRequestAuthorizations.add(authorization == null ? "" : authorization);
        final var token = "token-" + this.tokensIssued.incrementAndGet();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        this.respond(exchange, 200, "{\"access_token\":\"" + token + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
    }

    private void handleApi(final HttpExchange exchange) throws IOException {
        final var authorization = exchange.getRequestHeaders().getFirst("Authorization");
        final var trace = exchange.getRequestHeaders().getFirst("X-Trace");
        final var requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (this.rejectNextApiCall.getAndSet(false)) {
            this.respond(exchange, 401, "");
            return;
        }
        this.respond(exchange, 200, "ok " + authorization
                + (trace == null ? "" : " trace=" + trace)
                + (requestBody.isEmpty() ? "" : " body=" + requestBody));
    }

    private void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        final var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (final var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }
}
