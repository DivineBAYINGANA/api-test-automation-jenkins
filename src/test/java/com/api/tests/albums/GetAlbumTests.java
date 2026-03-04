package com.api.tests.albums;

import com.api.config.ApiConfig;
import com.api.base.TestBase;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("API Test Automation")
@Feature("Album Management")
@Story("GET Albums")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GET Album API Tests")
public class GetAlbumTests extends TestBase {

    @Test
    @Order(1)
    @DisplayName("GET /albums — should return 200 with albums list")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllAlbums_ShouldReturn200() {
        given()
                .spec(requestSpec)
                .when()
                .get(ApiConfig.ALBUMS_ENDPOINT)
                .then()
                .statusCode(ApiConfig.STATUS_OK)
                .body("$.size()", equalTo(AlbumDataFactory.BUGGY_ALBUM_COUNT));
    }
}
