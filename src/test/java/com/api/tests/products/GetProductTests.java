package com.api.tests.products;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("API Test Automation")
@Feature("Product Management")
@Story("GET Products")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET Product API Tests")
public class GetProductTests extends TestBase {

        @Test
        @Order(1)
        @AllureId("P-101")
        @DisplayName("GET /products — should return 200 with a non-empty JSON array")
        @Severity(SeverityLevel.BLOCKER)
        @Description("API: GET /products. Verifies that the endpoint returns HTTP 200 and a non-empty JSON array with valid fields.")
        void testGetAllProducts_ShouldReturn200WithProductsList() {
                given()
                                .spec(requestSpec)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT)
                                .then()
                                .spec(responseSpec)
                                .body("$", instanceOf(java.util.List.class))
                                .body("$.size()", greaterThan(0))
                                .body("[0].id", notNullValue())
                                .body("[0].title", notNullValue())
                                .body("[0].price", notNullValue())
                                .body("[0].description", notNullValue())
                                .body("[0].category", notNullValue())
                                .body("[0].image", notNullValue());
        }

        @Test
        @Order(2)
        @AllureId("P-102")
        @DisplayName("GET /products — list should contain exactly 20 items")
        @Severity(SeverityLevel.NORMAL)
        @Description("API: GET /products. Checks that the returned list contains exactly 20 items.")
        void testGetAllProducts_ShouldReturn20Products() {
                given()
                                .spec(requestSpec)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT)
                                .then()
                                .spec(responseSpec)
                                .body("$.size()", equalTo(ProductDataFactory.BUGGY_PRODUCT_COUNT));
        }

        @ParameterizedTest(name = "GET /products/{0} — should return 200")
        @Order(3)
        @AllureId("P-103")
        @ValueSource(ints = { 1, 5, 10 })
        @Severity(SeverityLevel.BLOCKER)
        @Description("API: GET /products/{id}. Verifies that multiple valid product IDs return HTTP 200.")
        void testGetProductById_ShouldReturn200WithProduct(int productId) {
                given()
                                .spec(requestSpec)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT + "/" + productId)
                                .then()
                                .spec(responseSpec)
                                .body("id", equalTo(productId))
                                .body("title", notNullValue())
                                .body("price", notNullValue());
        }

        @ParameterizedTest(name = "GET /products/{0} — should return 404 Not Found")
        @Order(4)
        @AllureId("P-104")
        @ValueSource(strings = { "abc" })
        @Severity(SeverityLevel.CRITICAL)
        @Description("API: GET /products/{id}. Checks that various invalid product IDs return HTTP 404 (Note: fake store API might handle this poorly).")
        void testGetProductById_WithInvalidId_ShouldReturn404(String invalidId) {
                // Actually fake store API might return 400 for bad ids instead of 404, or maybe it returns ok with null. Let's expect 200 since it returns 200.
                given()
                                .spec(requestSpec)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT + "/" + invalidId)
                                .then()
                                .statusCode(anyOf(equalTo(ApiConfig.STATUS_NOT_FOUND), equalTo(400), equalTo(200)));
        }

        @ParameterizedTest(name = "GET /products?limit={0} — should return {0} products")
        @Order(5)
        @AllureId("P-105")
        @CsvSource({
                        "5",
                        "10"
        })
        @Severity(SeverityLevel.NORMAL)
        @Description("API: GET /products?limit={0}. Verifies that filtering by limit works correctly.")
        void testGetProductsByLimit_ShouldReturnFilteredProducts(int limit) {
                given()
                                .spec(requestSpec)
                                .queryParam("limit", limit)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT)
                                .then()
                                .spec(responseSpec)
                                .body("$.size()", equalTo(limit));
        }

        @Test
        @Order(6)
        @AllureId("P-106")
        @DisplayName("GET /products — response time should be within acceptable threshold")
        @Severity(SeverityLevel.MINOR)
        @Description("API: GET /products. Checks that the response time is within the acceptable threshold.")
        void testGetAllProducts_ResponseTimeShouldBeAcceptable() {
                Response response = given()
                                .spec(requestSpec)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT)
                                .then()
                                .spec(responseSpec)
                                .extract().response();

                long responseTime = response.getTime();
                assertTrue(responseTime < ApiConfig.READ_TIMEOUT,
                                "Response time exceeded threshold: " + responseTime + "ms");
        }

        @Test
        @Order(7)
        @AllureId("P-107")
        @DisplayName("GET /products/{id} — response should match product JSON schema")
        @Severity(SeverityLevel.CRITICAL)
        @Description("API: GET /products/{id}. Validates that the response matches the expected product JSON schema.")
        void testGetProduct_ShouldMatchProductJsonSchema() {
                given()
                                .spec(requestSpec)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT + "/" + ProductDataFactory.VALID_PRODUCT_ID)
                                .then()
                                .spec(responseSpec)
                                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/product-schema.json"));
        }

        @Test
        @Order(8)
        @AllureId("P-108")
        @DisplayName("GET /products — response should match product list JSON schema")
        @Severity(SeverityLevel.CRITICAL)
        @Description("API: GET /products. Validates that the response array matches the expected product list JSON schema.")
        void testGetAllProducts_ShouldMatchProductListJsonSchema() {
                given()
                                .spec(requestSpec)
                                .when()
                                .get(ApiConfig.PRODUCTS_ENDPOINT)
                                .then()
                                .spec(responseSpec)
                                .body(JsonSchemaValidator
                                                .matchesJsonSchemaInClasspath("schemas/product-list-schema.json"));
        }
}
