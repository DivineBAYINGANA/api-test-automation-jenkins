package com.api.tests.users;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    @AllureId("U-001")
    @DisplayName("GET /users — should return 200 with 10 users")
    @Severity(SeverityLevel.BLOCKER)
    @Description("API: GET /users. Verifies that the endpoint returns HTTP 200 and a list of 10 users. Checks that each user object contains id, name, email, and username fields. Issue: Endpoint may return incorrect user count or missing fields.")
    void testGetAllUsers_ShouldReturn200WithUsersList() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", equalTo(UserDataFactory.EXPECTED_USER_COUNT))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue())
                .body("[0].email", notNullValue())
                .body("[0].username", notNullValue());
    }

    @ParameterizedTest(name = "GET /users/{0} — should return 200 for user {1}")
    @Order(2)
    @AllureId("U-002")
    @CsvSource({
            "1, Leanne Graham, Sincere@april.biz",
            "2, Ervin Howell, Shanna@melissa.tv",
            "3, Clementine Bauch, Nathan@yesenia.net"
    })
    @Severity(SeverityLevel.CRITICAL)
    @Description("API: GET /users/{id}. Verifies that multiple valid user IDs return HTTP 200 and correct user data.")
    void testGetUserById_ShouldReturn200WithUser(int userId, String expectedName, String expectedEmail) {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + userId)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("id", equalTo(userId))
                .body("name", equalTo(expectedName))
                .body("email", equalTo(expectedEmail));
    }

    @Test
    @Order(3)
    @AllureId("U-003")
    @DisplayName("GET /users/{id} — response should match user JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("API: GET /users/{id}. Validates that the response matches the expected user JSON schema. Issue: Schema mismatch or missing required fields.")
    void testGetUser_ShouldMatchUserJsonSchema() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + UserDataFactory.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/user-schema.json"));
    }

    @Test
    @Order(4)
    @AllureId("U-004")
    @DisplayName("GET /users/{id} — email field should contain '@'")
    @Severity(SeverityLevel.MINOR)
    @Description("API: GET /users/{id}. Checks that the email field contains '@'. Issue: Email format may be invalid or missing '@'.")
    void testGetUser_EmailShouldMatchPattern() {
        String email = given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + UserDataFactory.VALID_USER_ID)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .extract().jsonPath().getString("email");

        assertNotNull(email, "Email must not be null");
        assertTrue(email.contains("@"), "Email should contain '@', got: " + email);
    }

    @Test
    @Order(5)
    @AllureId("U-005")
    @DisplayName("GET /users/{id} — nested address and company objects should exist")
    @Severity(SeverityLevel.NORMAL)
    @Description("API: GET /users/{id}. Verifies that nested address and company objects exist and contain required fields. Issue: Missing nested objects or fields.")
    void testGetUser_NestedAddressShouldExist() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.USERS_ENDPOINT + "/" + UserDataFactory.VALID_USER_ID)
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
