package com.api.tests.posts;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
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
                .statusCode(ApiConfig.STATUS_OK)
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
                .statusCode(ApiConfig.STATUS_OK)
                .body("title", equalTo(patchData.get("title")))
                .body("id", equalTo(PostDataFactory.VALID_POST_ID));
    }
}
