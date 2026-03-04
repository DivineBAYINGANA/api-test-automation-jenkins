package com.api.tests.users;

import java.util.HashMap;
import java.util.Map;

public class UserDataFactory {

    public static final int VALID_USER_ID = 1;
    public static final int EXPECTED_USER_COUNT = 10;

    public static Map<String, Object> createValidUser() {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "John Doe");
        user.put("username", "johndoe");
        user.put("email", "john.doe@example.com");
        return user;
    }

    private UserDataFactory() {
    }
}
