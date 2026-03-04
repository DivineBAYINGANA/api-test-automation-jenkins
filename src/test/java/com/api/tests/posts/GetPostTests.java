package com.api.tests.posts;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    @AllureId("P-101")
    @DisplayName("GET /posts — should return 200 with a non-empty JSON array")
    @Severity(SeverityLevel.BLOCKER)
    @Description("API: GET /posts. Verifies that the endpoint returns HTTP 200 and a non-empty JSON array with valid fields. Issue: Endpoint may return empty array or missing fields.")
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
    @AllureId("P-102")
    @DisplayName("GET /posts — list should contain exactly 100 items")
    @Severity(SeverityLevel.NORMAL)
    @Description("API: GET /posts. Checks that the returned list contains exactly 100 items. Issue: Incorrect item count returned.")
    void testGetAllPosts_ShouldReturn100Posts() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", equalTo(PostDataFactory.BUGGY_POST_COUNT));
    }

    @ParameterizedTest(name = "GET /posts/{0} — should return 200")
    @Order(3)
    @AllureId("P-103")
    @ValueSource(ints = { 1, 5, 10 })
    @Severity(SeverityLevel.BLOCKER)
    @Description("API: GET /posts/{id}. Verifies that multiple valid post IDs return HTTP 200 and correct post data.")
    void testGetPostById_ShouldReturn200WithPost(int postId) {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + postId)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id", equalTo(postId))
                .body("title", notNullValue())
                .body("body", notNullValue());
    }

    @ParameterizedTest(name = "GET /posts/{0} — should return 404 Not Found")
    @Order(4)
    @AllureId("P-104")
    @ValueSource(strings = { "9999", "0", "-1", "abc" })
    @Severity(SeverityLevel.CRITICAL)
    @Description("API: GET /posts/{id}. Checks that various invalid post IDs return HTTP 404.")
    void testGetPostById_WithInvalidId_ShouldReturn404(String invalidId) {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + invalidId)
                .then()
                .statusCode(ApiConfig.STATUS_NOT_FOUND);
    }

    @ParameterizedTest(name = "GET /posts?userId={0} — user {0} should have {1} posts")
    @Order(5)
    @AllureId("P-105")
    @CsvSource({
            "1, 10",
            "2, 10",
            "3, 10"
    })
    @Severity(SeverityLevel.NORMAL)
    @Description("API: GET /posts?userId={0}. Verifies that filtering by userId works correctly for multiple users.")
    void testGetPostsByUserId_ShouldReturnFilteredPosts(int userId, int expectedCount) {
        given()
                .spec(requestSpec)
                .queryParam("userId", userId)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", equalTo(expectedCount))
                .body("userId", everyItem(equalTo(userId)));
    }

    @Test
    @Order(6)
    @AllureId("P-106")
    @DisplayName("GET /posts — response time should be within acceptable threshold")
    @Severity(SeverityLevel.MINOR)
    @Description("API: GET /posts. Checks that the response time is within the acceptable threshold. Issue: Slow response times may indicate performance problems.")
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
    @AllureId("P-107")
    @DisplayName("GET /posts/{id} — response should match post JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("API: GET /posts/{id}. Validates that the response matches the expected post JSON schema. Issue: Schema mismatch or missing required fields.")
    void testGetPost_ShouldMatchPostJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + PostDataFactory.VALID_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-schema.json"));
    }
}
