package com.astrivix.qr114.models;

public class PasswordChangeRequest {
    final String current_password;
    final String password;
    final String password_confirmation; // Add this field

    public PasswordChangeRequest(String current_password, String password, String password_confirmation) {
        this.current_password = current_password;
        this.password = password;
        this.password_confirmation = password_confirmation;
    }
}