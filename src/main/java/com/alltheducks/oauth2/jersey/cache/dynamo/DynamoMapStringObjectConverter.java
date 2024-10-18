package com.alltheducks.oauth2.jersey.cache.dynamo;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

public class DynamoMapStringObjectConverter implements AttributeConverter<Map<String, Object>> {

        @Override
        public AttributeValue transformFrom(final Map<String, Object> input) {
            if (input == null) {
                return AttributeValue.builder().m(Collections.emptyMap()).build();
            }

            final var map = new HashMap<String, AttributeValue>();
            for (final var entry : input.entrySet()) {
                map.put(entry.getKey(), toAttributeValue(entry.getValue()));
            }

            return AttributeValue.builder().m(map).build();
        }

        @Override
        public Map<String, Object> transformTo(final AttributeValue attributeValue) {
            final var attributeMap = attributeValue.m();
            final var result = new HashMap<String, Object>();

            for (final var entry : attributeMap.entrySet()) {
                result.put(entry.getKey(), fromAttributeValue(entry.getValue()));
            }

            return result;
        }

        @Override
        public EnhancedType<Map<String, Object>> type() {
            return EnhancedType.mapOf(String.class, Object.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.M;  // "M" stands for "Map" in DynamoDB types
        }

        private AttributeValue toAttributeValue(final Object value) {
            if (value instanceof String) {
                return AttributeValue.builder().s((String) value).build();
            } else if (value instanceof Number) {
                return AttributeValue.builder().n(value.toString()).build();
            } else if (value instanceof Boolean) {
                return AttributeValue.builder().bool((Boolean) value).build();
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                final var mapValue = (Map<String, Object>) value;
                return transformFrom(mapValue);  // Recursively handle maps
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                final var listValue = (List<Object>) value;
                final var attributeList = new ArrayList<AttributeValue>();
                for (final var item : listValue) {
                    attributeList.add(toAttributeValue(item));  // Recursively handle lists
                }
                return AttributeValue.builder().l(attributeList).build();
            } else {
                throw new IllegalArgumentException("Unsupported type: " + value.getClass());
            }
        }

        private Object fromAttributeValue(final AttributeValue attributeValue) {
            if (attributeValue.s() != null) {
                return attributeValue.s();
            } else if (attributeValue.n() != null) {
                return Double.valueOf(attributeValue.n());
            } else if (attributeValue.bool() != null) {
                return attributeValue.bool();
            } else if (attributeValue.m() != null) {
                return transformTo(attributeValue);  // Recursively handle maps
            } else if (attributeValue.l() != null) {
                final var attributeList = attributeValue.l();
                final var list = new ArrayList<Object>();
                for (final var item : attributeList) {
                    list.add(fromAttributeValue(item));  // Recursively handle lists
                }
                return list;
            } else {
                throw new IllegalArgumentException("Unsupported AttributeValue: " + attributeValue);
            }
        }
    }