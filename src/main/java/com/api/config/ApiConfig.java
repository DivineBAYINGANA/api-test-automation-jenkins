package com.api.config;

/**
 * API Configuration constants for Divine's API Testing Framework
 * Base URL: https://jsonplaceholder.typicode.com (sample REST API)
 */
public class ApiConfig {

    // ===================== BASE URLS =====================
    public static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    public static final String BASE_PATH = "/";

    // ===================== ENDPOINTS =====================
    public static final String POSTS_ENDPOINT      = "/posts";
    public static final String USERS_ENDPOINT      = "/users";
    public static final String COMMENTS_ENDPOINT   = "/comments";
    public static final String ALBUMS_ENDPOINT     = "/albums";
    public static final String PHOTOS_ENDPOINT     = "/photos";
    public static final String TODOS_ENDPOINT      = "/todos";

    // ===================== HTTP STATUS CODES =====================
    public static final int STATUS_OK              = 200;
    public static final int STATUS_CREATED         = 201;
    public static final int STATUS_NO_CONTENT      = 204;
    public static final int STATUS_BAD_REQUEST     = 400;
    public static final int STATUS_UNAUTHORIZED    = 401;
    public static final int STATUS_NOT_FOUND       = 404;
    public static final int STATUS_UNPROCESSABLE   = 422;

    // ===================== HEADERS =====================
    public static final String CONTENT_TYPE_JSON   = "application/json; charset=utf-8";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String ACCEPT_HEADER       = "Accept";

    // ===================== TIMEOUTS (ms) =====================
    public static final int CONNECTION_TIMEOUT     = 10000;
    public static final int READ_TIMEOUT           = 15000;

    // ===================== TEST DATA =====================
    public static final int VALID_POST_ID          = 1;
    public static final int VALID_USER_ID          = 1;
    public static final int INVALID_ID             = 9999;

    private ApiConfig() {
        // Utility class - prevent instantiation
    }
}
