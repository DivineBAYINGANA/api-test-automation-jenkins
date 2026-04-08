package com.api.tests.products;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;

@Epic("API Test Automation")
@Feature("Product Management")
@Story("DELETE Products")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DELETE Product API Tests")
public class DeleteProductTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("DELETE /products/{id} — should return 200")
    @Severity(SeverityLevel.BLOCKER)
    void testDeleteProduct_WithValidId_ShouldReturn200() {
        given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.PRODUCTS_ENDPOINT + "/" + ProductDataFactory.VALID_PRODUCT_ID)
                .then()
                .spec(responseSpec);
    }

    @Test
    @Order(2)
    @AllureId("P-402")
    @DisplayName("DELETE /products/{id} — response should match product JSON schema")
    @Severity(SeverityLevel.NORMAL)
    @Description("API: DELETE /products/{id}. Validates that the response after deletion matches the product JSON schema.")
    void testDeleteProduct_ShouldMatchProductJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .delete(ApiConfig.PRODUCTS_ENDPOINT + "/" + ProductDataFactory.VALID_PRODUCT_ID)
                .then()
                .spec(responseSpec)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/product-schema.json"));
    }
}
