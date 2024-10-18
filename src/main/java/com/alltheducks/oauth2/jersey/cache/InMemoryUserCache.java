package com.alltheducks.oauth2.jersey.cache;

import io.vertx.ext.auth.User;

import java.util.Optional;

public class InMemoryUserCache extends ExpiringUserCache {

    private User user = null;

    @Override
    public void clearUser() {
        this.user = null;
    }

    @Override
    public void cacheUser(User user) {
        this.user = user;
    }

    @Override
    protected Optional<User> getMaybeExpiredUser() {
        return Optional.ofNullable(user);
    }

}

