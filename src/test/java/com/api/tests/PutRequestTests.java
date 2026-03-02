package com.api.tests;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import com.api.utils.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("API Test Automation")
@Feature("PUT / PATCH Requests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PutRequestTests extends TestBase {

    private static final int TARGET_POST_ID = ApiConfig.VALID_POST_ID;

    @Test @Order(1)
    @DisplayName("PUT /posts/{id} — full update should return 200")
    @Story("PUT - Full Update") @Severity(SeverityLevel.BLOCKER)
    @Description("PUT /posts/{id} with a complete payload must return HTTP 200 and the fully updated resource")
    void testPutPost_WithValidPayload_ShouldReturn200() {
        var updatedPost = TestDataFactory.createUpdatedPost();

        given()
                .spec(requestSpec)
                .body(updatedPost)
                .when()
                .put(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id",     equalTo(TARGET_POST_ID))
                .body("userId", equalTo(updatedPost.getUserId()))
                .body("title",  equalTo(updatedPost.getTitle()))
                .body("body",   equalTo(updatedPost.getBody()));
    }

    @Test @Order(2)
    @DisplayName("PUT /posts/{id} — all updated fields should match the request")
    @Story("PUT - Full Update") @Severity(SeverityLevel.CRITICAL)
    void testPutPost_UpdatedFieldsShouldMatchRequest() {
        String uniqueTitle = "Updated Title - " + System.currentTimeMillis();
        String uniqueBody  = "Updated body content - " + System.currentTimeMillis();

        Map<String, Object> payload = new HashMap<>();
        payload.put("id",     TARGET_POST_ID);
        payload.put("userId", 5);
        payload.put("title",  uniqueTitle);
        payload.put("body",   uniqueBody);

        Response response = given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .put(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().response();

        assertAll("updated fields",
                () -> assertEquals(uniqueTitle, response.jsonPath().getString("title")),
                () -> assertEquals(uniqueBody,  response.jsonPath().getString("body")),
                () -> assertEquals(5,           response.jsonPath().getInt("userId"))
        );
        log.info("PUT update verified for post ID: {}", TARGET_POST_ID);
    }

    @Test @Order(3)
    @DisplayName("PUT /posts/{id} — response Content-Type should be JSON")
    @Story("PUT - Headers") @Severity(SeverityLevel.MINOR)
    void testPutPost_ResponseHeadersShouldBeCorrect() {
        given()
                .spec(requestSpec)
                .body(TestDataFactory.createUpdatedPost())
                .when()
                .put(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .header(ApiConfig.CONTENT_TYPE_HEADER, containsString("application/json"));
    }

    @Test @Order(4)
    @DisplayName("PUT /posts/9999 — non-existent resource should return 404 or handle gracefully")
    @Story("PUT - Negative") @Severity(SeverityLevel.NORMAL)
    void testPutPost_WithInvalidId_ShouldReturn404OrHandle() {
        Response response = given()
                .spec(requestSpec)
                .body(TestDataFactory.createUpdatedPost())
                .when()
                .put(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.INVALID_ID)
                .then()
                .extract().response();

        int status = response.getStatusCode();
        log.info("PUT on invalid ID returned: {}", status);
        assertTrue(status >= 400 || status == ApiConfig.STATUS_OK,
                "Unexpected status: " + status);
    }

    @Test @Order(5)
    @DisplayName("PATCH /posts/{id} — partial update of title should return 200")
    @Story("PATCH - Partial Update") @Severity(SeverityLevel.CRITICAL)
    @Description("PATCH /posts/{id} with a partial payload must return HTTP 200 and reflect the change")
    void testPatchPost_ShouldUpdateOnlySpecifiedFields() {
        Map<String, Object> patchData = TestDataFactory.createPatchData();

        given()
                .spec(requestSpec)
                .body(patchData)
                .when()
                .patch(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("title", equalTo(patchData.get("title")))
                .body("id",    equalTo(TARGET_POST_ID));
    }

    @Test @Order(6)
    @DisplayName("PATCH /posts/{id} — unpatched fields should be preserved")
    @Story("PATCH - Field Preservation") @Severity(SeverityLevel.NORMAL)
    void testPatchPost_ShouldPreserveUnpatchedFields() {
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("title", "Only Title Changed");

        Response response = given()
                .spec(requestSpec)
                .body(patchData)
                .when()
                .patch(ApiConfig.POSTS_ENDPOINT + "/" + TARGET_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().response();

        assertAll("preserved fields after PATCH",
                () -> assertNotNull(response.jsonPath().getString("body"),   "body should be preserved"),
                () -> assertNotNull(response.jsonPath().getString("userId"), "userId should be preserved"),
                () -> assertEquals("Only Title Changed", response.jsonPath().getString("title"))
        );
    }
}
