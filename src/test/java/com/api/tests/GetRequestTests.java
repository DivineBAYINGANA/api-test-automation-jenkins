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
 * GET Request Test Suite for Divine's API — JUnit 5
 */
@Epic("API Test Automation")
@Feature("GET Requests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GetRequestTests extends TestBase {

    @Test @Order(1)
    @DisplayName("GET /posts — should return 200 with a non-empty JSON array")
    @Story("GET All Posts") @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that GET /posts returns 200 with a non-empty JSON array containing valid fields")
    void testGetAllPosts_ShouldReturn200WithPostsList() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .contentType(containsString("application/json"))
                .body("$",          instanceOf(java.util.List.class))
                .body("$.size()",   greaterThan(0))
                .body("[0].id",     notNullValue())
                .body("[0].userId", notNullValue())
                .body("[0].title",  notNullValue())
                .body("[0].body",   notNullValue());
    }

    @Test @Order(2)
    @DisplayName("GET /posts — list should contain exactly 100 items")
    @Story("GET All Posts") @Severity(SeverityLevel.NORMAL)
    void testGetAllPosts_ShouldReturn100Posts() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", equalTo(100));
    }

    @Test @Order(3)
    @DisplayName("GET /posts/{id} — valid ID should return 200 with post data")
    @Story("GET Single Post") @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that GET /posts/{id} returns 200 with correct post data")
    void testGetPostById_ShouldReturn200WithPost() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.VALID_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id",     equalTo(ApiConfig.VALID_POST_ID))
                .body("userId", equalTo(1))
                .body("title",  notNullValue())
                .body("body",   notNullValue());
    }

    @Test @Order(4)
    @DisplayName("GET /posts/{id} — invalid ID should return 404")
    @Story("GET Single Post - Negative") @Severity(SeverityLevel.CRITICAL)
    void testGetPostById_WithInvalidId_ShouldReturn404() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.INVALID_ID)
                .then()
                .statusCode(ApiConfig.STATUS_NOT_FOUND);
    }

    @Test @Order(5)
    @DisplayName("GET /posts?userId=1 — should return only posts for that user")
    @Story("GET Posts - Filtering") @Severity(SeverityLevel.NORMAL)
    void testGetPostsByUserId_ShouldReturnFilteredPosts() {
        given()
                .spec(requestSpec)
                .queryParam("userId", 1)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", greaterThan(0))
                .body("userId",   everyItem(equalTo(1)));
    }

    @Test @Order(6)
    @DisplayName("GET /users — should return 200 with 10 users")
    @Story("GET All Users") @Severity(SeverityLevel.BLOCKER)
    void testGetAllUsers_ShouldReturn200WithUsersList() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()",     equalTo(10))
                .body("[0].id",       notNullValue())
                .body("[0].name",     notNullValue())
                .body("[0].email",    notNullValue())
                .body("[0].username", notNullValue());
    }

    @Test @Order(7)
    @DisplayName("GET /users/{id} — valid ID should return 200 with user data")
    @Story("GET Single User") @Severity(SeverityLevel.CRITICAL)
    void testGetUserById_ShouldReturn200WithUser() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id",    equalTo(ApiConfig.VALID_USER_ID))
                .body("name",  notNullValue())
                .body("email", notNullValue());
    }

    @Test @Order(8)
    @DisplayName("GET /posts — response headers should include Content-Type: application/json")
    @Story("Response Headers Validation") @Severity(SeverityLevel.MINOR)
    void testGetPosts_ResponseHeadersShouldBeValid() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .header(ApiConfig.CONTENT_TYPE_HEADER, containsString("application/json"))
                .header("X-Content-Type-Options", notNullValue());
    }

    @Test @Order(9)
    @DisplayName("GET /posts — response time should be within acceptable threshold")
    @Story("Performance - Response Time") @Severity(SeverityLevel.MINOR)
    void testGetAllPosts_ResponseTimeShouldBeAcceptable() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().response();

        long responseTime = response.getTime();
        log.info("Response time: {} ms", responseTime);
        assertTrue(responseTime < ApiConfig.READ_TIMEOUT,
                "Response time exceeded threshold: " + responseTime + "ms");
    }

    @Test @Order(10)
    @DisplayName("GET /comments?postId=1 — should return comments for the given post")
    @Story("GET Comments by Post") @Severity(SeverityLevel.NORMAL)
    void testGetCommentsByPostId_ShouldReturnCommentsList() {
        given()
                .spec(requestSpec)
                .queryParam("postId", ApiConfig.VALID_POST_ID)
                .when()
                .get(ApiConfig.COMMENTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()",   greaterThan(0))
                .body("[0].postId", equalTo(ApiConfig.VALID_POST_ID))
                .body("[0].email",  notNullValue())
                .body("[0].body",   notNullValue());
    }
}
