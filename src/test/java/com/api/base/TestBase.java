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

/**
 * Base test configuration class for JUnit 5.
 * Sets up REST Assured request/response specifications and Allure reporting filters.
 * @BeforeAll runs once per test class (static method).
 */
public abstract class TestBase {

    protected static final Logger log = LoggerFactory.getLogger(TestBase.class);

    protected static RequestSpecification  requestSpec;
    protected static ResponseSpecification responseSpec;

    @BeforeAll
    static void globalSetup() {
        log.info("========== Initializing Divine's API Test Suite ==========");

        // ----- Request Specification -----
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ApiConfig.BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())           // Allure report integration
                .addFilter(new RequestLoggingFilter(LogDetail.ALL))
                .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
                .build();

        // ----- Default Response Specification -----
        responseSpec = new ResponseSpecBuilder()
                .expectStatusCode(ApiConfig.STATUS_OK)
                .expectContentType(ContentType.JSON)
                .build();

        RestAssured.requestSpecification = requestSpec;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        log.info("Base URL   : {}", ApiConfig.BASE_URL);
        log.info("Setup complete. Running tests...\n");
    }

    /**
     * Build a custom response spec for a specific status code.
     */
    protected ResponseSpecification expectStatus(int statusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .build();
    }
}
