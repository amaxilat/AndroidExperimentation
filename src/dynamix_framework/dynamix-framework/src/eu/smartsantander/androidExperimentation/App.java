package eu.smartsantander.androidExperimentation;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseUser;
import com.splunk.mint.Mint;

import org.ambientdynamix.util.Log;

import eu.smartsantander.androidExperimentation.operations.AsyncConstantsTask;

public class App extends Application {

    private static final String TAG = "App";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Initializing...");

        Mint.initAndStartSession(getApplicationContext(), "6d443500");


        Parse.enableLocalDatastore(this.getApplicationContext());
        Parse.initialize(this.getApplicationContext(), "0MnJVDC7k6ySseWr771fSxhsE9IwDwrY9tvwEDeC", "A51n4N3wjX9AxWs0XbtQ99omRbRmYYAZh1WUicmm"); // Your Application ID and Client Key are defined elsewhere
        ParseUser.enableAutomaticUser();

        try {
            new AsyncConstantsTask().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}