package com.astrivix.qr114.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class ErrorResponse {

    public void setMessage(String message) {
        this.message = message;
    }

    public void setErrors(Map<String, List<String>> errors) {
        this.errors = errors;
    }

    @SerializedName("message")
    private String message;

    @SerializedName("errors")
    private Map<String, List<String>> errors;

    public String getMessage() {
        return message;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}