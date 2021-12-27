package com.ever_ai_technologies.ever_lpr_connect.readers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public abstract class SocketReader {
    protected InputStream inputStream;

    public SocketReader(InputStream inputStream){
        this.inputStream = inputStream;
    }

    /**
     * Will be called continuously to read from the socket.
     * Must be a blocking call.
     * @return byte array of data, or null if any error.
     * @throws IOException if anything happens...
     */
    public JSONObject read() throws IOException, JSONException {
        Log.d("SocketReader", "null");
        return null;
    }
}
