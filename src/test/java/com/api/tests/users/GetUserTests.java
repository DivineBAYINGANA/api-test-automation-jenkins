package com.api.tests.users;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("API Test Automation")
@Feature("User Management")
@Story("GET Users")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET User API Tests")
public class GetUserTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("GET /users — should return 200 with 10 users")
    @Severity(SeverityLevel.BLOCKER)
    void testGetAllUsers_ShouldReturn200WithUsersList() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", equalTo(11))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue())
                .body("[0].email", notNullValue())
                .body("[0].username", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("GET /users/{id} — valid ID should return 200 with user data")
    @Severity(SeverityLevel.CRITICAL)
    void testGetUserById_ShouldReturn200WithUser() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id", equalTo(ApiConfig.VALID_USER_ID))
                .body("name", notNullValue())
                .body("email", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("GET /users/{id} — response should match user JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    void testGetUser_ShouldMatchUserJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/user-schema.json"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /users/{id} — email field should contain '@'")
    @Severity(SeverityLevel.MINOR)
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
    }

    @Test
    @Order(5)
    @DisplayName("GET /users/{id} — nested address and company objects should exist")
    @Severity(SeverityLevel.NORMAL)
    void testGetUser_NestedAddressShouldExist() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + ApiConfig.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("address", notNullValue())
                .body("address.street", notNullValue())
                .body("address.city", notNullValue())
                .body("address.zipcode", notNullValue())
                .body("company", notNullValue())
                .body("company.name", notNullValue());
    }
}
