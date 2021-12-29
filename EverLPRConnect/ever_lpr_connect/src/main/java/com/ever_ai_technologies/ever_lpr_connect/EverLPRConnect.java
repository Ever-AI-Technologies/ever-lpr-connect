package com.ever_ai_technologies.ever_lpr_connect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;

import com.ever_ai_technologies.ever_lpr_connect.interfaces.BluetoothCallback;
import com.ever_ai_technologies.ever_lpr_connect.interfaces.DataHandlingCallback;
import com.ever_ai_technologies.ever_lpr_connect.interfaces.DeviceManagerCallback;
import com.ever_ai_technologies.ever_lpr_connect.models.Vehicle;
import com.ever_ai_technologies.ever_lpr_connect.readers.LineReader;
import com.ever_ai_technologies.ever_lpr_connect.readers.SocketReader;
import com.ever_ai_technologies.ever_lpr_connect.utils.constants.DeviceErrorMsg;
import com.ever_ai_technologies.ever_lpr_connect.utils.constants.OperationEnum;
import com.ever_ai_technologies.ever_lpr_connect.utils.helpers.ThreadHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class EverLPRConnect {
    private final static String DEVICE_NAME = "Ever-LPR";
    private final static String DEFAULT_UUID = "00001101-0000-1000-8000-00805f9b34fb";
    private final static int REQUEST_ENABLE_BT = 1111;

    private Activity activity;
    private Context context;
    private UUID uuid;
    private String deviceName;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private DeviceManagerCallback deviceCallback;
    private BluetoothCallback bluetoothCallback;
    private DataHandlingCallback dataCallback;

    private ReceiveThread receiveThread;
    private boolean runOnUi;

    private Class readerClass;

    public EverLPRConnect(Context context) {
        initialize(context, DEVICE_NAME, UUID.fromString(DEFAULT_UUID));
    }

    public EverLPRConnect(Context context, String deviceName) {
        initialize(context, deviceName, UUID.fromString(DEFAULT_UUID));
    }

    public EverLPRConnect(Context context, UUID uuid) {
        initialize(context, DEVICE_NAME, UUID.fromString(DEFAULT_UUID));
    }

    public EverLPRConnect(Context context, String deviceName, UUID uuid) {
        initialize(context, deviceName, uuid);
    }

    private void initialize(Context context, String deviceName, UUID uuid) {
        this.context = context;
        this.deviceName = deviceName;
        this.uuid = uuid;
        this.readerClass = LineReader.class;
        this.deviceCallback = null;
        this.bluetoothCallback = null;
        this.runOnUi = false;
    }

    public void onStart() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        context.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void onActivityResult(int requestCode, final int resultCode) {
        if (bluetoothCallback != null) {
            if (requestCode == REQUEST_ENABLE_BT) {
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        if (resultCode == Activity.RESULT_CANCELED) {
                            bluetoothCallback.onUserDeniedActivation();
                        }
                    }
                });
            }
        }
    }

    public void enable() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }

    public void readData() {
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.READ_DATA, null);
    }

    public void getStatus() {
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.GET_STATUS, null);
    }

    public void updateMultipleData(ArrayList<Vehicle> vehicles) {
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.MASS_UPDATE_DATA, vehicles);
    }

    public void addData(Vehicle vehicle) {
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle);
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.ADD_DATA, vehicles);
    }

    public void updateData(Vehicle vehicle) {
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle);
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.UPDATE_DATA, vehicles);
    }

    public void deleteData(Vehicle vehicle) {
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle);
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.UPDATE_DATA, vehicles);
    }

    public void deleteData(String id) {
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicles.add(vehicle);
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.DELETE_DATA, vehicles);
    }

    public void getDeviceTime() {
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.GET_TIME, null);
    }

    public void syncDeviceTime() {
        connectToNameAndExecute(this.deviceName, false, false, OperationEnum.SYNC_TIME, null);
    }

    public void onStop() {
        context.unregisterReceiver(bluetoothReceiver);
    }

    public boolean isEnabled() {
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }

    public void showEnableDialog(Activity activity) {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (bluetoothCallback != null) {
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            switch (state) {
                                case BluetoothAdapter.STATE_OFF:
                                    bluetoothCallback.onBluetoothOff();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_OFF:
                                    bluetoothCallback.onBluetoothTurningOff();
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    bluetoothCallback.onBluetoothOn();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_ON:
                                    bluetoothCallback.onBluetoothTurningOn();
                                    break;
                            }
                        }
                    });
                }
            }
        }
    };

    public void connectToNameAndExecute(String name, boolean insecureConnection, boolean withPortTrick, int opEnum, ArrayList<Vehicle> vehicles) {
        for (BluetoothDevice blueDevice : bluetoothAdapter.getBondedDevices()) {
            if (blueDevice.getName().equals(name)) {
                connectAndExecute(blueDevice, insecureConnection, withPortTrick, opEnum, vehicles);
                return;
            }
        }
    }

    public void setDataHandlingCallback(DataHandlingCallback cb) {
        this.dataCallback = cb;
    }

    public void removeDataHandlingCallback() {
        this.dataCallback = null;
    }

    public void setDeviceManagerCallback(DeviceManagerCallback cb) {
        this.deviceCallback = cb;
    }

    public void removeDeviceManagerCallback() {
        this.deviceCallback = null;
    }

    public void setBluetoothCallback(BluetoothCallback cb) {
        this.bluetoothCallback = cb;
    }

    public void removeBluetoothCallback() {
        this.bluetoothCallback = null;
    }

    private BluetoothSocket createBluetoothSocketWithPortTrick(BluetoothDevice device) {
        BluetoothSocket socket = null;
        try {
            socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.w(getClass().getSimpleName(), e.getMessage());
        }
        return socket;
    }

    public void disconnect() {
        Log.d("disconnect", "Disconnect");
        try {
            receiveThread.getSocket().close();
        } catch (final IOException e) {
            if (deviceCallback != null) {
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        Log.w(getClass().getSimpleName(), e.getMessage());
                        deviceCallback.onStatusDown(DeviceErrorMsg.FAILED_WHILE_DISCONNECTED);
                    }
                });
            }
        }
    }

    private void connectAndExecute(BluetoothDevice device, boolean insecureConnection, boolean withPortTrick, int opEnum, ArrayList<Vehicle> vehicles) {
        BluetoothSocket socket = null;
        if (withPortTrick) {
            socket = createBluetoothSocketWithPortTrick(device);
        }
        if (socket == null) {
            try {
                if (insecureConnection) {
                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                } else {
                    socket = device.createRfcommSocketToServiceRecord(uuid);
                }
            } catch (IOException e) {
                if (deviceCallback != null) {
                    Log.w(getClass().getSimpleName(), e.getMessage());
                    deviceCallback.onStatusDown(DeviceErrorMsg.FAILED_WHILE_CREATING_SOCKET);
                }
            }
        }
        connectAndExecuteInThread(socket, device, opEnum, vehicles);
    }

    private void connectAndExecuteInThread(final BluetoothSocket socket, final BluetoothDevice device, int opEnum, ArrayList<Vehicle> objects) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.connect();

                    // Quick create thread
                    receiveThread = new ReceiveThread(readerClass, socket, device);
                    receiveThread.start();

                    SystemClock.sleep(100);

                    switch (opEnum) {
                        case OperationEnum.READ_DATA:
                            send("GET_DATA;");
                            break;
                        case OperationEnum.MASS_UPDATE_DATA:
                            String send_str = "[";
                            for (int i = 0; i < objects.size(); i++) {
                                if (send_str.length() > 3) {
                                    send_str += ",";
                                }
                                send_str += "{";
                                send_str += "\"id\":\"" + (objects.get(i).getId() == null ? "" : objects.get(i).getId()) + "\",";
                                send_str += "\"owner\":\"" + (objects.get(i).getOwner() == null ? "" : objects.get(i).getOwner()) + "\",";
                                send_str += "\"plate_no\":\"" + (objects.get(i).getPlateNo() == null ? "" : objects.get(i).getPlateNo()) + "\",";
                                send_str += "\"type\":\"" + (objects.get(i).getType() == null ? "" : objects.get(i).getPlateNo()) + "\",";
                                send_str += "\"brand\":\"" + (objects.get(i).getBrand() == null ? "" : objects.get(i).getBrand()) + "\",";
                                send_str += "\"manufactured_year\":" + objects.get(i).getManufacturedYear() + ",";
                                send_str += "\"is_blacklisted\":" + (objects.get(i).getIsBlacklisted() ? "1" : "0") + ",";
                                send_str += "\"last_in\":\"" + (objects.get(i).getLastIn() == null ? "" : objects.get(i).getLastIn()) + "\",";
                                send_str += "\"last_out\":\"" + (objects.get(i).getLastOut() == null ? "" : objects.get(i).getLastOut()) + "\"";
                                send_str += "}";
                            }
                            send_str += "]";
//                        send("UPDATE_DATA;[{\"id\":\"X1200\", \"owner\": \"Amri\", , \"plate_no\": \"XRF2233\", \"type\": \"\", \"brand\": \"\", \"manufactured_year\": 2000, \"is_blacklisted\": 0, \"last_in\": \"2020-10-01T12:00:10\", \"last_out\": \"2020-10-01T15:00:10\"}, {\"id\":\"X1201\", \"owner\": \"Syaza\", , \"plate_no\": \"AGF3344\", \"type\": \"\", \"brand\": \"\", \"manufactured_year\": 2000, \"is_blacklisted\": 1, \"last_in\": \"2020-10-02T12:00:10\", \"last_out\": \"2020-10-02T14:00:10\"}, {\"id\":\"X1203\", \"owner\": \"Tan Ah Beck\", , \"plate_no\": \"LPR3342\", \"type\": \"\", \"brand\": \"\", \"manufactured_year\": 2000, \"is_blacklisted\": 0, \"last_in\": \"2020-10-01T08:00:10\", \"last_out\": \"2020-10-01T09:00:10\"}]");
                            send("MASS_UPDATE_DATA;" + send_str);
                            break;
                        case OperationEnum.UPDATE_DATA:
                            String send_str_upd = "[";
                            for (int i = 0; i < objects.size(); i++) {
                                if (send_str_upd.length() > 3) {
                                    send_str_upd += ",";
                                }
                                send_str_upd += "{";
                                send_str_upd += "\"id\":\"" + (objects.get(i).getId() == null ? "" : objects.get(i).getId()) + "\",";
                                send_str_upd += "\"owner\":\"" + (objects.get(i).getOwner() == null ? "" : objects.get(i).getOwner()) + "\",";
                                send_str_upd += "\"plate_no\":\"" + (objects.get(i).getPlateNo() == null ? "" : objects.get(i).getPlateNo()) + "\",";
                                send_str_upd += "\"type\":\"" + (objects.get(i).getType() == null ? "" : objects.get(i).getPlateNo()) + "\",";
                                send_str_upd += "\"brand\":\"" + (objects.get(i).getBrand() == null ? "" : objects.get(i).getBrand()) + "\",";
                                send_str_upd += "\"manufactured_year\":" + objects.get(i).getManufacturedYear() + ",";
                                send_str_upd += "\"is_blacklisted\":" + (objects.get(i).getIsBlacklisted() ? "1" : "0") + ",";
                                send_str_upd += "\"last_in\":\"" + (objects.get(i).getLastIn() == null ? "" : objects.get(i).getLastIn()) + "\",";
                                send_str_upd += "\"last_out\":\"" + (objects.get(i).getLastOut() == null ? "" : objects.get(i).getLastOut()) + "\"";
                                send_str_upd += "}";
                            }
                            send_str_upd += "]";
                            send("UPDATE_DATA;" + send_str_upd);
                            break;
                        case OperationEnum.ADD_DATA:
                            String send_str_single = "[";
                            for (int i = 0; i < objects.size(); i++) {
                                if (send_str_single.length() > 3) {
                                    send_str_single += ",";
                                }
                                send_str_single += "{";
                                send_str_single += "\"id\":\"" + (objects.get(i).getId() == null ? "" : objects.get(i).getId()) + "\",";
                                send_str_single += "\"owner\":\"" + (objects.get(i).getOwner() == null ? "" : objects.get(i).getOwner()) + "\",";
                                send_str_single += "\"plate_no\":\"" + (objects.get(i).getPlateNo() == null ? "" : objects.get(i).getPlateNo()) + "\",";
                                send_str_single += "\"type\":\"" + (objects.get(i).getType() == null ? "" : objects.get(i).getPlateNo()) + "\",";
                                send_str_single += "\"brand\":\"" + (objects.get(i).getBrand() == null ? "" : objects.get(i).getBrand()) + "\",";
                                send_str_single += "\"manufactured_year\":" + objects.get(i).getManufacturedYear() + ",";
                                send_str_single += "\"is_blacklisted\":" + (objects.get(i).getIsBlacklisted() ? "1" : "0") + ",";
                                send_str_single += "\"last_in\":\"" + (objects.get(i).getLastIn() == null ? "" : objects.get(i).getLastIn()) + "\",";
                                send_str_single += "\"last_out\":\"" + (objects.get(i).getLastOut() == null ? "" : objects.get(i).getLastOut()) + "\"";
                                send_str_single += "}";
                            }
                            send_str_single += "]";
                            send("ADD_DATA;" + send_str_single);
                            break;
                        case OperationEnum.DELETE_DATA:
                            String send_str_add = "[";
                            for (int i = 0; i < objects.size(); i++) {
                                if (send_str_add.length() > 3) {
                                    send_str_add += ",";
                                }
                                send_str_add += "{";
                                send_str_add += "\"id\":\"" + (objects.get(i).getId() == null ? "" : objects.get(i).getId()) + "\"";
                                send_str_add += "}";
                            }
                            send_str_add += "]";
                            send("DELETE_DATA;" + send_str_add);
                            break;
                        case OperationEnum.GET_STATUS:
                            send("GET_STATUS;");
                            break;
                        case OperationEnum.GET_TIME:
                            send("GET_TIME;");
                            break;
                        case OperationEnum.SYNC_TIME:
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String formattedDate = sdf.format(new Date());
                            Log.d("TIME", formattedDate);
                            send("SYNC_TIME;" + formattedDate);
                            break;
                        default:
                            break;
                    }

                } catch (final IOException e) {
                    if (deviceCallback != null) {
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                deviceCallback.onStatusDown(DeviceErrorMsg.FAILED_WHILE_CREATING_THREAD);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void send(String msg, Charset charset) {
        if (charset == null) {
            send(msg.getBytes());
        } else {
            send(msg.getBytes(charset));
        }
    }

    public void send(String msg) {
        send(msg, null);
    }

    public void send(byte[] data) {
        if (receiveThread != null) {
            OutputStream out = receiveThread.getOutputStream();
            try {
                out.write(data);
            } catch (final IOException e) {
                if (deviceCallback != null) {
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            deviceCallback.onStatusDown(e.getMessage());
                        }
                    });
                }
            }
        }
    }

    private class ReceiveThread extends Thread implements Runnable {
        private SocketReader reader;
        private BluetoothSocket socket;
        private BluetoothDevice device;
        private OutputStream out;

        public ReceiveThread(Class<?> readerClass, BluetoothSocket socket, BluetoothDevice device) {
            Log.d("ReceiveThread", "Start");
            this.socket = socket;
            this.device = device;
            try {
                out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                this.reader = (SocketReader) readerClass.getDeclaredConstructor(InputStream.class).newInstance(in);
            } catch (IOException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                Log.w(getClass().getSimpleName(), e.getMessage());
            }
        }

        public void run() {
            Log.d("ReceiveThread", "run");
            JSONObject msg;
            try {
                while ((msg = reader.read()) != null) {
                    if (dataCallback != null && deviceCallback != null) {
                        final JSONObject msgCopy = msg;
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    switch (msgCopy.getString("status")) {
                                        case "SUCCESS":
                                            if (msgCopy.getString("operation").equals("GET_DATA")) {
                                                ArrayList<Vehicle> listdata = new ArrayList<Vehicle>();
                                                JSONArray jArray = (JSONArray) msgCopy.getJSONArray("messages");
                                                if (jArray != null) {
                                                    for (int i = 0; i < jArray.length(); i++) {
                                                        try {
                                                            listdata.add(new Vehicle(jArray.getJSONObject(i)));
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }
                                                dataCallback.onReadDataSuccess(listdata);
                                            } else if (msgCopy.getString("operation").equals("MASS_UPDATE_DATA")) {
                                                dataCallback.onWriteDataSuccess("MASS_UPDATE_DATA");
                                            } else if (msgCopy.getString("operation").equals("UPDATE_DATA")) {
                                                dataCallback.onWriteDataSuccess("UPDATE_DATA");
                                            } else if (msgCopy.getString("operation").equals("ADD_DATA")) {
                                                dataCallback.onWriteDataSuccess("ADD_DATA");
                                            } else if (msgCopy.getString("operation").equals("DELETE_DATA")) {
                                                dataCallback.onWriteDataSuccess("DELETE_DATA");
                                            } else if (msgCopy.getString("operation").equals("GET_TIME")) {
                                                deviceCallback.onGetTime(msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("SYNC_TIME")) {
                                                deviceCallback.onSyncTime(msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("GET_STATUS")) {
                                                deviceCallback.onStatusUp(msgCopy.getString("messages"));
                                            }
                                            break;
                                        case "FAILED":
                                            if (msgCopy.getString("operation").equals("GET_DATA")) {
                                                dataCallback.onReadDataFailed(msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("MASS_UPDATE_DATA")) {
                                                dataCallback.onWriteDataFailed("MASS_UPDATE_DATA", msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("UPDATE_DATA")) {
                                                dataCallback.onWriteDataFailed("UPDATE_DATA", msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("ADD_DATA")) {
                                                dataCallback.onWriteDataFailed("ADD_DATA", msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("DELETE_DATA")) {
                                                dataCallback.onWriteDataFailed("DELETE_DATA", msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("GET_STATUS")) {
                                                deviceCallback.onStatusDown("System down");
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                    disconnect();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    if (deviceCallback != null) {
                                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                                            @Override
                                            public void run() {
                                                deviceCallback.onStatusDown(e.getMessage());
                                            }
                                        });
                                    }
                                    disconnect();
                                }
                            }
                        });
                    }
                }
            } catch (final IOException | JSONException e) {
                Log.d("REPLY", String.valueOf(e.getMessage()));
                if (deviceCallback != null) {
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            deviceCallback.onStatusDown(e.getMessage());
                        }
                    });
                }
                disconnect();
            }
        }

        public BluetoothSocket getSocket() {
            return socket;
        }

        public BluetoothDevice getDevice() {
            return device;
        }

        public OutputStream getOutputStream() {
            return out;
        }
    }

}
