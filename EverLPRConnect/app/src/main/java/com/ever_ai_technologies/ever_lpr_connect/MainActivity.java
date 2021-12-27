package com.ever_ai_technologies.ever_lpr_connect;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

    EverLPRConnect everLPRConnect;

    private TableLayout table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate Ever LPR library
        everLPRConnect = new EverLPRConnect(this);

        // Set Data Handling callback
        everLPRConnect.setDataHandlingCallback(dataCallback);

        // Set Device Manager callback
        everLPRConnect.setDeviceManagerCallback(deviceCallback);

        // Set Bluetooth callback
        everLPRConnect.setBluetoothCallback(btCallback);

        table = (TableLayout) findViewById(R.id.tblDisplay);
    }

    @Override
    protected void onStart() {
        super.onStart();
        everLPRConnect.onStart();
        if (everLPRConnect.isEnabled()) {
            // doStuffWhenBluetoothOn() ...
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

    private DataHandlingCallback dataCallback = new DataHandlingCallback() {

        @Override
        public void onReadDataSuccess(ArrayList<Vehicle> vehicles) {
            Log.d("DataHandlingCallback", "onReadDataSuccess");

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
        }

        @Override
        public void onWriteDataSuccess() {
            Log.d("DataHandlingCallback", "onWriteDataSuccess");
        }

        @Override
        public void onWriteDataFailed(String errorCode) {
            Log.d("DataHandlingCallback", "onWriteDataFailed");
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
        table.removeViews(1, table.getChildCount() - 1);
        for (int i = 0; i < vehicles.size(); i++) {
            TableRow row = new TableRow(this);

            CheckBox tv9 = new CheckBox(this);
            tv9.setChecked(vehicles.get(i).getIsBlacklisted());

            TextView tv1 = new TextView(this);
            tv1.setText(vehicles.get(i).getId());

            TextView tv2 = new TextView(this);
            tv2.setText(vehicles.get(i).getOwner());

            TextView tv3 = new TextView(this);
            tv3.setText(vehicles.get(i).getPlateNo());

            TextView tv4 = new TextView(this);
            tv4.setText(vehicles.get(i).getType());

            TextView tv5 = new TextView(this);
            tv5.setText(vehicles.get(i).getBrand());

            TextView tv6 = new TextView(this);
            tv6.setText(String.valueOf(vehicles.get(i).getManufacturedYear()));

            TextView tv7 = new TextView(this);
            tv7.setText(vehicles.get(i).getLastIn());

            TextView tv8 = new TextView(this);
            tv8.setText(vehicles.get(i).getLastOut());


            row.addView(tv9);
            row.addView(tv1);
            row.addView(tv2);
            row.addView(tv3);
            row.addView(tv4);
            row.addView(tv5);
            row.addView(tv6);
            row.addView(tv7);
            row.addView(tv8);


            table.addView(row);
        }
    }

    public void getData(View view) {
        everLPRConnect.readData();
    }

    public void getStatus(View view) {
        everLPRConnect.getStatus();
    }

    public void storeData(View view) {
        ArrayList<Vehicle> vehicles = new ArrayList<>();

        for (int i = 1; i < table.getChildCount(); i++) {
            View viewRow = table.getChildAt(i);
            if (viewRow instanceof TableRow) {
                TableRow row = (TableRow) viewRow;
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
                vehicles.add(vehicle);
            }
        }
        everLPRConnect.storeData(vehicles);
    }
}