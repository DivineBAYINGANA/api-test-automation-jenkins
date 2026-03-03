package com.api.tests.photos;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("API Test Automation")
@Feature("Photo Management")
@Story("GET Photos")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET Photo API Tests")
public class GetPhotoTests extends TestBase {

    @Test @Order(1)
    @DisplayName("GET /photos — should return 200 with photos list")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllPhotos_ShouldReturn200() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.PHOTOS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", greaterThan(0));
    }
}
