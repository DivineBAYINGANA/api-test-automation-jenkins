package com.api.tests;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("API Test Automation")
@Feature("JSON Schema Validation")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchemaValidationTests extends TestBase {

    @Test @Order(1)
    @DisplayName("GET /posts/{id} — response should match post JSON schema")
    @Story("Schema - Single Post") @Severity(SeverityLevel.CRITICAL)
    @Description("Ensure the single post response conforms to the defined JSON schema")
    void testGetPost_ShouldMatchPostJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.VALID_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-schema.json"));
    }

    @Test @Order(2)
    @DisplayName("GET /posts — response array should match post list JSON schema")
    @Story("Schema - Posts List") @Severity(SeverityLevel.CRITICAL)
    void testGetAllPosts_ShouldMatchPostListJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/post-list-schema.json"));
    }

    @Test @Order(3)
    @DisplayName("GET /users/{id} — response should match user JSON schema")
    @Story("Schema - Single User") @Severity(SeverityLevel.CRITICAL)
    void testGetUser_ShouldMatchUserJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/user-schema.json"));
    }

    @Test @Order(4)
    @DisplayName("GET /posts/{id} — post field data types should be correct")
    @Story("Field Type Validation") @Severity(SeverityLevel.NORMAL)
    void testGetPost_FieldTypesShouldBeCorrect() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.VALID_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id",     instanceOf(Integer.class))
                .body("userId", instanceOf(Integer.class))
                .body("title",  instanceOf(String.class))
                .body("body",   instanceOf(String.class));
    }

    @Test @Order(5)
    @DisplayName("GET /users/{id} — user field data types should be correct")
    @Story("Field Type Validation") @Severity(SeverityLevel.NORMAL)
    void testGetUser_FieldTypesShouldBeCorrect() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id",       instanceOf(Integer.class))
                .body("name",     instanceOf(String.class))
                .body("username", instanceOf(String.class))
                .body("email",    instanceOf(String.class));
    }

    @Test @Order(6)
    @DisplayName("GET /posts/{id} — title and body fields should not be empty")
    @Story("Field Constraints") @Severity(SeverityLevel.NORMAL)
    void testGetPost_TitleShouldNotBeEmpty() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.POSTS_ENDPOINT + "/" + ApiConfig.VALID_POST_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("title", not(emptyOrNullString()))
                .body("body",  not(emptyOrNullString()));
    }

    @Test @Order(7)
    @DisplayName("GET /users/{id} — email field should contain '@'")
    @Story("Field Constraints") @Severity(SeverityLevel.MINOR)
    void testGetUser_EmailShouldMatchPattern() {
        String email = given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().jsonPath().getString("email");

        assertNotNull(email, "Email must not be null");
        assertTrue(email.contains("@"), "Email should contain '@', got: " + email);
        log.info("Validated email format: {}", email);
    }

    @Test @Order(8)
    @DisplayName("GET /users/{id} — nested address and company objects should exist")
    @Story("Nested Object Validation") @Severity(SeverityLevel.NORMAL)
    void testGetUser_NestedAddressShouldExist() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("address",         notNullValue())
                .body("address.street",  notNullValue())
                .body("address.city",    notNullValue())
                .body("address.zipcode", notNullValue())
                .body("company",         notNullValue())
                .body("company.name",    notNullValue());
    }
}
