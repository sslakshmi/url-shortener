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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Slf4j
public class UrlShortener implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String INVALID_REQUEST = "Invalid request";
    private final DynamoDBDataManager dataManager;

    public UrlShortener() {
        this.dataManager = new DynamoDBDataManager();
    }

    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String requestBody = request.getBody();
        ObjectMapper mapper = new ObjectMapper();
        try {
            UrlMappingDynamoDbItem urlMappingDynamoDbItem = mapper.readValue(requestBody, UrlMappingDynamoDbItem.class);
            Map<String, String> claims = (Map<String, String>) request.getRequestContext().getAuthorizer().get("claims");
            String userId = claims.get("cognito:username");
            UrlShortenerResponse urlShortenerResponse = dataManager.createShortUrl(urlMappingDynamoDbItem, userId);
            if (urlShortenerResponse.getHasError()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody(mapper.writeValueAsString(urlShortenerResponse));
            } else {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withHeaders(new HashMap<String, String>() {{
                            put("Access-Control-Allow-Origin", "*");
                        }})
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
            log.error("Json processing error",jsonProcessingException);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(ErrorConstants.INTERNAL_SERVER_ERROR);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody(errorMessage);
    }
}