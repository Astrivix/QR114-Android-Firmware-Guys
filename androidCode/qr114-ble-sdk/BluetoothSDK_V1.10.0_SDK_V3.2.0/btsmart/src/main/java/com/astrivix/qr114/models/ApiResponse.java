package com.astrivix.qr114.models; // Or your preferred models package

public class ApiResponse {
    // These field names must exactly match the keys in your API's JSON response
    boolean success;
    String message;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}