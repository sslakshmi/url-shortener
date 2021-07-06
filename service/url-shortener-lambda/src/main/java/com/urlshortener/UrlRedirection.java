package com.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.urlshortener.persistence.DynamoDBDataManager;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@AllArgsConstructor
@Log4j2
public class UrlRedirection implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDBDataManager dataManager;

    public UrlRedirection() {
        this.dataManager = new DynamoDBDataManager();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String shortUrl = request.getPathParameters().get("shortUrl");
            String longUrl = dataManager.getLongUrl(shortUrl);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(302)
                    .withHeaders(Map.of("Location", longUrl));
        } catch (IllegalArgumentException e) {
            log.info("Invalid short URL", e);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Invalid short URL, please try again with a valid URL");
        }
    }
}