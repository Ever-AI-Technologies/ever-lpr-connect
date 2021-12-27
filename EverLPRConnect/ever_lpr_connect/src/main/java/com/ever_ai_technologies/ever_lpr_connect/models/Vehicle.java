package com.ever_ai_technologies.ever_lpr_connect.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Vehicle {

    private String id;
    private String owner;
    private String plateNo;
    private String type;
    private String brand;
    private int manufacturedYear;
    private String lastIn;
    private String lastOut;
    private boolean isBlacklisted;

    public Vehicle() {

    }

    public Vehicle(JSONObject obj) throws JSONException {
        id = obj.getString("id");
        owner = obj.getString("owner");
        plateNo = obj.getString("plate_no");
        type = obj.getString("type");
        brand = obj.getString("brand");
        manufacturedYear = obj.getInt("manufactured_year");
        isBlacklisted = obj.getInt("is_blacklisted") != 0;
        lastIn = obj.getString("last_in");
        lastOut = obj.getString("last_out");
    }

    public String showData() {
        return "id = " + id + ", plateNo = " + plateNo;
    }

    public void setId (String _id) {
        id = _id;
    }

    public String getId () {
        return id;
    }

    public void setOwner (String _owner) {
        owner = _owner;
    }

    public String getOwner () {
        return owner;
    }

    public void setPlateNo (String _plateNo) {
        plateNo = _plateNo;
    }

    public String getPlateNo () {
        return plateNo;
    }

    public void setType (String _type) {
        type = _type;
    }

    public String getType () {
        return type;
    }

    public void setBrand (String _brand) {
        brand = _brand;
    }

    public String getBrand () {
        return brand;
    }

    public void setManufacturedYear (int _manufacturedYear) {
        manufacturedYear = _manufacturedYear;
    }

    public int getManufacturedYear () {
        return manufacturedYear;
    }

    public void setIsBlacklisted (boolean _isBlacklisted) {
        isBlacklisted = _isBlacklisted;
    }

    public boolean getIsBlacklisted () {
        return isBlacklisted;
    }

    public void setLastIn(String _lastIn) {
        lastIn = _lastIn;
    }

    public String getLastIn () {
        return lastIn;
    }

    public void setLastOut(String _lastOut) {
        lastOut = _lastOut;
    }

    public String getLastOut () {
        return lastOut;
    }
}
