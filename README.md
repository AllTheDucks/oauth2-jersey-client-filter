# Jersey OAuth2 Client Filter

This project provides a Jersey client feature that handles OAuth2 access tokens (fetching/caching/refreshing + adding Authorization header) using the [Vert.x](https://vertx.io/docs/vertx-auth-oauth2/java/) library. 

It currently supports the Client Credentials flow. Feel free to submit a PR to implement `PASSWORD` or `AUTH_CODE` flow.
## Install
This package can be imported using [JitPack](https://jitpack.io/#AllTheDucks/oauth2-jersey-client-filter) or throw GitHub Packages.

e.g.
```aiignore
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
        implementation 'com.github.AllTheDucks:oauth2-jersey-client-filter:Tag'
}
```
or
```aiignore
<dependency>
  <groupId>com.alltheducks</groupId>
  <artifactId>oauth2-jersey-client-filter</artifactId>
  <version>1.0.1</version>
</dependency>
```

`mvn install`