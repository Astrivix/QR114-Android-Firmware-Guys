package com.astrivix.qr114.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserCountry() {
        return userCountry;
    }

    public void setUserCountry(String userCountry) {
        this.userCountry = userCountry;
    }

    public String getMobileModel() {
        return mobileModel;
    }

    public void setMobileModel(String mobileModel) {
        this.mobileModel = mobileModel;
    }

    @SerializedName("email")
    private String email;

    @SerializedName("serial_number")
    private String serialNumber;

    @SerializedName("password")
    private String password;

    @SerializedName("user_country")
    private String userCountry;

    @SerializedName("mobile_model")
    private String mobileModel;

    public RegisterRequest(String email, String serialNumber, String password, String userCountry, String mobileModel) {
        this.email = email;
        this.serialNumber = serialNumber;
        this.password = password;
        this.userCountry = userCountry;
        this.mobileModel = mobileModel;
    }

}