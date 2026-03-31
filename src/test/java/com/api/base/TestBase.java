package com.api.base;

import com.api.config.ApiConfig;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TestBase {

    protected static final Logger log = LoggerFactory.getLogger(TestBase.class);

    protected static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;

    @BeforeAll
    static void globalSetup() {
        log.info("========== Initializing API Test Suite ==========");

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ApiConfig.BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .addFilter(new RequestLoggingFilter(LogDetail.ALL))
                .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectStatusCode(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(ApiConfig.STATUS_OK),
                        org.hamcrest.Matchers.is(ApiConfig.STATUS_CREATED)))
                .build();

        RestAssured.requestSpecification = requestSpec;
        log.info("Setup complete. Running tests...\n");
    }
}
