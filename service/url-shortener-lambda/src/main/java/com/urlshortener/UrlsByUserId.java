package com.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.model.UrlsByUserIdResponse;
import com.urlshortener.persistence.DynamoDBDataManager;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Slf4j
public class UrlsByUserId implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String INVALID_REQUEST = "Invalid request";
    private final DynamoDBDataManager dataManager;

    public UrlsByUserId() {
        this.dataManager = new DynamoDBDataManager();
    }

    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        System.out.println("logging request");
        log.info("logging request - slf4j");
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(request));
        log.info(mapper.writeValueAsString(request));
        Map<String, String> claims = (Map<String, String>) request.getRequestContext().getAuthorizer().get("claims");
        String userId = claims.get("cognito:username");
        UrlsByUserIdResponse urlsByUserIdResponse = dataManager.getShortUrls(userId);
        if (urlsByUserIdResponse.getHasError()) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(mapper.writeValueAsString(urlsByUserIdResponse));
        } else {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(new HashMap<String, String>() {{
                        put("Access-Control-Allow-Origin", "*");
                    }})
                    .withBody(mapper.writeValueAsString(urlsByUserIdResponse));
        }
    }
}