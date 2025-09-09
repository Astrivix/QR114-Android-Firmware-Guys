package com.astrivix.qr114.models;

import com.google.gson.annotations.SerializedName;
/**
 *
 * @author sasanda saumya
 * @since 2025/7/16
 */
public class LoginResponse {
    private boolean success;
    private String message;
    private LoginData data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public LoginData getData() { return data; }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(LoginData data) {
        this.data = data;
    }

    // Nested class for the "data" object
    public static class LoginData {
        private User user;
        @SerializedName("access_token")
        private String accessToken;
        @SerializedName("token_type")
        private String tokenType;

        public User getUser() { return user; }
        public String getAccessToken() { return accessToken; }
    }

    // Nested class for the "user" object
    public static class User {
        private int id;
        private String email;
        @SerializedName("serial_number")
        private String serialNumber;

        public int getId() { return id; }
        public String getEmail() { return email; }
        public String getSerialNumber() { return serialNumber; }

    }
}