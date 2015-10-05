package eu.smartsantander.androidExperimentation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by amaxilatis on 10/5/15.
 */
public class ActivityRecognitionService extends IntentService {

    private static final String TAG = "ActivityRecognition";
    private Context context;
    private int activityConfidence;
    private int activityCode;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ActivityRecognitionService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            Log.i(TAG, getType(result.getMostProbableActivity().getType()) + "t" + result.getMostProbableActivity().getConfidence());

            context = getApplicationContext();
            Log.d("o3nWatcherLog", "ActivityRecognitionService onHandleIntent called...");

            activityConfidence = result.getMostProbableActivity().getConfidence();
            activityCode = result.getMostProbableActivity().getType();

            Log.d("o3nWatcherLog", " ACTIVITY CODE : " + activityCode + " ACTIVITY CONFIDENCE : " + activityConfidence);

            // Evaluate the avtivity recognition result
            evaluateActivityResult();
        }
    }

    private void evaluateActivityResult() {

    }

    // This method is only used in a log line to have readable status in logs
    private String getType(int type) {
        if (type == DetectedActivity.UNKNOWN)
            return "UNKNOWN";
        else if (type == DetectedActivity.IN_VEHICLE)
            return "IN_VEHICLE";
        else if (type == DetectedActivity.ON_BICYCLE)
            return "ON_BICYCLE";
        else if (type == DetectedActivity.ON_FOOT)
            return "ON_FOOT";
        else if (type == DetectedActivity.STILL)
            return "STILL";
        else if (type == DetectedActivity.TILTING)
            return "TILTING";
        else
            return "";
    }

}
