package com.api.tests.posts;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("API Test Automation")
@Feature("Post Management")
@Story("UPDATE Posts")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PUT/PATCH Update API Tests")
public class UpdatePostTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("PUT /posts/{id} — full update should return 200")
    @Severity(SeverityLevel.BLOCKER)
    void testPutPost_WithValidPayload_ShouldReturn200() {
        Map<String, Object> updatedPost = PostDataFactory.createUpdatedPost();

        given()
                .spec(requestSpec)
                .body(updatedPost)
                .when()
                .put(ApiConfig.POSTS_ENDPOINT + "/" + PostDataFactory.VALID_POST_ID)
                .then()
                .spec(responseSpec)
                .body("id", equalTo(PostDataFactory.VALID_POST_ID))
                .body("userId", equalTo(updatedPost.get("userId")))
                .body("title", equalTo(updatedPost.get("title")))
                .body("body", equalTo(updatedPost.get("body")));
    }

    @Test
    @Order(2)
    @DisplayName("PATCH /posts/{id} — partial update of title should return 200")
    @Severity(SeverityLevel.CRITICAL)
    void testPatchPost_ShouldUpdateOnlySpecifiedFields() {
        Map<String, Object> patchData = PostDataFactory.createPatchData();

        given()
                .spec(requestSpec)
                .body(patchData)
                .when()
                .patch(ApiConfig.POSTS_ENDPOINT + "/" + PostDataFactory.VALID_POST_ID)
                .then()
                .spec(responseSpec)
                .body("title", equalTo(patchData.get("title")))
                .body("id", equalTo(PostDataFactory.VALID_POST_ID));
    }

    @Test
    @Order(3)
    @AllureId("P-303")
    @DisplayName("PUT /posts/{id} — response should match post JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("API: PUT /posts/{id}. Validates that the fully updated post matches the JSON schema.")
    void testPutPost_ShouldMatchPostJsonSchema() {
        given()
                .spec(requestSpec)
                .body(PostDataFactory.createUpdatedPost())
                .when()
                .put(ApiConfig.POSTS_ENDPOINT + "/" + PostDataFactory.VALID_POST_ID)
                .then()
                .spec(responseSpec)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-schema.json"));
    }

    @Test
    @Order(4)
    @AllureId("P-304")
    @DisplayName("PATCH /posts/{id} — response should match post JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("API: PATCH /posts/{id}. Validates that the partially updated post matches the JSON schema.")
    void testPatchPost_ShouldMatchPostJsonSchema() {
        given()
                .spec(requestSpec)
                .body(PostDataFactory.createPatchData())
                .when()
                .patch(ApiConfig.POSTS_ENDPOINT + "/" + PostDataFactory.VALID_POST_ID)
                .then()
                .spec(responseSpec)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-schema.json"));
    }
}
