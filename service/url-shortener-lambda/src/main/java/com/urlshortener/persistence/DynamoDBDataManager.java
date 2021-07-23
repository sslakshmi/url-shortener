package com.urlshortener.persistence;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.urlshortener.constants.ErrorConstants;
import com.urlshortener.model.ServiceError;
import com.urlshortener.model.UrlMappingDynamoDbItem;
import com.urlshortener.model.UrlShortenerResponse;
import com.urlshortener.model.UrlsByUserIdResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Log4j2
public class DynamoDBDataManager {
    private final DynamoDBMapper mapper;

    public DynamoDBDataManager() {
        String tableName = System.getenv("DynamoDbTableName");
        if (Strings.isEmpty(tableName)) {
            log.error("Table name not found in environment");
            throw new RuntimeException("Invalid Table Name");
        }
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                .build();
        mapper = new DynamoDBMapper(dynamoDBClient, dynamoDBMapperConfig);
    }

    public String getLongUrl(String shortString) throws IllegalArgumentException {
        UrlMappingDynamoDbItem urlMappingDynamoDbItem = mapper.load(UrlMappingDynamoDbItem.builder().shortString(shortString).build());
        if (Objects.isNull(urlMappingDynamoDbItem)) {
            throw new IllegalArgumentException(String.format("HashKey %s not found", shortString));
        }
        return urlMappingDynamoDbItem.getLongUrl();
    }

    public UrlShortenerResponse createShortUrl(UrlMappingDynamoDbItem urlMappingDynamoDbItem, String userId) {
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
            urlMappingDynamoDbItem.setCreatedBy(userId);
            urlMappingDynamoDbItem.setCreatedAt(LocalDateTime.now());
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

    public UrlsByUserIdResponse getShortUrls(String userId) {
        UrlMappingDynamoDbItem urlMappingDynamoDbItemKey = UrlMappingDynamoDbItem.builder().createdBy(userId).build();
        DynamoDBQueryExpression<UrlMappingDynamoDbItem> dynamoDBQueryExpression = new DynamoDBQueryExpression<UrlMappingDynamoDbItem>()
                .withIndexName("createdByIndex")
                .withHashKeyValues(urlMappingDynamoDbItemKey)
                .withConsistentRead(false);

        PaginatedQueryList<UrlMappingDynamoDbItem> urlMappingDynamoDbItems = mapper.query(UrlMappingDynamoDbItem.class, dynamoDBQueryExpression);

        return UrlsByUserIdResponse.builder()
                .urlDetails(UrlsByUserIdResponse.UrlDetails.fromUrlMappingDynamoDbItem(urlMappingDynamoDbItems))
                .hasError(false)
                .build();
    }
}
