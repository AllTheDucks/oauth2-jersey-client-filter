package com.alltheducks.oauth2.jersey.cache.dynamo;

import com.alltheducks.oauth2.jersey.cache.ExpiringUserCache;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.Optional;

public class DynamoDbUserCache extends ExpiringUserCache {

    private final String tokenKey;
    private final DynamoDbTable<DynamoDbCachedUser> cachedUserTable;

    public DynamoDbUserCache(final DynamoDbTable<DynamoDbCachedUser> cachedUserTable, final String tokenKey) {
        super();
        this.cachedUserTable = cachedUserTable;
        this.tokenKey = tokenKey;
    }

    @Override
    public void cacheUser(final User user) {
        this.cachedUserTable.putItem(this.mapUserToDbModel(user));
    }

    @Override
    public void clearUser() {
        this.getMaybeExpiredUser().ifPresent(user ->
                this.cachedUserTable.deleteItem(r -> r.key(k -> k.partitionValue(this.tokenKey)))
        );
    }

    @Override
    protected Optional<User> getMaybeExpiredUser() {
        final var cachedUser = this.cachedUserTable.getItem(r -> r.key(k -> k.partitionValue(this.tokenKey)));
        return Optional.ofNullable(cachedUser).map(this::mapDbModelToUser);
    }

    private DynamoDbCachedUser mapUserToDbModel(final User user) {
        final var principal = user.principal().getMap();
        final var attributes = user.attributes().getMap();
        return new DynamoDbCachedUser(this.tokenKey, principal, attributes);
    }

    private User mapDbModelToUser(final DynamoDbCachedUser authUser) {
        final var principalJsonObject = new JsonObject(authUser.getPrincipal());
        final var attributesJsonObject = new JsonObject(authUser.getAttributes());
        return User.create(principalJsonObject, attributesJsonObject);
    }

}
