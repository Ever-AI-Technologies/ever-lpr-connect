package com.ever_ai_technologies.ever_lpr_connect;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.ever_ai_technologies.ever_lpr_connect.interfaces.BluetoothCallback;
import com.ever_ai_technologies.ever_lpr_connect.interfaces.DataHandlingCallback;
import com.ever_ai_technologies.ever_lpr_connect.interfaces.DeviceManagerCallback;
import com.ever_ai_technologies.ever_lpr_connect.models.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String DEVICE_NAME = "Ever-LPR-5S00A001";

    EverLPRConnect everLPRConnect;

    LinearLayout mainLayout;

    TextView lblTotal;

    ArrayList<Vehicle> mVehicles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate Ever LPR library
        everLPRConnect = new EverLPRConnect(this, DEVICE_NAME);

        // Set Data Handling callback
        everLPRConnect.setDataHandlingCallback(dataCallback);

        // Set Device Manager callback
        everLPRConnect.setDeviceManagerCallback(deviceCallback);

        // Set Bluetooth callback
        everLPRConnect.setBluetoothCallback(btCallback);

        mainLayout = findViewById(R.id.mainLayout);

        lblTotal = findViewById(R.id.lblTotal);

        lblTotal.setText("0 items");

    }

    @Override
    protected void onStart() {
        super.onStart();
        everLPRConnect.onStart();
        if (everLPRConnect.isEnabled()) {
            // do nothing since bluetooth adaptor already connected
        } else {
            everLPRConnect.showEnableDialog(MainActivity.this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        everLPRConnect.removeDataHandlingCallback();
        everLPRConnect.removeDeviceManagerCallback();
        everLPRConnect.removeBluetoothCallback();
        everLPRConnect.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        everLPRConnect.onActivityResult(requestCode, resultCode);
    }

    private DataHandlingCallback dataCallback = new DataHandlingCallback() {

        @Override
        public void onReadDataSuccess(ArrayList<Vehicle> vehicles) {
            Log.d("DataHandlingCallback", "onReadDataSuccess");

            mVehicles = vehicles;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTable(vehicles);
                }
            });

        }

        @Override
        public void onReadDataFailed(String errorCode) {
            Log.d("DataHandlingCallback", "onReadDataFailed");
            Log.d("DataHandlingCallback", errorCode);
        }

        @Override
        public void onWriteDataSuccess(String operationMode) {
            Log.d("DataHandlingCallback", "onWriteDataSuccess : " + operationMode);
        }

        @Override
        public void onWriteDataFailed(String operationMode, String errorCode) {
            Log.d("DataHandlingCallback", "onWriteDataFailed " + operationMode);
            Log.d("DataHandlingCallback", errorCode);
        }
    };

    private DeviceManagerCallback deviceCallback = new DeviceManagerCallback() {

        @Override
        public void onStatusUp() {
            Log.d("DeviceManagerCallback", "Device is up");
        }

        @Override
        public void onStatusDown(String errorCode) {
            Log.d("DeviceManagerCallback", errorCode);
        }

    };

    private BluetoothCallback btCallback = new BluetoothCallback() {

        @Override
        public void onBluetoothTurningOn() {

        }

        @Override
        public void onBluetoothOn() {

        }

        @Override
        public void onBluetoothTurningOff() {

        }

        @Override
        public void onBluetoothOff() {

        }

        @Override
        public void onUserDeniedActivation() {

        }
    };

    public void updateTable(ArrayList<Vehicle> vehicles) {
        lblTotal.setText(vehicles.size() + " items");
        if (mainLayout.getChildCount() > 0) {
            mainLayout.removeViews(0, mainLayout.getChildCount());
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 0, 0, 0);

        for (int i = 0; i < vehicles.size(); i++) {

            LinearLayout ly = new LinearLayout(this);

            CheckBox tv9 = new CheckBox(this);
            tv9.setChecked(vehicles.get(i).getIsBlacklisted());
            tv9.setLayoutParams(params);

            TextView tv1 = new TextView(this);
            tv1.setText(vehicles.get(i).getId());
            tv1.setVisibility(View.GONE);

            TextView tv2 = new TextView(this);
            tv2.setText(vehicles.get(i).getOwner());
            tv2.setLayoutParams(params);

            TextView tv3 = new TextView(this);
            tv3.setText(vehicles.get(i).getPlateNo());
            tv3.setLayoutParams(params);

            TextView tv4 = new TextView(this);
            tv4.setText(vehicles.get(i).getType());
            tv4.setVisibility(View.GONE);

            TextView tv5 = new TextView(this);
            tv5.setText(vehicles.get(i).getBrand());
            tv5.setVisibility(View.GONE);

            TextView tv6 = new TextView(this);
            tv6.setText(String.valueOf(vehicles.get(i).getManufacturedYear()));
            tv6.setVisibility(View.GONE);

            TextView tv7 = new TextView(this);
            tv7.setText(vehicles.get(i).getLastIn());
            tv7.setVisibility(View.GONE);

            TextView tv8 = new TextView(this);
            tv8.setText(vehicles.get(i).getLastOut());
            tv8.setVisibility(View.GONE);

            ly.addView(tv9);
            ly.addView(tv1);
            ly.addView(tv2);
            ly.addView(tv3);
            ly.addView(tv4);
            ly.addView(tv5);
            ly.addView(tv6);
            ly.addView(tv7);
            ly.addView(tv8);

            mainLayout.addView(ly);
        }
    }

    public void getData(View view) {
        everLPRConnect.readData();
    }

    public void getStatus(View view) {
        everLPRConnect.getStatus();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void storeData(View view) {
        ArrayList<Vehicle> vehicles = new ArrayList<>();

        for (int i = 0; i < mainLayout.getChildCount(); i++) {
            View viewRow = mainLayout.getChildAt(i);
            if (viewRow instanceof LinearLayout) {

                LinearLayout row = (LinearLayout) viewRow;

                Vehicle vehicle = new Vehicle();
                vehicle.setId((String) ((TextView) row.getChildAt(1)).getText());
                vehicle.setOwner((String) ((TextView) row.getChildAt(2)).getText());
                vehicle.setPlateNo((String) ((TextView) row.getChildAt(3)).getText());
                vehicle.setBrand((String) ((TextView) row.getChildAt(4)).getText());
                vehicle.setType((String) ((TextView) row.getChildAt(5)).getText());
                vehicle.setManufacturedYear(Integer.parseInt((String) ((TextView) row.getChildAt(6)).getText()));
                vehicle.setIsBlacklisted(((CheckBox) row.getChildAt(0)).isChecked());
                vehicle.setLastIn((String) ((TextView) row.getChildAt(7)).getText());
                vehicle.setLastOut((String) ((TextView) row.getChildAt(8)).getText());

                Vehicle originalVehicle = findById(vehicle.getId(), mVehicles);
                boolean noChange = originalVehicle.compare(vehicle);

                if (!noChange) {
                    vehicles.add(vehicle);
                }
            }
        }
        everLPRConnect.updateMultipleData(vehicles);
    }

    public Vehicle findById(String id, ArrayList<Vehicle> vehicles) {
        Vehicle findVehicle = null;

        for (Vehicle v : vehicles) {
            if (v.getId().equals(id)) {
                findVehicle = v;
                break;
            }
        }

        return findVehicle;
    }
}