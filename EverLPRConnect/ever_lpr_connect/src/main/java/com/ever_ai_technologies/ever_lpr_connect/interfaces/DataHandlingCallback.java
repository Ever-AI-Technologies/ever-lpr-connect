package com.ever_ai_technologies.ever_lpr_connect.interfaces;

import com.ever_ai_technologies.ever_lpr_connect.models.Vehicle;

import java.util.ArrayList;

public interface DataHandlingCallback {
    void onReadDataSuccess(ArrayList<Vehicle> vehicles);
    void onReadDataFailed(String errorCode);
    void onWriteDataSuccess();
    void onWriteDataFailed(String errorCode);
}
