package com.urlshortener;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.urlshortener.model.UrlDetails;

/**
 * Hello world!
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        System.out.println(request.getBody());
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
        UrlDetails urlDetails = UrlDetails.builder().shortUrl("hello").longUrl("test").build();
        mapper.save(urlDetails);
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Hello All");
    }
}
