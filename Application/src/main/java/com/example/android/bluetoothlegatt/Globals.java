package com.example.android.bluetoothlegatt;

/**
 * Created by zacks on 2/28/2018.
 */


import android.bluetooth.BluetoothGattCharacteristic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class Globals {
    public static final String SERVER_ADDRESS = "https://cloud.de0gee.com";
//public static final String SERVER_ADDRESS = "http://192.168.0.23:8002";
    public static final String WEBSOCKET_ADDRESS = "wss://cloud.de0gee.com/ws2";

    public static final UUID TEMPERATURE= UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb");
    public static final UUID HUMIDTY= UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb");
    public static final UUID AMBIENT_LIGHT= UUID.fromString("c24229aa-d7e4-4438-a328-c2c548564643");
    public static final UUID PRESSURE= UUID.fromString("2f256c42-cdef-4378-8e78-694ea0f53ea8");
    public static final UUID BATTERY= UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID MOTION_SENSOR= UUID.fromString("15e438b8-558e-4b1f-992f-23f90a8c129b");

    public static Map<UUID, Integer> de0gee_characteristic_id = new HashMap<UUID,Integer>();
    static {
        de0gee_characteristic_id.put(TEMPERATURE,1);
        de0gee_characteristic_id.put(HUMIDTY,2);
        de0gee_characteristic_id.put(AMBIENT_LIGHT,3);
        de0gee_characteristic_id.put(PRESSURE,4);
        de0gee_characteristic_id.put(BATTERY,5);
        de0gee_characteristic_id.put(MOTION_SENSOR,6);
    }

    public static Map<UUID, Integer> de0gee_characteristic_format  = new HashMap<UUID,Integer>();
    static {
        de0gee_characteristic_format.put(TEMPERATURE,BluetoothGattCharacteristic.FORMAT_UINT16);
        de0gee_characteristic_format.put(HUMIDTY,BluetoothGattCharacteristic.FORMAT_UINT8);
        de0gee_characteristic_format.put(AMBIENT_LIGHT,BluetoothGattCharacteristic.FORMAT_UINT32);
        de0gee_characteristic_format.put(PRESSURE,BluetoothGattCharacteristic.FORMAT_UINT16);
        de0gee_characteristic_format.put(BATTERY,BluetoothGattCharacteristic.FORMAT_UINT8);
        de0gee_characteristic_format.put(MOTION_SENSOR,BluetoothGattCharacteristic.FORMAT_UINT16);
    }

    public static Map<UUID, Integer> de0gee_characteristic_steps  = new HashMap<UUID,Integer>();
    static {
        de0gee_characteristic_steps.put(TEMPERATURE,50);
        de0gee_characteristic_steps.put(HUMIDTY,50);
        de0gee_characteristic_steps.put(AMBIENT_LIGHT,50);
        de0gee_characteristic_steps.put(PRESSURE,50);
        de0gee_characteristic_steps.put(BATTERY,50);
        de0gee_characteristic_steps.put(MOTION_SENSOR,1);
    }
}