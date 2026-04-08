package com.api.tests.carts;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import com.api.tests.products.ProductDataFactory;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("API Test Automation")
@Feature("Cart Management")
@Story("GET Carts")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET Cart API Tests")
public class GetCartTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("GET /carts?userId=1 — should return carts for the given user")
    @Severity(SeverityLevel.NORMAL)
    void testGetCartsByUserId_ShouldReturnCartsList() {
        given()
                .spec(requestSpec)
                .queryParam("userId", ProductDataFactory.VALID_PRODUCT_ID)  // Using valid user/product ID = 1
                .when()
                .get(ApiConfig.CARTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", greaterThan(0))
                .body("[0].userId", equalTo(ProductDataFactory.VALID_PRODUCT_ID))
                .body("[0].date", notNullValue())
                .body("[0].products", notNullValue());
    }
}
