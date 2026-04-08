package com.api.tests.products;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ProductDataFactory {

    public static final int VALID_PRODUCT_ID = 1;
    public static final int BUGGY_PRODUCT_COUNT = 20;

    public static Map<String, Object> createValidProduct() {
        Map<String, Object> product = new HashMap<>();
        product.put("title", "API Test - Test Product");
        product.put("price", 13.5);
        product.put("description", "This is a test product created by the API test automation framework.");
        product.put("image", "https://i.pravatar.cc");
        product.put("category", "electronic");
        return product;
    }

    public static Map<String, Object> createUpdatedProduct() {
        Map<String, Object> product = new HashMap<>();
        product.put("title", "Updated Title via PUT Request");
        product.put("price", 25.5);
        product.put("description", "This body was updated using a full PUT request operation.");
        product.put("image", "https://i.pravatar.cc");
        product.put("category", "electronic");
        return product;
    }

    public static Map<String, Object> createPatchData() {
        Map<String, Object> patch = new HashMap<>();
        patch.put("title", "Patched Title via PATCH Request");
        return patch;
    }

    public static Stream<Map<String, Object>> validProductProvider() {
        Map<String, Object> product1 = new HashMap<>();
        product1.put("title", "Product 1 Title");
        product1.put("price", 10.0);
        product1.put("description", "Description of product 1");
        product1.put("image", "https://i.pravatar.cc");
        product1.put("category", "electronic");

        Map<String, Object> product2 = new HashMap<>();
        product2.put("title", "Product 2 Title");
        product2.put("price", 15.0);
        product2.put("description", "Description of product 2");
        product2.put("image", "https://i.pravatar.cc");
        product2.put("category", "jewelery");

        return Stream.of(product1, product2);
    }

    private ProductDataFactory() {
    }
}
