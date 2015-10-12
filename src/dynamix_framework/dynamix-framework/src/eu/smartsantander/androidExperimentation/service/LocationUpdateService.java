package eu.smartsantander.androidExperimentation.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import org.ambientdynamix.util.Log;

public class LocationUpdateService extends IntentService {
    private static final String TAG = "LocationUpdateService";

    private Context context;
    private int activityConfidence;
    private int activityCode;

    public LocationUpdateService() {
        super("LocationUpdateService");
    }

    public LocationUpdateService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public void onLocationChanged(Location location) {
        String message = String.format(
                "Lon %1$s Lat: %2$s",
                location.getLongitude(), location.getLatitude());
        Log.d(TAG, message);
        //noteManager.postNotification(message);
    }
}
