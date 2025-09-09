package com.astrivix.qr114.models;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("serial_number")
    private String serialNumber;

    @SerializedName("password")
    private String password;

    @SerializedName("password_confirmation")
    private String passwordConfirmation;

    public ForgotPasswordRequest(String email, String serialNumber, String password, String passwordConfirmation) {
        this.email = email;
        this.serialNumber = serialNumber;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
    }

    // Getters can be added if needed, but are not required for Gson serialization
}