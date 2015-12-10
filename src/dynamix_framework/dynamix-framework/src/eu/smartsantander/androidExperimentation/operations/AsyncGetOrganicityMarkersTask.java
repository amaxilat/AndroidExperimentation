package eu.smartsantander.androidExperimentation.operations;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.HomeActivity;
import org.springframework.web.client.HttpClientErrorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import eu.smartsantander.androidExperimentation.jsonEntities.Entities;

public class AsyncGetOrganicityMarkersTask extends AsyncTask<HomeActivity, LatLng, String> {
    private final String TAG = "GetMarkersTask";
    private HomeActivity activity;

    @Override
    protected String doInBackground(final HomeActivity... params) {
        activity = params[0];
        try {
            final URL yahoo = new URL("http://ec2-54-68-181-32.us-west-2.compute.amazonaws.com:8090/v1/entities");
            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yahoo.openStream()));

            String inputLine;
            final StringBuilder sb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
            in.close();
            final List<Entities> entities = new ObjectMapper().readValue(sb.toString(),
                    new TypeReference<List<Entities>>() {
                    });
            for (final Entities entity : entities) {
                Log.i(TAG, "Adding : " + entity);
                double lat = entity.getData().getLocation().getLatitude();
                double lon = entity.getData().getLocation().getLongitude();
                publishProgress(new LatLng(lat, lon));
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return "AndroidExperimentation Async Experiment Task Executed";
    }

    @Override
    protected void onPostExecute(final String result) {
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(final LatLng... values) {
        final MarkerOptions marker = new MarkerOptions();
        marker.position(values[0]);
        activity.mMap.getMap().addMarker(marker);
    }

    @Override
    protected void onCancelled() {
    }

}
