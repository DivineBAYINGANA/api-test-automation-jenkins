package com.api.tests.posts;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("API Test Automation")
@Feature("Post Management")
@Story("GET Posts")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET Post API Tests")
public class GetPostTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("GET /posts — should return 200 with a non-empty JSON array")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that GET /posts returns 200 with a non-empty JSON array containing valid fields")
    void testGetAllPosts_ShouldReturn200WithPostsList() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .contentType(containsString("application/json"))
                .body("$", instanceOf(java.util.List.class))
                .body("$.size()", greaterThan(0))
                .body("[0].id", notNullValue())
                .body("[0].userId", notNullValue())
                .body("[0].title", notNullValue())
                .body("[0].body", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("GET /posts — list should contain exactly 100 items")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllPosts_ShouldReturn100Posts() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", equalTo(101));
    }

    @Test
    @Order(3)
    @DisplayName("GET /posts/{id} — valid ID should return 200 with post data")
    @Severity(SeverityLevel.BLOCKER)
    void testGetPostById_ShouldReturn200WithPost() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.VALID_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id", equalTo(ApiConfig.VALID_POST_ID))
                .body("userId", equalTo(1))
                .body("title", notNullValue())
                .body("body", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("GET /posts/{id} — invalid ID should return 404")
    @Severity(SeverityLevel.CRITICAL)
    void testGetPostById_WithInvalidId_ShouldReturn404() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.INVALID_ID)
                .then()
                .statusCode(ApiConfig.STATUS_NOT_FOUND);
    }

    @Test
    @Order(5)
    @DisplayName("GET /posts?userId=1 — should return only posts for that user")
    @Severity(SeverityLevel.NORMAL)
    void testGetPostsByUserId_ShouldReturnFilteredPosts() {
        given()
                .spec(requestSpec)
                .queryParam("userId", 1)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", greaterThan(0))
                .body("userId", everyItem(equalTo(1)));
    }

    @Test
    @Order(6)
    @DisplayName("GET /posts — response time should be within acceptable threshold")
    @Severity(SeverityLevel.MINOR)
    void testGetAllPosts_ResponseTimeShouldBeAcceptable() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().response();

        long responseTime = response.getTime();
        assertTrue(responseTime < ApiConfig.READ_TIMEOUT,
                "Response time exceeded threshold: " + responseTime + "ms");
    }

    @Test
    @Order(7)
    @DisplayName("GET /posts/{id} — response should match post JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    void testGetPost_ShouldMatchPostJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.VALID_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-schema.json"));
    }
}
