package com.api.config;

public class ApiConfig {

    // ===================== BASE URLS =====================
    public static final String BASE_URL = "https://fakestoreapi.com";

    // ===================== ENDPOINTS =====================
    public static final String PRODUCTS_ENDPOINT = "/products";
    public static final String USERS_ENDPOINT = "/users";
    public static final String CARTS_ENDPOINT = "/carts";
    public static final String LOGIN_ENDPOINT = "/auth/login";
    public static final int STATUS_OK = 200;
    public static final int STATUS_CREATED = 201;
    public static final int STATUS_NOT_FOUND = 404;

    // ===================== TIMEOUTS (ms) =====================
    public static final int READ_TIMEOUT = 15000;

    private ApiConfig() {
    }
}
