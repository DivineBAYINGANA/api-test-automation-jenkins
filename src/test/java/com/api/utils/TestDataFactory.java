package com.api.utils;

import com.api.models.Post;
import com.api.models.User;

import java.util.HashMap;
import java.util.Map;

public class TestDataFactory {

    // ===================== Post Test Data =====================

    public static Post createValidPost() {
        return new Post(1, "API Test - Test Post Title", "This is a test body created by the API test automation framework.");
    }

    public static Post createUpdatedPost() {
        return new Post(1, 1, "Updated Title via PUT Request", "This body was updated using a full PUT request operation.");
    }

    public static Map<String, Object> createPatchData() {
        Map<String, Object> patch = new HashMap<>();
        patch.put("title", "Patched Title via PATCH Request");
        return patch;
    }

    public static Post createPostWithLongTitle() {
        return new Post(1, "A".repeat(255), "Body with very long title for boundary testing.");
    }

    public static Post createPostWithEmptyBody() {
        return new Post(1, "Valid Title", "");
    }

    // ===================== User Test Data =====================

    public static User createValidUser() {
        return new User("Divine Tester", "divine_tester", "divine@testapi.com");
    }

    // ===================== Raw Map Test Data =====================

    public static Map<String, Object> createRawPostMap() {
        Map<String, Object> post = new HashMap<>();
        post.put("userId", 1);
        post.put("title", "Raw Map Post for Testing");
        post.put("body", "Created via raw map — used for quick request body construction.");
        return post;
    }

    private TestDataFactory() {}
}
