package com.api.tests.comments;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("API Test Automation")
@Feature("Comment Management")
@Story("GET Comments")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET Comment API Tests")
public class GetCommentTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("GET /comments?postId=1 — should return comments for the given post")
    @Severity(SeverityLevel.NORMAL)
    void testGetCommentsByPostId_ShouldReturnCommentsList() {
        given()
                .spec(requestSpec)
                .queryParam("postId", ApiConfig.VALID_POST_ID)
                .when()
                .get(ApiConfig.COMMENTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", greaterThan(0))
                .body("[0].postId", equalTo(ApiConfig.VALID_POST_ID))
                .body("[0].email", notNullValue())
                .body("[0].body", notNullValue());
    }
}
