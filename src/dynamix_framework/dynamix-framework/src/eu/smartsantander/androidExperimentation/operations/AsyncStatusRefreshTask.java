package eu.smartsantander.androidExperimentation.operations;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.ambientdynamix.core.DynamixApplication;
import org.ambientdynamix.core.DynamixApplicationAdapter;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.HomeActivity;
import org.ambientdynamix.core.R;
import org.ambientdynamix.data.DynamixPreferences;
import org.ambientdynamix.util.Log;

import java.util.ArrayList;

import eu.smartsantander.androidExperimentation.Constants;

public class AsyncStatusRefreshTask extends AsyncTask<HomeActivity, String, Integer> {
    private final String TAG = this.getClass().getSimpleName();
    HomeActivity activity;

    @Override
    protected Integer doInBackground(HomeActivity... params) {
        activity = params[0];

        //SmartSantander
        if (DynamixService.isEnabled()) {
            if (!DynamixService.isDeviceRegistered()) {
                DynamixService.getPhoneProfiler().register();
                publishProgress("phone", "message");
            } else {
                publishProgress("phone", "Device ID:" + String.valueOf(DynamixService.getPhoneProfiler().getPhoneId()));
                DynamixService.getPhoneProfiler().savePrefs();
            }
        } else {
            publishProgress("phone", String.valueOf("Device ID: Not Connected"));
        }

        if (DynamixService.getExperiment() != null) {
            publishProgress("phone", "Device ID: " + String.valueOf(DynamixService.getPhoneProfiler().getPhoneId()));
            publishProgress("experiment", "Id: " + String.valueOf(DynamixService.getExperiment().getId()) + " Name: ");
            publishProgress("experimentDescription", String.valueOf(DynamixService.getExperiment().getName()));
        }

        if (DynamixService.getConnectionStatus() && DynamixService.isEnabled()) {
            publishProgress("status", "Connected with Server");
        } else {
            publishProgress("status", "Disconnected from Server");
        }

        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
    }


    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if ("phone".equals(values[0])) {
            activity.phoneIdTv.setText(String.valueOf(values[1]));
        } else if ("experiment".equals(values[0])) {
            activity.expIdTv.setText(values[1]);
        } else if ("experimentDescription".equals(values[0])) {
            activity.expDescriptionTv.setText(values[1]);
        } else if ("status".equals(values[0])) {
            activity.connectionStatus.setText(values[1]);
        }
    }

    @Override
    protected void onCancelled() {
    }

}
