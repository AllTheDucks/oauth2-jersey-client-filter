package com.alltheducks.oauth2.jersey.cache;

import io.vertx.ext.auth.User;

import java.util.Optional;
import java.util.function.Predicate;

public abstract class ExpiringUserCache {

    public ExpiringUserCache() {}

    public abstract void cacheUser(User user);
    public abstract void clearUser();
    protected abstract Optional<User> getMaybeExpiredUser();

    public Optional<User> getUser() {
        return this.getMaybeExpiredUser()
                .filter(Predicate.not(this::isUserExpired));
    }

    private boolean isUserExpired(final User user) {
        return user.expired();
    }

}
