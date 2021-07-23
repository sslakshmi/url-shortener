package com.urlshortener.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class UrlsByUserIdResponse {
    private List<UrlDetails> urlDetails;
    private Boolean hasError;
    private ServiceError serviceError;

    @Builder
    @Data
    public static class UrlDetails {
        private final String shortString;
        private final String longUrl;
        private final String createdAt;

        public static List<UrlDetails> fromUrlMappingDynamoDbItem(List<UrlMappingDynamoDbItem> dynamoDbItems) {
            return dynamoDbItems.stream()
                    .map(item -> UrlDetails.builder()
                            .shortString(item.getShortString())
                            .longUrl(item.getLongUrl())
                            .createdAt(item.getCreatedAt().toString())
                            .build())
                    .collect(Collectors.toList());
        }
    }
}
