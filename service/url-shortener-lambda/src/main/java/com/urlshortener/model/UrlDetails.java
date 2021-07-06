package com.urlshortener.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@DynamoDBTable(tableName = "urlDetails")
public class UrlDetails {
    @DynamoDBHashKey
    private String shortUrl;
    private String longUrl;
}
