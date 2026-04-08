package com.api.tests.products;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("API Test Automation")
@Feature("Product Management")
@Story("UPDATE Products")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PUT/PATCH Update API Tests")
public class UpdateProductTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("PUT /products/{id} — full update should return success")
    @Severity(SeverityLevel.BLOCKER)
    void testPutProduct_WithValidPayload_ShouldReturnSuccess() {
        Map<String, Object> updatedProduct = ProductDataFactory.createUpdatedProduct();

        given()
                .spec(requestSpec)
                .body(updatedProduct)
                .when()
                .put(ApiConfig.PRODUCTS_ENDPOINT + "/" + ProductDataFactory.VALID_PRODUCT_ID)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("id", equalTo(ProductDataFactory.VALID_PRODUCT_ID))
                .body("title", equalTo(updatedProduct.get("title")))
                .body("description", equalTo(updatedProduct.get("description")));
    }

    @Test
    @Order(2)
    @DisplayName("PATCH /products/{id} — partial update of title should return success")
    @Severity(SeverityLevel.CRITICAL)
    void testPatchProduct_ShouldUpdateOnlySpecifiedFields() {
        Map<String, Object> patchData = ProductDataFactory.createPatchData();

        given()
                .spec(requestSpec)
                .body(patchData)
                .when()
                .patch(ApiConfig.PRODUCTS_ENDPOINT + "/" + ProductDataFactory.VALID_PRODUCT_ID)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("title", equalTo(patchData.get("title")))
                .body("id", equalTo(ProductDataFactory.VALID_PRODUCT_ID));
    }

    @Test
    @Order(3)
    @AllureId("P-303")
    @DisplayName("PUT /products/{id} — response should match product JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("API: PUT /products/{id}. Validates that the fully updated product matches the JSON schema.")
    void testPutProduct_ShouldMatchProductJsonSchema() {
        given()
                .spec(requestSpec)
                .body(ProductDataFactory.createUpdatedProduct())
                .when()
                .put(ApiConfig.PRODUCTS_ENDPOINT + "/" + ProductDataFactory.VALID_PRODUCT_ID)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/product-schema.json"));
    }

    // PATCH returns only the patched properties in Fake Store API, so it does not match the full product schema.
}
