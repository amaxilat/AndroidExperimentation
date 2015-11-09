package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.core.DynamixService;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import eu.smartsantander.androidExperimentation.util.Constants;

public class Demon extends Service {
    private final static String TAG = "Demon";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

//        if (DynamixService.isEnabled()) {
//            if (!DynamixService.isDeviceRegistered()) {
//                Log.d(TAG, "AndroidExperimentation Running Unregistered Device");
//                DynamixService.getPhoneProfiler().register();
//            } else if (DynamixService.isInitialized()) {
//                Log.i(TAG, "Will execute experiment");
//                AsyncExpTask experiment = new AsyncExpTask();
//                experiment.execute();
//            }
//        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
    }
}
