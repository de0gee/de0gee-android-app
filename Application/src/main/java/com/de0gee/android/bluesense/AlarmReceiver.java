package com.de0gee.android.bluesense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by zacks on 3/2/2018.
 */


public class AlarmReceiver extends BroadcastReceiver {
    private static PowerManager.WakeLock wakeLock;

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Recurring alarm");
        PowerManager pm = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "De0geeWakeLock");
        wakeLock.acquire();
        Intent scanService = new Intent(context, BluetoothLeService.class);
        try {
            context.startService(scanService);
            Log.v(TAG, "restarted the service");
        } catch (Exception e) {
            Log.w(TAG,e.toString());
        }
        Log.d(TAG,"Releasing wakelock");
        if (wakeLock != null) wakeLock.release();
        wakeLock = null;
    }


}
