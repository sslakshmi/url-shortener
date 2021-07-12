package com.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.constants.ErrorConstants;
import com.urlshortener.model.ServiceError;
import com.urlshortener.model.UrlMappingDynamoDbItem;
import com.urlshortener.model.UrlShortenerResponse;
import com.urlshortener.persistence.DynamoDBDataManager;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
public class UrlShortener implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String INVALID_REQUEST = "Invalid request";
    private final DynamoDBDataManager dataManager;

    public UrlShortener() {
        this.dataManager = new DynamoDBDataManager();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String requestBody = request.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            UrlMappingDynamoDbItem urlMappingDynamoDbItem = mapper.readValue(requestBody, UrlMappingDynamoDbItem.class);
            UrlShortenerResponse urlShortenerResponse = dataManager.createShortUrl(urlMappingDynamoDbItem);
            if (urlShortenerResponse.getHasError()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody(mapper.writeValueAsString(urlShortenerResponse));
            } else {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(mapper.writeValueAsString(urlShortenerResponse));
            }
        } catch (JsonProcessingException e) {
            log.info(INVALID_REQUEST, e);
            return getInvalidRequestProxyResponseEvent(mapper);
        }
    }

    private APIGatewayProxyResponseEvent getInvalidRequestProxyResponseEvent(ObjectMapper mapper) {
        ServiceError error = ServiceError.builder()
                .code(ErrorConstants.INVALID_REQUEST_CODE)
                .message(ErrorConstants.INVALID_REQUEST_MESSAGE)
                .build();
        UrlShortenerResponse urlShortenerResponse = UrlShortenerResponse.builder()
                .hasError(true)
                .serviceError(error)
                .build();
        String errorMessage;
        try {
            errorMessage = mapper.writeValueAsString(urlShortenerResponse);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error(jsonProcessingException);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(ErrorConstants.INTERNAL_SERVER_ERROR);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody(errorMessage);
    }
}