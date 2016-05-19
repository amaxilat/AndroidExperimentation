package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.ambientdynamix.core.BaseActivity;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.OrganicityProfile;
import eu.smartsantander.androidExperimentation.operations.Communication;
import eu.smartsantander.androidExperimentation.operations.OrganicityAAA;
import gr.cti.android.experimentation.model.RankingEntry;
import gr.cti.android.experimentation.model.SmartphoneStatistics;
import gr.cti.android.experimentation.model.UsageEntry;

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
    private OrganicityProfile profile;
    private int phoneId = -1;

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

        final TextView title = (TextView) findViewById(R.id.heatmaptitle);
        final View map = findViewById(R.id.map);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (heatMapItems.size() == 1) {
                    title.setVisibility(View.GONE);
                    map.setVisibility(View.GONE);
                } else {
                    title.setVisibility(View.VISIBLE);
                    map.setVisibility(View.VISIBLE);
                }
            }
        });
        mProvider = new HeatmapTileProvider.Builder().data(heatMapItems).build();
        mOverlay = mMap.getMap().addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {

    }

    private LatLngBounds updateExperimentDeviceHeatMap(final SmartphoneStatistics smartphoneStatistics) {
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
                Log.i(TAG, "profile:" + BaseActivity.access_token);

                if (profile == null) {
                    final String accessToken = getApplicationContext().getSharedPreferences("aaa", MODE_PRIVATE).getString("access_token", null);
                    if (accessToken != null) {
                        profile = new OrganicityAAA().getProfile(accessToken);
                        Log.i(TAG, "profile:" + profile);
                        if (profile != null) {
                            final TextView statsName = (TextView) findViewById(R.id.stats_name);
                            final TextView statsUsername = (TextView) findViewById(R.id.stats_email);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    statsName.setText(profile.getName());
                                    statsUsername.setText(profile.getEmail());
                                }
                            });
                        }
                    }
                }

                phoneId = DynamixService.getPhoneProfiler().getPhoneId();
                final Experiment exp = DynamixService.getExperiment();

                SmartphoneStatistics tempStats;
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
                    final LatLngBounds bounds = updateExperimentDeviceHeatMap(smartphoneStatistics);
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
                    final LinearLayout currentExperimentLayout = (LinearLayout) findViewById(R.id.currentExperimentLayout);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currentExperimentLayout.setVisibility(View.VISIBLE);
                        }
                    });
                    try {
                        final SortedSet<RankingEntry> list = new TreeSet<>(new Comparator<RankingEntry>() {
                            @Override
                            public int compare(RankingEntry o1, RankingEntry o2) {
                                return (int) (o2.getCount() - o1.getCount());
                            }
                        });
                        list.addAll(smartphoneStatistics.getExperimentRankings());
                        int ranking = 0;
                        for (final RankingEntry entry : list) {
                            ranking++;
                            if (entry.getPhoneId() == phoneId) {
                                Log.i(TAG, "My Ranking " + ranking);
                                final int finalRanking = ranking;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final TextView experimentRankingTextView = (TextView) findViewById(R.id.rankingExperimentMessage);
                                        experimentRankingTextView.setText("Position " + finalRanking + " of " + list.size());
                                    }
                                });
                            }
                        }
                    } catch (NullPointerException ignore) {

                    }
                } else {
                    final LinearLayout currentExperimentLayout = (LinearLayout) findViewById(R.id.currentExperimentLayout);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currentExperimentLayout.setVisibility(View.GONE);
                        }
                    });
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
                    } catch (NullPointerException ignore) {

                    }
                }

                if (smartphoneStatistics.getExperimentBadges() != null) {
                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView experimnetBadgesTextView = (TextView) findViewById(R.id.badgesExperimentMessage);
                                experimnetBadgesTextView.setText(
                                        smartphoneStatistics.getExperimentBadges().size() + " badges earned");
                            }
                        });

                    } catch (NullPointerException ignore) {

                    }
                }
                if (smartphoneStatistics.getBadges() != null) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView badgesTextView = (TextView) findViewById(R.id.badgesMessage);
                                badgesTextView.setText(
                                        smartphoneStatistics.getBadges().size() + " badges earned");
                            }
                        });
                    } catch (NullPointerException ignore) {
                    }
                }
                if (smartphoneStatistics.getExperimentUsage() != null) {
                    try {
                        long total = 0;
                        for (UsageEntry
                                entry : smartphoneStatistics.getExperimentUsage()) {
                            total += entry.getTime();
                        }
                        final long totalF = total;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView badgesTextView = (TextView) findViewById(R.id.stats_time_value);
                                badgesTextView.setText(
                                        (totalF / 60) + " hours");
                            }
                        });

                    } catch (NullPointerException ignore) {

                    }
                }
                if (smartphoneStatistics.getUsage() != null) {
                    try {
                        long total = 0;
                        for (UsageEntry
                                entry : smartphoneStatistics.getUsage()) {
                            total += entry.getTime();
                        }
                        final long totalF = total;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView badgesTextView = (TextView) findViewById(R.id.stats_time_value_a);
                                badgesTextView.setText(
                                        (totalF / 60) + " hours");
                            }
                        });
                    } catch (NullPointerException ignore) {
                    }
                }
            }
        }).start();
        super.onResume();

    }

    public void fillStatsFields() {

        final TextView timeTodayTextView = (TextView) findViewById(R.id.stats_time_value);
        final TextView timeAllTextView = (TextView) findViewById(R.id.stats_time_value_a);

        timeTodayTextView.setText(String.format("%.3g",
                (float) TimeUnit.MILLISECONDS.toSeconds(DynamixService.getTotalTimeConnectedOnline()) / 3600));
        timeAllTextView.setText(String.format("%.3g",
                (float) TimeUnit.MILLISECONDS.toSeconds(DynamixService.getTotalTimeConnectedOnline()) / 3600));
    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        client.disconnect();
    }
}
