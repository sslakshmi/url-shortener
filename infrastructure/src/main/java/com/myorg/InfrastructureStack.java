package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

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

        Function apiLambda = new Function(this, "APILambda", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../service/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(lambdaPackagingInstructions)
                                .build())
                        .build()))
                .handler("com.urlshortener.App")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_MONTH)
                .build());

        RestApi restApi = RestApi.Builder.create(this, "URLShortenerAPI")
                .restApiName("URLShortenerAPI")
                .build();

        LambdaIntegration getApiIntergration = LambdaIntegration.Builder.create(apiLambda).build();

        restApi.getRoot().addMethod("GET", getApiIntergration);

        Table urlDetails = new Table(this, "urlDetails", TableProps.builder()
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .tableName("urlDetails")
                .removalPolicy(RemovalPolicy.DESTROY)
                .partitionKey(Attribute.builder()
                        .name("shortUrl")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        urlDetails.grantReadWriteData(apiLambda.getRole());

        new CfnOutput(this, "APILambdaARN", CfnOutputProps.builder()
                .description("API lambda function ARN")
                .value(apiLambda.getFunctionArn())
                .build());

        new CfnOutput(this, "URLShortenerAPI-URL", CfnOutputProps.builder()
                .description("URL shortener API URL")
                .value(restApi.getUrl())
                .build());
    }
}

