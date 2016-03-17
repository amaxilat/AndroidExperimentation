package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;


import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import eu.smartsantander.androidExperimentation.jsonEntities.Badge;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.RankingEntry;
import eu.smartsantander.androidExperimentation.jsonEntities.SmartphoneStatistics;
import eu.smartsantander.androidExperimentation.operations.Communication;
import eu.smartsantander.androidExperimentation.operations.PhoneProfiler;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
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
    private BarChart mBarChart;
    private StatisticsTab thisActivity;
    private MapFragment mMap;
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    private List<LatLng> heatMapItems;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics_tab);

        thisActivity = this;

        final SharedPreferences prefs = getSharedPreferences("OrganicityConfigurations", Context.MODE_PRIVATE);

        prefs.registerOnSharedPreferenceChangeListener(this);

        // fill textViews with device data
        fillStatsFields();

        // call the AsyncTask to get the stats picture
        mBarChart = (BarChart) findViewById(R.id.barchart);
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
        mMap.getMap().setMyLocationEnabled(true);
        heatMapItems = new ArrayList<>();
        heatMapItems.add(new LatLng(0, 0));
        mProvider = new HeatmapTileProvider.Builder().data(heatMapItems).build();
        mOverlay = mMap.getMap().addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {

    }

    private LatLngBounds updateExperimentDeviceHeatMap() {
        final JSONArray mapStats = new Communication().getLastPoints(DynamixService.getPhoneProfiler().getPhoneId());
        if (mapStats != null) {
            final LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                int phoneId = DynamixService.getPhoneProfiler().getPhoneId();
                Experiment exp = DynamixService.getExperiment();

                SmartphoneStatistics tempStats = null;
                if (exp == null) {
                    tempStats = new Communication().getSmartphoneStatistics(phoneId);
                } else {
                    tempStats = new Communication().getSmartphoneStatistics(phoneId, exp.getId());
                }
                final SmartphoneStatistics smartphoneStatistics = tempStats;

                final SortedMap<Long, Long> sortedMap = new TreeMap<>(new Comparator<Long>() {
                    @Override
                    public int compare(Long lhs, Long rhs) {
                        return (int) (rhs - lhs);
                    }
                });
                sortedMap.putAll(smartphoneStatistics.getLast7Days());
                thisActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBarChart.clearChart();
                        for (Long integer : sortedMap.keySet()) {
                            mBarChart.addBar(new BarModel(integer.toString(), sortedMap.get(integer).floatValue(), 0xFF1FF4AC));
                        }
                        mBarChart.startAnimation();
                    }
                });

                try {
                    final LatLngBounds bounds = updateExperimentDeviceHeatMap();
                    if (bounds != null) {
                        thisActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMap.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 10));
                            }
                        });
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.clearTileCache();
                        }
                    });
                } catch (IllegalStateException e) {

                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final TextView experimentsTodayTextView = (TextView) findViewById(R.id.stats_number_exp_value);
                        experimentsTodayTextView.setText("");
                        final TextView readingsTodayTextView = (TextView) findViewById(R.id.stats_number_readings_value);
                        readingsTodayTextView.setText("" + smartphoneStatistics.getExperimentReadings());

                        final TextView experimentsAllTextView = (TextView) findViewById(R.id.stats_number_exp_value_a);
                        experimentsAllTextView.setText("" + smartphoneStatistics.getExperiments());
                        final TextView readingsAllTextView = (TextView) findViewById(R.id.stats_number_readings_value_a);
                        readingsAllTextView.setText("" + smartphoneStatistics.getReadings());
                    }
                });


                //-------------------------------------//

                if (smartphoneStatistics.getExperimentRankings() != null) {
                    try {
                        final SortedSet<RankingEntry> list = new TreeSet<>(new Comparator<RankingEntry>() {
                            @Override
                            public int compare(RankingEntry o1, RankingEntry o2) {
                                return (int) (o2.getCount() - o1.getCount());
                            }
                        });
                        list.addAll(smartphoneStatistics.getRankings());
                        int ranking = 0;
                        for (final RankingEntry entry : list) {
                            ranking++;
                            if (entry.getPhoneId() == phoneId) {
                                Log.i(TAG, "My Ranking " + ranking);
                                final int finalRanking = ranking;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final TextView rankingTextView = (TextView) findViewById(R.id.rankingExperimentMessage);
                                        rankingTextView.setText("Position " + finalRanking + " of " + list.size());
                                    }
                                });
                            }
                        }
                    } catch (NullPointerException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //TODO: not here no experiment
                            }
                        });
                    }
                }
                if (smartphoneStatistics.getRankings() != null) {
                    try {
                        final SortedSet<RankingEntry> list = new TreeSet<>(new Comparator<RankingEntry>() {
                            @Override
                            public int compare(RankingEntry o1, RankingEntry o2) {
                                return (int) (o2.getCount() - o1.getCount());
                            }
                        });
                        list.addAll(smartphoneStatistics.getRankings());
                        int ranking = 0;
                        for (final RankingEntry entry : list) {
                            ranking++;
                            if (entry.getPhoneId() == phoneId) {
                                Log.i(TAG, "My Ranking " + ranking);
                                final int finalRanking = ranking;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final TextView rankingTextView = (TextView) findViewById(R.id.rankingMessage);
                                        rankingTextView.setText("Position " + finalRanking + " of " + list.size());
                                    }
                                });
                            }
                        }
                    } catch (NullPointerException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //TODO: not here no rankings
                            }
                        });
                    }
                }

                if (smartphoneStatistics.getExperimentBadges() != null) {
                    try {
                        final SortedSet<Badge> badgesList = new TreeSet<>(new Comparator<Badge>() {
                            @Override
                            public int compare(Badge o1, Badge o2) {
                                return (int) (o2.getTimestamp() - o1.getTimestamp());
                            }
                        });
                        badgesList.addAll(smartphoneStatistics.getExperimentBadges());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView rankingTextView = (TextView) findViewById(R.id.badgesExperimentMessage);
                                rankingTextView.setText(badgesList.size() + " badges earned");
                            }
                        });

                    } catch (NullPointerException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //TODO: not here no experiment
                            }
                        });
                    }
                }
                if (smartphoneStatistics.getBadges() != null) {
                    try {
                        final SortedSet<Badge> badgesList = new TreeSet<>(new Comparator<Badge>() {
                            @Override
                            public int compare(Badge o1, Badge o2) {
                                return (int) (o2.getTimestamp() - o1.getTimestamp());
                            }
                        });
                        badgesList.addAll(smartphoneStatistics.getBadges());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView rankingTextView = (TextView) findViewById(R.id.badgesMessage);
                                rankingTextView.setText(badgesList.size() + " badges earned");
                            }
                        });
                    } catch (NullPointerException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //TODO: not here no rankings
                            }
                        });
                    }
                }
            }
        }).start();
        super.onResume();

    }

    public void fillStatsFields() {

        final TextView timeTodayTextView = (TextView) findViewById(R.id.stats_time_value);
        final TextView experimentsTodayTextView = (TextView) findViewById(R.id.stats_number_exp_value);
        final TextView readingsTodayTextView = (TextView) findViewById(R.id.stats_number_readings_value);
        final TextView timeAllTextView = (TextView) findViewById(R.id.stats_time_value_a);
        final TextView experimentsAllTextView = (TextView) findViewById(R.id.stats_number_exp_value_a);
        final TextView readingsAllTextView = (TextView) findViewById(R.id.stats_number_readings_value_a);

        timeTodayTextView.setText(String.format("%.3g",
                (float) TimeUnit.MILLISECONDS.toSeconds(DynamixService.getTotalTimeConnectedOnline()) / 3600));
        timeAllTextView.setText(String.format("%.3g",
                (float) TimeUnit.MILLISECONDS.toSeconds(DynamixService.getTotalTimeConnectedOnline()) / 3600));
        final int experiments = DynamixService.getPhoneProfiler().getExperiments().size();
        experimentsTodayTextView.setText(String.valueOf(experiments));
        experimentsAllTextView.setText(String.valueOf(experiments));
        final long totalMsg = DynamixService.getPhoneProfiler().getTotalReadingsProduced();
        readingsTodayTextView.setText(Long.toString(totalMsg));
        readingsAllTextView.setText(Long.toString(totalMsg));

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "StatisticsTab Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://eu.smartsantander.androidExperimentation.tabs/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "StatisticsTab Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://eu.smartsantander.androidExperimentation.tabs/http/host/path")
//        );
//        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
