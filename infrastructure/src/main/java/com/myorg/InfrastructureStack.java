package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Attribute sortKeyAttribute = Attribute.builder()
                .name("createdAt")
                .type(AttributeType.STRING)
                .build();
        Table urlDetails = new Table(this, "urlDetails", TableProps.builder()
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .partitionKey(Attribute.builder()
                        .name("shortString")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        urlDetails.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("createdByIndex")
                .partitionKey(Attribute.builder()
                        .name("createdBy")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(sortKeyAttribute)
                .projectionType(ProjectionType.ALL)
                .build());

        List<String> lambdaPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd url-shortener-lambda " +
                        "&& mvn clean install " +
                        "&& cp /asset-input/url-shortener-lambda/target/url-shortener-lambda.jar /asset-output/"
        );

        // Defines Docker image specifications and local folder mounts
        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(lambdaPackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED);

        Function urlRedirectionLambda = new Function(this, "URLRedirectionLambda", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../service/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(lambdaPackagingInstructions)
                                .build())
                        .build()))
                .handler("com.urlshortener.UrlRedirection")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_MONTH)
                .environment(new HashMap<>(){{
                    put("DynamoDbTableName", urlDetails.getTableName());
                }})
                .build());

        RestApi restApi = RestApi.Builder.create(this, "URLShortenerAPI")
                .restApiName("URLShortenerAPI")
                .defaultCorsPreflightOptions(CorsOptions.builder().allowOrigins(Arrays.asList("*")).build())
                .build();

        LambdaIntegration urlRedirectionApiIntegration = LambdaIntegration.Builder.create(urlRedirectionLambda).build();

        //URL Redirection API resource creation
        Resource urlRedirection = Resource.Builder.create(this, "URLRedirection")
                .parent(restApi.getRoot())
                .pathPart("{shortString}")
                .build();

        //Adding integration to redirection lambda
        urlRedirection.addMethod("GET", urlRedirectionApiIntegration);

        Function urlShortenerLambda = new Function(this, "URLShortenerLambda", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../service/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(lambdaPackagingInstructions)
                                .build())
                        .build()))
                .handler("com.urlshortener.UrlShortener")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_MONTH)
                .environment(new HashMap<>(){{
                    put("DynamoDbTableName", urlDetails.getTableName());
                }})
                .build());

        UserPool urlShortenerUserPool = new UserPool(this, "UrlShortenerUserPool", UserPoolProps.builder()
                .signInAliases(SignInAliases.builder()
                        .email(true)
                        .build())
                .build());

        Authorizer authorizer = new CognitoUserPoolsAuthorizer(this, "UrlShortenerAuthorizer", CognitoUserPoolsAuthorizerProps.builder()
                .cognitoUserPools(Arrays.asList(urlShortenerUserPool))
                .identitySource("method.request.header.Authorization")
                .build());

        //Endpoint => domain/shorturl
        Resource shortUrl = Resource.Builder.create(this, "shortURL")
                .parent(restApi.getRoot())
                .pathPart("shorturl")
                .build();

        LambdaIntegration urlMappingApiIntegration = LambdaIntegration.Builder.create(urlShortenerLambda).build();

        shortUrl.addMethod("POST", urlMappingApiIntegration, MethodOptions.builder()
                .authorizer(authorizer)
                .authorizationType(AuthorizationType.COGNITO)
                .build());

        Function urlsByUserIdLambda = new Function(this, "UrlsByUserIdLambda", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../service/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(lambdaPackagingInstructions)
                                .build())
                        .build()))
                .handler("com.urlshortener.UrlsByUserId")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_MONTH)
                .environment(new HashMap<>(){{
                    put("DynamoDbTableName", urlDetails.getTableName());
                }})
                .build());

        LambdaIntegration urlsByUsrIdApiIntegration = LambdaIntegration.Builder.create(urlsByUserIdLambda).build();

        shortUrl.addMethod("GET", urlsByUsrIdApiIntegration, MethodOptions.builder()
                .authorizer(authorizer)
                .authorizationType(AuthorizationType.COGNITO)
                .build());

        urlDetails.grantReadWriteData(urlRedirectionLambda.getRole());
        urlDetails.grantReadWriteData(urlShortenerLambda.getRole());
        urlDetails.grantReadWriteData(urlsByUserIdLambda.getRole());


        new CfnOutput(this, "APILambdaARN", CfnOutputProps.builder()
                .description("API lambda function ARN")
                .value(urlRedirectionLambda.getFunctionArn())
                .build());

        new CfnOutput(this, "URLShortenerAPI-URL", CfnOutputProps.builder()
                .description("URL shortener API URL")
                .value(restApi.getUrl())
                .build());
    }
}

