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
import java.util.ArrayList;
import java.util.UUID;

public class EverLPRConnect {
    private final static String DEVICE_NAME = "Ever-LPR";
    private final static String DEFAULT_UUID = "00001101-0000-1000-8000-00805f9b34fb";
    private final static int REQUEST_ENABLE_BT = 1111;

    private Activity activity;
    private Context context;
    private UUID uuid;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private DeviceManagerCallback deviceCallback;
    private BluetoothCallback bluetoothCallback;
    private DataHandlingCallback dataCallback;

    private ReceiveThread receiveThread;
    private boolean connected, runOnUi;

    private Class readerClass;

    public EverLPRConnect(Context context) {
        initialize(context, UUID.fromString(DEFAULT_UUID));
    }

    public EverLPRConnect(Context context, UUID uuid) {
        initialize(context, uuid);
    }

    private void initialize(Context context, UUID uuid) {
        this.context = context;
        this.uuid = uuid;
        this.readerClass = LineReader.class;
        this.deviceCallback = null;
        this.bluetoothCallback = null;
        this.connected = false;
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

    public void readData() {
        connectToNameAndExecute(DEVICE_NAME, false, false, OperationEnum.READ_DATA, null);
    }

    public void getStatus() {
        connectToNameAndExecute(DEVICE_NAME, false, false, OperationEnum.GET_STATUS, null);
    }

    public void storeData(ArrayList<Vehicle> vehicles) {
        connectToNameAndExecute(DEVICE_NAME, false, false, OperationEnum.STORE_DATA, vehicles);
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
                        deviceCallback.onStatusDown("Failed to disconnect");
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
                    deviceCallback.onStatusDown("Failed to create socket");
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
                    connected = true;

                    // Quick create thread
                    receiveThread = new ReceiveThread(readerClass, socket, device);
                    receiveThread.start();

                    SystemClock.sleep(100);

                    switch (opEnum) {
                        case OperationEnum.READ_DATA:
                            send("GET_DATA;");
                            break;
                        case OperationEnum.STORE_DATA:
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
                            send("UPDATE_DATA;" + send_str);
                            break;
                        case OperationEnum.GET_STATUS:
                            send("GET_STATUS;");
                            break;
                        default:
                            break;
                    }

                } catch (final IOException e) {
                    if (deviceCallback != null) {
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                deviceCallback.onStatusDown("Failed to create thread");
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
                connected = false;
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
                    if (dataCallback != null) {
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
                                            } else if (msgCopy.getString("operation").equals("UPDATE_DATA")) {
                                                dataCallback.onWriteDataSuccess();
                                            }
                                            break;
                                        case "FAILED":
                                            if (msgCopy.getString("operation").equals("GET_DATA")) {
                                                dataCallback.onReadDataFailed(msgCopy.getString("messages"));
                                            } else if (msgCopy.getString("operation").equals("UPDATE_DATA")) {
                                                dataCallback.onWriteDataFailed(msgCopy.getString("messages"));
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            } catch (final IOException | JSONException e) {
                connected = false;
                Log.d("REPLY", String.valueOf(e.getMessage()));
                if (dataCallback != null) {
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            dataCallback.onReadDataFailed(e.getMessage());
                        }
                    });
                }
            } finally {
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
