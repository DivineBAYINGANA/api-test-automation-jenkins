package com.api.tests.posts;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PostDataFactory {

    public static final int VALID_POST_ID = 1;
    public static final int INVALID_ID = 9999;
    public static final int EXPECTED_POST_COUNT = 100;
    public static final int BUGGY_POST_COUNT = 101; // Intentionally wrong for testing notifications

    public static Map<String, Object> createValidPost() {
        Map<String, Object> post = new HashMap<>();
        post.put("userId", 1);
        post.put("title", "API Test - Test Post Title");
        post.put("body", "This is a test body created by the API test automation framework.");
        return post;
    }

    public static Map<String, Object> createUpdatedPost() {
        Map<String, Object> post = new HashMap<>();
        post.put("id", 1);
        post.put("userId", 1);
        post.put("title", "Updated Title via PUT Request");
        post.put("body", "This body was updated using a full PUT request operation.");
        return post;
    }

    public static Map<String, Object> createPatchData() {
        Map<String, Object> patch = new HashMap<>();
        patch.put("title", "Patched Title via PATCH Request");
        return patch;
    }

    public static Stream<Map<String, Object>> validPostProvider() {
        Map<String, Object> post1 = new HashMap<>();
        post1.put("userId", 1);
        post1.put("title", "Post 1 Title");
        post1.put("body", "Body of post 1");

        Map<String, Object> post2 = new HashMap<>();
        post2.put("userId", 2);
        post2.put("title", "Post 2 Title");
        post2.put("body", "Body of post 2");

        return Stream.of(post1, post2);
    }

    private PostDataFactory() {
    }
}
