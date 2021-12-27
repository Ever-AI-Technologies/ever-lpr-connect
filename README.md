# Ever LPR Connect Library

Ever LPR Connect SDK can be used to access data from **Ever-LPR** device.

# Install

Add to your gradle dependencies:

```
implementation 'com.ever-ai-technologies.libraries:ever-lpr-connect:0.0.1'
```

## Managing bluetooth permission

### Prompt user for bluetooth activation

```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    // ...
    // Need to pair bluetooth device with Ever-LPR before starting apps
    everLPRConnect = new EverLPRConnect(this);
    everLPRConnect.setBluetoothCallback(btCallback);
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
    
    everLPRConnect.removeBluetoothCallback();
    everLPRConnect.onStop();
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    everLPRConnect.onActivityResult(requestCode, resultCode);
}

private BluetoothCallback btCallback = new BluetoothCallback() {
    @Override public void onBluetoothTurningOn() {}
    @Override public void onBluetoothTurningOff() {}
    @Override public void onBluetoothOff() {}
    @Override public void onBluetoothOn() {}
    @Override public void onUserDeniedActivation() {}
};
```

### Force the bluetooth activation without prompting user

```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    // ...
    // Need to pair bluetooth device with Ever-LPR before starting apps
    everLPRConnect = new EverLPRConnect(this);
    everLPRConnect.setBluetoothCallback(btCallback);
}

@Override
protected void onStart() {
    super.onStart();

    everLPRConnect.onStart();
    if (everLPRConnect.isEnabled()) {
        // do nothing since bluetooth adaptor already connected
    } else {
        everLPRConnect.enable();
    }
}

@Override
protected void onStop() {
    super.onStop();
    
    everLPRConnect.removeBluetoothCallback();
    everLPRConnect.onStop();
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    everLPRConnect.onActivityResult(requestCode, resultCode);
}

private BluetoothCallback btCallback = new BluetoothCallback() {
    @Override public void onBluetoothTurningOn() {}
    @Override public void onBluetoothTurningOff() {}
    @Override public void onBluetoothOff() {}
    @Override public void onBluetoothOn() {}
    @Override public void onUserDeniedActivation() {}
};
```

## Connecting to Ever-LPR device

### Listener

```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    // ...
    // Set Data Handling callback
    everLPRConnect.setDataHandlingCallback(dataCallback);

    // Set Device Manager callback
    everLPRConnect.setDeviceManagerCallback(deviceCallback);
}

@Override
protected void onStop() {
    ....
    everLPRConnect.removeDataHandlingCallback();
    everLPRConnect.removeDeviceManagerCallback();
    ....
}

private DeviceManagerCallback deviceCallback = new DeviceManagerCallback() {
    @Override public void onStatusUp() {}
    @Override public void onStatusDown(String errorCode) {}
};

private DataHandlingCallback dataCallback = new DataHandlingCallback() {
    @Override public void onReadDataSuccess(ArrayList<Vehicle> vehicles) {}
    @Override public void onReadDataFailed(String errorCode) {}
    @Override public void onWriteDataSuccess(String operationMode) {}
    @Override public void onWriteDataFailed(String operationMode, String errorCode) {}
};
```

### Get Status

```java
everLPRConnect.getStatus();
```

### Read data

```java
everLPRConnect.readData();
```

### Update multiple data

```java
ArrayList<Vehicle> vehicles = new ArrayList<>();
...
everLPRConnect.updateMultipleData(vehicles);
```

### Update single data

```java
Vehicle vehicle = new Vehicle();
...
everLPRConnect.updateData(vehicle);
```

### Add single data

```java
Vehicle vehicles = new Vehicle();
...
everLPRConnect.addData(vehicle);
```

### Delete data

```java
...
everLPRConnect.deleteData(vehicle.getId());
```

# Example Code

[app module](https://github.com/Ever-AI-Technologies/ever-lpr-connect/tree/master/EverLPRConnect/app)

# License

```
MIT License

Copyright (c) 2017 Michel Omar Aflak

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

