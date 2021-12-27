package com.ever_ai_technologies.ever_lpr_connect.readers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LineReader extends SocketReader {
    private BufferedReader reader;

    public LineReader(InputStream inputStream) {
        super(inputStream);
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public JSONObject read() throws IOException, JSONException {
        String val = reader.readLine().replace("\"", "'");
        Log.d("LineReader", val);
        if (val.length() > 0) {
            JSONObject obj = new JSONObject(val);
            Log.d("LineReader Post process", obj.toString());
            return obj;
        } else {
            return null;
        }
    }
}
