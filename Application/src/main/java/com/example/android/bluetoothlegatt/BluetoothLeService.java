/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.NotificationManager;
import android.app.Service;
import android.os.SystemClock;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private ArrayList<BluetoothGattCharacteristic> mGattCharacteristics =
            new ArrayList<BluetoothGattCharacteristic>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private SomeBackgroundProcess pullData = null;
    private RequestQueue queue;

    // services for posting data
    private String mUsername;
    private String mAPIKey;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private Map<UUID, Integer> characteristicCurrentValues = new HashMap<UUID, Integer>();

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
                if (pullData == null) {
                    pullData = new SomeBackgroundProcess();
                    pullData.start();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                if (pullData != null) {
                    pullData.stop();
                    pullData = null;
                }
                broadcastUpdate(intentAction);
                // try to reconnect
                connect(mBluetoothDeviceAddress);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG,"onCharacteristicRead (" + Integer.toString(status) + ") " + characteristic.getUuid().toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }


    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {


        final Intent intent = new Intent(action);

        Integer format = Globals.de0gee_characteristic_format.get(characteristic.getUuid());
        if (format == null) {
            Log.w(TAG, "got null format");
            return;
        }
        Integer id = Globals.de0gee_characteristic_id.get(characteristic.getUuid());
        if (id == null) {
            Log.w(TAG, "got null id");
            return;
        }

        final int sensorValue = characteristic.getIntValue(format, 0);

        int lastSensor = -1;
        try {
            lastSensor = characteristicCurrentValues.get(characteristic.getUuid()).intValue();
        } catch (Exception e) {
            lastSensor = -1;
            // do nothing
        }
        if (sensorValue == lastSensor) {
            characteristicCurrentValues.put(characteristic.getUuid(), sensorValue);
            sendData(id, sensorValue);
        } else {
            characteristicCurrentValues.put(characteristic.getUuid(), sensorValue);
            sendData(id, sensorValue);
            intent.putExtra(EXTRA_DATA, String.valueOf(sensorValue));
        }

        pullData.didRead();
        sendBroadcast(intent);
    }

    public void sendData(final int sensorID, final int sensorValue) {
        try {
            String URL = Globals.SERVER_ADDRESS + "/sensor";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("a", mAPIKey);
            jsonBody.put("s", sensorID); // sensor ID
            jsonBody.put("v", sensorValue);
            jsonBody.put("t", System.currentTimeMillis());
            final String mRequestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
//                    Log.i("LOG_VOLLEY", response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("LOG_VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {

                        responseString = String.valueOf(response.statusCode);

                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            queue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if (pullData == null) {
            pullData = new SomeBackgroundProcess();
            pullData.start();
        }

        queue = Volley.newRequestQueue(this);
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.d(TAG, "Bluetooth disconnect() called");
        if (pullData != null) {
            pullData.stop();
            pullData = null;
        }
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Log.d(TAG, "Bluetooth close() called");
        if (pullData != null) {
            pullData.stop();
            pullData = null;
        }
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Set username and password
     */
    public void setUsernameAndPassword(String username, String apikey) {
        mUsername = username;
        mAPIKey = apikey;
        return;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        Log.d(TAG,"getSupportedGattServices()");
        if (mBluetoothGatt == null) return null;
        List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();

        mGattCharacteristics = new ArrayList<BluetoothGattCharacteristic>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                mGattCharacteristics.add(gattCharacteristic);
            }
        }

        // initialize the GATT services
        return gattServices;
    }

    public class SomeBackgroundProcess implements Runnable {

        Thread backgroundThread;
        private int count = 0;
        private boolean hasRead = true;

        public void didRead() {
            synchronized ((Object) hasRead) {
                hasRead = true;
            }
        }

        public void start() {
            if (backgroundThread == null) {
                backgroundThread = new Thread(this);
                backgroundThread.start();
            }
        }

        public void stop() {
            if (backgroundThread != null) {
                backgroundThread.interrupt();
            }
        }

        public void run() {
            boolean tryAgain = false;
            try {
                Log.i(TAG, "Thread starting.");
                while (!this.backgroundThread.interrupted()) {
                    // get the specified data
                    for (BluetoothGattCharacteristic gattCharacteristic : mGattCharacteristics) {
                        final long elapsedThreadMillis = SystemClock.currentThreadTimeMillis();
                        while (true) {
                            // wait
                            if (SystemClock.currentThreadTimeMillis() - elapsedThreadMillis > 5000) {
                                synchronized ((Object) hasRead) {
                                    hasRead = true;
                                }
                            }
                            synchronized ((Object) hasRead) {
                                if (hasRead == true) {
                                    break;
                                }
                            }
                        }
                        Integer steps = Globals.de0gee_characteristic_steps.get(gattCharacteristic.getUuid());
                        if (steps == null) {
                            continue;
                        }
                        if (count % steps == 0) {
                            try {
                                synchronized ((Object) hasRead) {
                                    hasRead = false;
                                }
                                mBluetoothGatt.readCharacteristic(gattCharacteristic);
                            } catch (Exception e) {
                                Log.d(TAG,"problem reading " + e.toString());
                                backgroundThread.interrupt();
                                break;
                            }
                        }
                    }
                    // iterate the counter
                    count = count + 1;
                }
                Log.i(TAG, "Thread stopping.");
            } catch (java.util.ConcurrentModificationException e) {
                Log.d(TAG, "Thread stopped, concurrent modification, trying again " + e.toString());
                tryAgain = true;
            } catch (Exception e) {
                // important you respond to the InterruptedException and stop processing
                // when its thrown!  Notice this is outside the while loop.
                Log.d(TAG, "Thread stopped: " + e.toString());
            } finally {
                backgroundThread = null;
                if (tryAgain == true) {
                    backgroundThread = new Thread(this);
                    backgroundThread.start();
                }
            }
        }
    }

}
