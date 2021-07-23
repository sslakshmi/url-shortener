package com.urlshortener.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "urlMapping")
public class UrlMappingDynamoDbItem {
    @DynamoDBHashKey
    private String shortString;
    @DynamoDBAttribute
    private String longUrl;
    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "createdByIndex")
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "createdByIndex")
    private String createdBy;

    static public class LocalDateTimeConverter implements DynamoDBTypeConverter<String, LocalDateTime> {

        @Override
        public String convert( final LocalDateTime time ) {

            return time.toString();
        }

        @Override
        public LocalDateTime unconvert( final String stringValue ) {

            return LocalDateTime.parse(stringValue);
        }
    }
}