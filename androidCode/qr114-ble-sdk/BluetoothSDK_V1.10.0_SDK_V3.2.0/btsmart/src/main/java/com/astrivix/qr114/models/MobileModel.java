package com.astrivix.qr114.models;

import android.os.Build;

public class MobileModel {
    public String getMobileModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }
}
