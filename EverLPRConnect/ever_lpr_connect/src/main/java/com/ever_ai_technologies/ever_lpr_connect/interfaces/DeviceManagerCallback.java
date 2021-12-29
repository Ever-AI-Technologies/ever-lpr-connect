package com.ever_ai_technologies.ever_lpr_connect.interfaces;

public interface DeviceManagerCallback {
    void onStatusUp(String status);
    void onStatusDown(String errorCode);
    void onGetTime(String dateTime);
    void onSyncTime(String syncStatus);
}
