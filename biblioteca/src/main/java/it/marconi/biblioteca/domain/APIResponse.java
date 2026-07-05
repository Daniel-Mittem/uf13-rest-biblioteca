package it.marconi.biblioteca.domain;

import java.util.Map;

public record APIResponse<T>(
        String status,   // "success" | "fail" | "error"
        T data,
        String message
) {
    public static <T> APIResponse<T> success(T data) {
        return new APIResponse<>("success", data, null);
    }

    public static APIResponse<Map<String, String>> fail(Map<String, String> errors) {
        return new APIResponse<>("fail", errors, null);
    }

    public static APIResponse<Void> error(String message) {
        return new APIResponse<>("error", null, message);
    }
}