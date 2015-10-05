package eu.smartsantander.androidExperimentation;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.bugsense.trace.ExceptionCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.parse.Parse;
import com.parse.ParseUser;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.util.Log;

import eu.smartsantander.androidExperimentation.operations.AsyncConstantsTask;

public class App extends Application implements ExceptionCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "App";

    private GoogleApiClient mGoogleApiClient;
    private PendingIntent pIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Initializing...");
        BugSenseHandler.initAndStartSession(this.getApplicationContext(), "91ce9553");
        BugSenseHandler.setExceptionCallback(this);

        Parse.enableLocalDatastore(this.getApplicationContext());
        Parse.initialize(this.getApplicationContext(), "0MnJVDC7k6ySseWr771fSxhsE9IwDwrY9tvwEDeC", "A51n4N3wjX9AxWs0XbtQ99omRbRmYYAZh1WUicmm"); // Your Application ID and Client Key are defined elsewhere
        ParseUser.enableAutomaticUser();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        try {
            new AsyncConstantsTask().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lastBreath(Exception e) {
        e.printStackTrace();
        DynamixService.logToFile(e.getMessage());
        BugSenseHandler.sendException(e);
        DynamixService.getPhoneProfiler().savePrefs();
        Toast.makeText(this.getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        try {
            Thread.sleep(5000);
        } catch (Exception w) {
        }

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Google activity recognition services connected");
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 10000, pIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google activity recognition services connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Google activity recognition services disconnected");
    }
}