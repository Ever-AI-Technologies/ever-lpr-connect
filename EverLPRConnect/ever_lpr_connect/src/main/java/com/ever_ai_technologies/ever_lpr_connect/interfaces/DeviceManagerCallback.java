package com.ever_ai_technologies.ever_lpr_connect.interfaces;

public interface DeviceManagerCallback {
    void onStatusUp();
    void onStatusDown(String errorCode);
}
