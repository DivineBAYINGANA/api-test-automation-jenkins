package com.api.tests.posts;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
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
                .statusCode(ApiConfig.STATUS_OK);
    }
}
