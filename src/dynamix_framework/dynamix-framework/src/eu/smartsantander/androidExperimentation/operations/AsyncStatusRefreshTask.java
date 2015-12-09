package eu.smartsantander.androidExperimentation.operations;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.HomeActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import eu.smartsantander.androidExperimentation.fragment.SensorMeasurement;
import eu.smartsantander.androidExperimentation.jsonEntities.Reading;
import eu.smartsantander.androidExperimentation.jsonEntities.Report;

public class AsyncStatusRefreshTask extends AsyncTask<Void, String, Integer> {
    private final String TAG = this.getClass().getSimpleName();
    private static final String GPS_PLUGIN = "org.ambientdynamix.contextplugins.GpsPlugin";
    HomeActivity activity;
    private String lastMessage;

    public AsyncStatusRefreshTask(final HomeActivity homeActivity) {
        this.activity = homeActivity;
        this.lastMessage = lastMessage;
    }

    @Override
    protected Integer doInBackground(Void... params) {

        //SmartSantander
        if (DynamixService.isEnabled()) {
            if (!DynamixService.isDeviceRegistered()) {
                DynamixService.getPhoneProfiler().register();
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
            if (DynamixService.getExperiment().getUrlDescription() != null) {
                publishProgress("experimentUrlDescription", String.valueOf(DynamixService.getExperiment().getUrlDescription()));
            }
        }

        if (DynamixService.getConnectionStatus() && DynamixService.isEnabled()) {
            publishProgress("status", "Connected with Server");
        } else {
            publishProgress("status", "Disconnected from Server");
        }


        if (DynamixService.isFrameworkInitialized()) {
            //get the last experiment message and show the changes in the home activity
            parseExperimentMessage(DynamixService.getCommunication().getLastMessage());
        }
        return 0;
    }


    private void parseExperimentMessage(final String message) {
        if (message != null && !message.equals(lastMessage)) {
            Log.i(TAG, message);
            // Add the fragment to the 'fragment_container' FrameLayout
            try {
                final Report report = new ObjectMapper().readValue(message, Report.class);
                for (final String result : report.getJobResults()) {
                    final Reading reading = Reading.fromJson(result);
                    if (GPS_PLUGIN.equals(reading.getContext())) {
                        try {
                            final JSONObject obj = new JSONObject(reading.getValue());
                            Double longitude = null;
                            Double latitude = null;
                            final Iterator keysIterator = obj.keys();
                            while (keysIterator.hasNext()) {
                                final String next = (String) keysIterator.next();
                                // Add item to adapter
                                try {
                                    if (next.toLowerCase().contains("longitude")) {
                                        final Double doubleVal = Double.valueOf((Integer) obj.get(next));
                                        if (doubleVal > 0) {
                                            longitude = doubleVal;
                                        }
                                    } else if (next.toLowerCase().contains("latitude")) {
                                        final Double doubleVal = Double.valueOf((Integer) obj.get(next));
                                        if (doubleVal > 0) {
                                            latitude = doubleVal;
                                        }
                                    }
                                } catch (Exception ignore) {
                                }
                            }
                            if (longitude != null && latitude != null) {
                                publishProgress("location-changed",
                                        String.valueOf(latitude), String.valueOf(longitude));
                            }
                        } catch (Exception ignore) {
                        }
                        continue;
                    }
                    try {
                        showMeasurementSparkLines(reading);
                    } catch (Exception ignore) {
                    }

                }
            } catch (IOException ignore) {
            }
        }
    }

    private void showMeasurementSparkLines(final Reading reading) throws JSONException {

        final JSONObject obj = new JSONObject(reading.getValue());
        final Iterator keyIterator = obj.keys();
        while (keyIterator.hasNext()) {
            final String next = (String) keyIterator.next();
            // Add item to adapter
            Double value = null;
            try {
                value = (Double) obj.get(next);
            } catch (ClassCastException e) {
                try {
                    value = Double.valueOf((Integer) obj.get(next));
                } catch (Exception ignore) {
                }
            }
            if (value != null) {
                publishProgress("spark-line", next, String.valueOf(value));
            }
        }
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
        } else if ("experimentDescription".equals(values[0])) {
            activity.expDescriptionTv.setText(values[1]);
        } else if ("status".equals(values[0])) {

        } else if ("experimentUrlDescription".equals(values[0])) {

        } else if ("location-changed".equals(values[0])) {
            activity.updateMapLocation(values[1], values[2]);
        } else if ("spark-line".equals(values[0])) {
            Log.i(TAG, "Progress: spark-line");
            String next = values[1];
            double value = Double.parseDouble(values[2]);
            Log.i(TAG, "Progress: sensors:" + activity.sensorMeasurements.size());
            boolean found = false;
            for (final SensorMeasurement sensorMeasurement : activity.sensorMeasurements) {
                if (sensorMeasurement.getType().equals(next)) {
                    found = true;
                    sensorMeasurement.add(value);
                    activity.sensorMeasurementAdapter.notifyDataSetChanged();
                    break;
                }
            }
            if (!found) {
                final SensorMeasurement measurement = new SensorMeasurement(next, value);
                activity.sensorMeasurements.add(measurement);
            }
        }
    }

    @Override
    protected void onCancelled() {
    }

}
