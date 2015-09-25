package eu.smartsantander.androidExperimentation;

import android.app.Application;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.bugsense.trace.ExceptionCallback;
import com.newrelic.agent.android.NewRelic;
import com.parse.Parse;
import com.parse.ParseUser;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.util.Log;

import eu.smartsantander.androidExperimentation.operations.AsyncConstantsTask;

public class App extends Application implements ExceptionCallback {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Initializing...");
        BugSenseHandler.initAndStartSession(this.getApplicationContext(), "91ce9553");
        BugSenseHandler.setExceptionCallback(this);

        NewRelic.withApplicationToken(
                "AA0fe9525a8553b77ed9ab623937bcd1bf403c6775"
        ).start(this.getApplicationContext());

        Parse.enableLocalDatastore(this.getApplicationContext());
        Parse.initialize(this.getApplicationContext(), "0MnJVDC7k6ySseWr771fSxhsE9IwDwrY9tvwEDeC", "A51n4N3wjX9AxWs0XbtQ99omRbRmYYAZh1WUicmm"); // Your Application ID and Client Key are defined elsewhere
        ParseUser.enableAutomaticUser();


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
}