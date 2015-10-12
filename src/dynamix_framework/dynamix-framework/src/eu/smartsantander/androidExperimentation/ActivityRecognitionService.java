package eu.smartsantander.androidExperimentation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import eu.smartsantander.androidExperimentation.util.Constants;

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
     */
    public ActivityRecognitionService() {
        super("ActivityRecognitionService");
    }

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
            Constants.activityStatus = getType(result.getMostProbableActivity().getType());
            Log.d("o3nWatcherLog", " ACTIVITY CODE : " + activityCode + " ACTIVITY CONFIDENCE : " + activityConfidence);
        }
    }

    private void evaluateActivityResult() {
    }

    // This method is only used in a log line to have readable status in logs
    private String getType(int type) {
        switch (type) {
            case DetectedActivity.UNKNOWN: {
                return "UNKNOWN";
            }
            case DetectedActivity.IN_VEHICLE: {
                return "IN_VEHICLE";
            }
            case DetectedActivity.ON_BICYCLE: {
                return "ON_BICYCLE";
            }
            case DetectedActivity.ON_FOOT: {
                return "ON_FOOT";
            }
            case DetectedActivity.STILL: {
                return "STILL";
            }
            case DetectedActivity.TILTING: {
                return "TILTING";
            }
            default: {
                return "";
            }
        }
    }

}
