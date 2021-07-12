package com.urlshortener.persistence;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.urlshortener.constants.ErrorConstants;
import com.urlshortener.model.ServiceError;
import com.urlshortener.model.UrlMappingDynamoDbItem;
import com.urlshortener.model.UrlShortenerResponse;
import org.apache.logging.log4j.util.Strings;

import java.util.Objects;
import java.util.UUID;

public class DynamoDBDataManager {
    private final DynamoDBMapper mapper;

    public DynamoDBDataManager() {
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        mapper = new DynamoDBMapper(dynamoDBClient);
    }

    public String getLongUrl(String shortString) throws IllegalArgumentException {
        UrlMappingDynamoDbItem urlMappingDynamoDbItem = mapper.load(UrlMappingDynamoDbItem.builder().shortString(shortString).build());
        if (Objects.isNull(urlMappingDynamoDbItem)) {
            throw new IllegalArgumentException(String.format("HashKey %s not found", shortString));
        }
        return urlMappingDynamoDbItem.getLongUrl();
    }

    public UrlShortenerResponse createShortUrl(UrlMappingDynamoDbItem urlMappingDynamoDbItem) {
        String shortUrl = urlMappingDynamoDbItem.getShortString();
        UrlMappingDynamoDbItem existingUrlMappingDynamoDbItem;
        if (Strings.isEmpty(shortUrl)) {
            do {
                final String uuid = UUID.randomUUID().toString().replace("-", "");
                shortUrl = uuid.substring(0, 7);
                urlMappingDynamoDbItem.setShortString(shortUrl);
                existingUrlMappingDynamoDbItem = mapper.load(UrlMappingDynamoDbItem.builder().shortString(shortUrl).build());
            } while (!Objects.isNull(existingUrlMappingDynamoDbItem));
        } else {
            existingUrlMappingDynamoDbItem = mapper.load(UrlMappingDynamoDbItem.builder().shortString(shortUrl).build());
        }
        if (Objects.isNull(existingUrlMappingDynamoDbItem)) {
            mapper.save(urlMappingDynamoDbItem);
            return UrlShortenerResponse.builder()
                    .hasError(false)
                    .shortUrl(shortUrl)
                    .build();
        } else {
            ServiceError serviceError = ServiceError.builder()
                    .code(ErrorConstants.SHORT_URL_ALREADY_EXISTS_CODE)
                    .message(ErrorConstants.SHORT_URL_ALREADY_EXISTS_MESSAGE)
                    .build();

            return UrlShortenerResponse.builder()
                    .hasError(true)
                    .serviceError(serviceError)
                    .build();
        }
    }
}
