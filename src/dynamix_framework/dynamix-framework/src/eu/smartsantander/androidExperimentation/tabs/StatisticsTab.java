package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import eu.smartsantander.androidExperimentation.operations.Communication;
import eu.smartsantander.androidExperimentation.operations.PhoneProfiler;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * This tab displays overall stats about the activity of this specific device
 * <p/>
 * Uses several textviews to display info regarding statistics of the current user, if any,
 * and a webview to display a graph retrieved as an image from the experimentation server.
 * The image is created at the server using jfreechart and some obscure JSP...
 * <p/>
 * TODO: CHANGE THE WAY WE HANDLE THE SERVER AND JSP URLs
 */

public class StatisticsTab extends Activity implements
        OnSharedPreferenceChangeListener {

    private static final String TAG = "StatsTab";
    private OnSharedPreferenceChangeListener listener;
    private PhoneProfiler pProfil;
    private SharedPreferences prefs;
    private loadJPGstatsTask jpgTask;

    private Communication communication;
    private BarChart mBarChart;
    private StatisticsTab thisActivity;
    private MapFragment mMap;
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    private List<LatLng> heatMapItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statisticsTab);

        pProfil = DynamixService.getPhoneProfiler();

        prefs = getSharedPreferences("OrganicityConfigurations",
                Context.MODE_PRIVATE);

        // update the field dynamically when changed
        listener = new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                // TODO Auto-generated method stub
                Log.v(TAG, "A CHANGE HAS COME!!!!!! " + key);

            }
        };

        thisActivity = this;

        communication = new Communication();

        prefs.registerOnSharedPreferenceChangeListener(listener);

        // fill textViews with device data

        fillStatsFields();

        // call the AsyncTask to get the stats picture
        mBarChart = (BarChart) findViewById(R.id.barchart);
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));

        jpgTask = new loadJPGstatsTask();
        jpgTask.execute();


        mMap.getMap().setMyLocationEnabled(true);

        heatMapItems = new ArrayList<LatLng>();
        heatMapItems.add(new LatLng(0, 0));


        mProvider = new HeatmapTileProvider.Builder().data(heatMapItems).build();
        mOverlay = mMap.getMap().addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // TODO Auto-generated method stub

    }


    // handle the downloading of the statistics JPG from the server as an asyncTask
    // otherwise, the UI blocks in case of network connectivity issues

    private class loadJPGstatsTask extends AsyncTask<String, Integer, String> {

        String theJPGData;


        @Override
        protected void onPreExecute() {
            System.out.println("ENTERING ASYNC TASK");
        }


        @Override
        protected void onPostExecute(String j) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOverlay.clearTileCache();
                }
            });
        }


        @Override
        protected String doInBackground(String... params) {


            final SortedMap<Integer, Double> sortedMap = communication.getLastStatistics(
                    DynamixService.getPhoneProfiler().getPhoneId()
            );

            thisActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBarChart.clearChart();
                    for (Integer integer : sortedMap.keySet()) {
                        mBarChart.addBar(new BarModel(integer.toString(), sortedMap.get(integer).floatValue(), 0xFF1FF4AC));
                    }
                    mBarChart.startAnimation();
                }
            });


            final LatLngBounds bounds = updateExperimentDeviceHeatMap();
            if (bounds != null) {
                thisActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMap.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 10));
                    }
                });
            }

            return theJPGData;
        }


    }

    private LatLngBounds updateExperimentDeviceHeatMap() {
        final JSONArray mapStats = communication.getLastPoints(DynamixService.getPhoneProfiler().getPhoneId());
        if (mapStats != null) {
            LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
            for (int i = 0; i < mapStats.length(); i++) {
                try {
                    final JSONArray elem = mapStats.getJSONArray(i);
                    final LatLng latLng = new LatLng(elem.getDouble(0), elem.getDouble(1));
                    heatMapItems.add(latLng);
                    boundsBuilder.include(latLng);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            mProvider.setData(heatMapItems);
            final LatLngBounds bounds = boundsBuilder.build();
            return bounds;
        }
        return null;
    }

    @Override
    public void onResume() {

        fillStatsFields();

        try {
            jpgTask.execute();
        } catch (Exception e) {
            Log.e(TAG, "Stats:" + e.getMessage());
        }
        super.onResume();

    }

    public void fillStatsFields() {

        TextView statsTextView1 = (TextView) findViewById(R.id.stats_time_title);
        TextView statsTextView2 = (TextView) findViewById(R.id.stats_time_value);
        TextView statsTextView3 = (TextView) findViewById(R.id.stats_number_exp_title);
        TextView statsTextView4 = (TextView) findViewById(R.id.stats_number_exp_value);
        TextView statsTextView5 = (TextView) findViewById(R.id.stats_number_readings_title);
        TextView statsTextView6 = (TextView) findViewById(R.id.stats_number_readings_value);
        TextView statsTextView9 = (TextView) findViewById(R.id.stats_graph_title);

        statsTextView1.setText(getString(R.string.statsTotalTime));

        statsTextView2.setText(String.format("%.3g%n",
                (float) TimeUnit.MILLISECONDS.toSeconds(DynamixService
                        .getTotalTimeConnectedOnline()) / 3600));
        int experiments = DynamixService.getPhoneProfiler().getExperiments()
                .size();
        statsTextView3.setText(getString(R.string.statsNumOfExp));
        statsTextView4.setText(String.valueOf(experiments));
        statsTextView5.setText(getString(R.string.statsNumOfReadings));
        long totalMsg = DynamixService.getPhoneProfiler()
                .getTotalReadingsProduced();
        statsTextView6.setText(Long.toString(totalMsg));
        statsTextView9.setText(getString(R.string.stats7Days));

    }

    public boolean checkNetworkIsAvailable() {
        ConnectivityManager cm = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

}
