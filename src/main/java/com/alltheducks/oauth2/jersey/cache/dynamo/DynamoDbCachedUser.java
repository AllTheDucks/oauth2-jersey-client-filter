package com.alltheducks.oauth2.jersey.cache.dynamo;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Map;

@DynamoDbBean
public class DynamoDbCachedUser {

    private String key;  // This should map to 'TokenKey' in DynamoDB
    private Map<String, Object> principal;
    private Map<String, Object> attributes;

    public DynamoDbCachedUser() {
    }

    public DynamoDbCachedUser(final String key, final Map<String, Object> principal, final Map<String, Object> attributes) {
        this.key = key;
        this.principal = principal;
        this.attributes = attributes;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("TokenKey")  // Maps to the partition key in DynamoDB schema
    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @DynamoDbAttribute("Principal")
    @DynamoDbConvertedBy(DynamoMapStringObjectConverter.class)
    public Map<String, Object> getPrincipal() {
        return principal;
    }

    public void setPrincipal(final Map<String, Object> principal) {
        this.principal = principal;
    }

    @DynamoDbAttribute("Attributes")
    @DynamoDbConvertedBy(DynamoMapStringObjectConverter.class)
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
