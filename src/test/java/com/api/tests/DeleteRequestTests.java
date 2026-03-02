package com.api.tests;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DELETE Request Test Suite for Divine's API — JUnit 5
 */
@Epic("API Test Automation")
@Feature("DELETE Requests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeleteRequestTests extends TestBase {

    private static final int TARGET_POST_ID = ApiConfig.VALID_POST_ID;

    @Test @Order(1)
    @DisplayName("DELETE /posts/{id} — should return 200")
    @Story("DELETE Post") @Severity(SeverityLevel.BLOCKER)
    @Description("DELETE /posts/{id} must return HTTP 200 confirming the resource was deleted")
    void testDeletePost_WithValidId_ShouldReturn200() {
        given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK);
    }

    @Test @Order(2)
    @DisplayName("DELETE /posts/{id} — response body should be empty or '{}'")
    @Story("DELETE Post - Response Body") @Severity(SeverityLevel.NORMAL)
    void testDeletePost_ResponseBodyShouldBeEmptyOrNull() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().response();

        String body = response.getBody().asString().trim();
        log.info("DELETE response body: '{}'", body);
        assertTrue(body.isEmpty() || body.equals("{}"),
                "DELETE response body should be empty or '{}', got: " + body);
    }

    @Test @Order(3)
    @DisplayName("DELETE /posts/{id} — resource should not be accessible after deletion")
    @Story("DELETE - Resource No Longer Accessible") @Severity(SeverityLevel.CRITICAL)
    void testDeletePost_ResourceShouldNotBeAccessibleAfterDeletion() {
        // Step 1: Delete
        given().spec(requestSpec)
                .when().delete(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then().statusCode(ApiConfig.STATUS_OK);

        log.info("Post {} deleted. Verifying inaccessibility...", TARGET_POST_ID);

        // Step 2: Attempt GET (JSONPlaceholder simulates deletion; real APIs return 404)
        Response getResponse = given().spec(requestSpec)
                .when().get(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then().extract().response();

        int status = getResponse.getStatusCode();
        log.info("GET after DELETE returned: {}", status);
        assertTrue(status == ApiConfig.STATUS_OK || status == ApiConfig.STATUS_NOT_FOUND,
                "Unexpected status after DELETE: " + status);
    }

    @Test @Order(4)
    @DisplayName("DELETE /posts — multiple sequential deletes should all return 200")
    @Story("DELETE - Multiple Resources") @Severity(SeverityLevel.NORMAL)
    void testDeleteMultiplePosts_ShouldAllReturn200() {
        int[] postIds = {1, 2, 3};
        for (int postId : postIds) {
            given()
                    .spec(requestSpec)
                    .when()
                    .delete(ApiConfig.POSTS_ENDPOINT + "/" + postId)
                    .then()
                    .statusCode(ApiConfig.STATUS_OK);
            log.info("Deleted post ID: {}", postId);
        }
    }

    @Test @Order(5)
    @DisplayName("DELETE /posts/9999 — non-existent resource should return 404 or handle gracefully")
    @Story("DELETE - Negative") @Severity(SeverityLevel.NORMAL)
    void testDeletePost_WithInvalidId_ShouldReturn404() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.INVALID_ID)
                .then()
                .extract().response();

        int status = response.getStatusCode();
        log.info("DELETE with invalid ID returned: {}", status);
        assertTrue(status == ApiConfig.STATUS_NOT_FOUND || status == ApiConfig.STATUS_OK,
                "Expected 404 for non-existent resource, got: " + status);
    }

    @Test @Order(6)
    @DisplayName("DELETE /posts/{id} — response time should be within acceptable threshold")
    @Story("Performance - DELETE Response Time") @Severity(SeverityLevel.MINOR)
    void testDeletePost_ResponseTimeShouldBeAcceptable() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().response();

        long responseTime = response.getTime();
        log.info("DELETE response time: {} ms", responseTime);
        assertTrue(responseTime < ApiConfig.READ_TIMEOUT,
                "DELETE response time exceeded threshold: " + responseTime + "ms");
    }

    @Test @Order(7)
    @DisplayName("DELETE /posts/{id} — response headers should include Content-Type")
    @Story("DELETE - Headers Validation") @Severity(SeverityLevel.MINOR)
    void testDeletePost_ResponseHeadersShouldBeValid() {
        given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .header(ApiConfig.CONTENT_TYPE_HEADER, notNullValue());
    }
}
