package com.alltheducks.oauth2.jersey.cache;

import io.vertx.ext.auth.User;

import java.util.List;
import java.util.Optional;

public class LayeredUserCache extends ExpiringUserCache {

    private final List<ExpiringUserCache> layers;

    public LayeredUserCache(final List<ExpiringUserCache> layers) {
        super();
        this.layers = layers;
    }

    @Override
    public void cacheUser(User user) {
        for (final ExpiringUserCache layer : layers) {
            layer.cacheUser(user);
        }
    }

    @Override
    public Optional<User> getUser() {
        return layers.stream()
                .map(ExpiringUserCache::getUser)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public void clearUser() {
        layers.reversed().forEach(ExpiringUserCache::clearUser);
    }

    @Override
    protected Optional<User> getMaybeExpiredUser() {
        // Purposefully not implemented as "getUser" is overridden
        throw new RuntimeException("Not Implemented");
    }

}
