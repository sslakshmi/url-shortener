package com.urlshortener.persistence;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.urlshortener.model.UrlDetails;

import java.util.Objects;

public class DynamoDBDataManager {
    private final DynamoDBMapper mapper;

    public DynamoDBDataManager() {
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        mapper = new DynamoDBMapper(dynamoDBClient);
    }

    public String getLongUrl(String shortUrl) throws IllegalArgumentException {
        UrlDetails urlDetails = mapper.load(UrlDetails.builder().shortUrl(shortUrl).build());
        if (Objects.isNull(urlDetails)) {
            throw new IllegalArgumentException(String.format("HashKey %s not found", shortUrl));
        }
        return urlDetails.getLongUrl();
    }
}
