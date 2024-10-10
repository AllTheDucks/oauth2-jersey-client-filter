# Jersey OAuth2 Client Filter

This project provides a Jersey client filter that handles OAuth2 access tokens using the [Vert.x](https://vertx.io/docs/vertx-auth-oauth2/java/) library. It currently supports the Client Credentials flow and handles automatic token refreshing when the token expires.

## Install

```aiignore
 implementation 'com.alltheducks:oauth2-jersey-client-filter:XXXX'
```