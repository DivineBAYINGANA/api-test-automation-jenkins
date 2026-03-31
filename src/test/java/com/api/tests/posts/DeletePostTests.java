package com.api.tests.posts;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;

@Epic("API Test Automation")
@Feature("Post Management")
@Story("DELETE Posts")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DELETE Post API Tests")
public class DeletePostTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("DELETE /posts/{id} — should return 200")
    @Severity(SeverityLevel.BLOCKER)
    void testDeletePost_WithValidId_ShouldReturn200() {
        given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.POSTS_ENDPOINT + "/" + PostDataFactory.VALID_POST_ID)
                .then()
                .spec(responseSpec);
    }

    @Test
    @Order(2)
    @AllureId("P-402")
    @DisplayName("DELETE /posts/{id} — response should match post JSON schema")
    @Severity(SeverityLevel.NORMAL)
    @Description("API: DELETE /posts/{id}. Validates that the response after deletion matches the post JSON schema. Note: JSONPlaceholder returns an empty object, which may cause failure if schema requires fields.")
    void testDeletePost_ShouldMatchPostJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.POSTS_ENDPOINT + "/" + PostDataFactory.VALID_POST_ID)
                .then()
                .spec(responseSpec)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-schema.json"));
    }
}
