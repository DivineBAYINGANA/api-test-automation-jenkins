package com.api.tests.todos;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("API Test Automation")
@Feature("Todo Management")
@Story("GET Todos")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET Todo API Tests")
public class GetTodoTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("GET /todos — should return 200 with todos list")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllTodos_ShouldReturn200() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.TODOS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", greaterThan(0));
    }
}
