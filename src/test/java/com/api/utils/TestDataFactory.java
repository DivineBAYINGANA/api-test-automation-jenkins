package com.api.utils;

import com.api.models.Post;

import java.util.HashMap;
import java.util.Map;

public class TestDataFactory {

    // ===================== Post Test Data =====================

    public static Post createValidPost() {
        return new Post(1, "API Test - Test Post Title",
                "This is a test body created by the API test automation framework.");
    }

    public static Post createUpdatedPost() {
        return new Post(1, 1, "Updated Title via PUT Request",
                "This body was updated using a full PUT request operation.");
    }

    public static Map<String, Object> createPatchData() {
        Map<String, Object> patch = new HashMap<>();
        patch.put("title", "Patched Title via PATCH Request");
        return patch;
    }

    private TestDataFactory() {
    }
}
