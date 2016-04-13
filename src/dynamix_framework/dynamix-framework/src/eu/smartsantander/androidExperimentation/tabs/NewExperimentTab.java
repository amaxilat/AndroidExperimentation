package eu.smartsantander.androidExperimentation.tabs;

import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.ambientdynamix.data.ExperimentAdapter;
import org.ambientdynamix.util.EmptyListSupportAdapter;
import org.ambientdynamix.util.SeparatedListAdapter;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.smartsantander.androidExperimentation.App;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.operations.Communication;
import eu.smartsantander.androidExperimentation.operations.Downloader;
import eu.smartsantander.androidExperimentation.operations.DynamixServiceListenerUtility;
import eu.smartsantander.androidExperimentation.util.Constants;
import gr.cti.android.experimentation.model.Plugin;

/**
 * This tab displays ....
 */

public class NewExperimentTab extends ListActivity {
    private final static String TAG = "NewExperimentTab";

    private ListView plugList = null;
    private AsyncTask<Void, String, List<Experiment>> runnableUpdate;
    private SeparatedListAdapter adapter;
    private List<Experiment> experiments;
    private SimpleDateFormat sdf;
    private MixpanelAPI mMixpanel;
    private InstalledExperimentAdapter installedAdapter;
    private ExperimentAdapter newExperimentsAdapter;
    private Map<Experiment, Integer> installables = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_experiment_tab);

        sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        experiments = new ArrayList<>();

        mMixpanel = MixpanelAPI.getInstance(this, Constants.MIXPANEL_TOKEN);
        mMixpanel.identify(String.valueOf(DynamixService.getPhoneProfiler().getPhoneId()));

        plugList = getListView();
        plugList.setClickable(true);
        // create our list and custom adapter
        adapter = new SeparatedListAdapter(this);
        installedAdapter = new InstalledExperimentAdapter(this,
                R.layout.experiment_icon_row, experiments,
                getString(R.string.no_experiments), "");
        installedAdapter.setNotifyOnChange(true);
        adapter.addSection(getString(R.string.installed_experiments),
                installedAdapter);
        newExperimentsAdapter = new ExperimentAdapter(
                this,
                R.layout.installable_experiment_row,
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),
                new ArrayList<Experiment>(), installables, false,
                getString(R.string.no_available_experiments),
                getString(R.string.tap_find_plugins));
        newExperimentsAdapter.setNotifyOnChange(true);
        adapter.addSection(getString(R.string.available_experiments),
                newExperimentsAdapter);
        plugList.setAdapter(this.adapter);

//        final ListView experimentListView = (ListView) findViewById(R.id.experiments_list);
//        experimentListView.setAdapter(experimentSelectAdapter);

        runnableUpdate = new AsyncTask<Void, String, List<Experiment>>() {
            @Override
            protected List<Experiment> doInBackground(Void... params) {
                final Communication communication = new Communication();
                try {
                    return communication.getExperiments();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(final List<Experiment> newExperiments) {
                adapter.notifyDataSetChanged();
                for (final Experiment experiment : newExperiments) {
                    newExperimentsAdapter.add(experiment);
                }
            }
        };

        runnableUpdate.execute();

        final Button installExperimentsButton = (Button) findViewById(R.id.btn_install_experiments);
        installExperimentsButton.setOnClickListener(

                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AsyncTask<Void, String, List<Experiment>>() {


                            @Override
                            protected List<Experiment> doInBackground(Void... params) {
                                final String[] smarDeps = DynamixService.getPhoneProfiler().getSensorRules().split(",");
                                if (installables.keySet().isEmpty()) {
                                    return null;
                                }
                                final Experiment experiment = installables.keySet().iterator().next();
                                final String[] expDeps = experiment.getSensorDependencies().split(",");

                                if (!Constants.match(smarDeps, expDeps)) {
                                    final List<String> missingPlugins = new ArrayList<>();
                                    for (final String expDep : expDeps) {
                                        if (!DynamixService.isContextPluginInstalled(expDep)) {
                                            final Plugin plugin = DynamixService.getDiscoveredPluginByContextType(expDep);
                                            if (plugin != null) {
                                                missingPlugins.add(plugin.getName());

                                            }
                                        }
                                    }
                                    runOnUiThread(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    final String names = TextUtils.join(", ", missingPlugins);
                                                    final Context context = getApplicationContext();
                                                    CharSequence text = "You need to install :" + names;
                                                    int duration = Toast.LENGTH_LONG;
                                                    Toast toast = Toast.makeText(context, text, duration);
                                                    toast.show();
                                                }
                                            }
                                    );
                                } else {
                                    try {
                                        Log.i(TAG, "Will install " + experiment);
                                        if (DynamixService.getExperiment() != null
                                                && experiment.getId().equals(DynamixService.getExperiment().getId())
                                                && DynamixService.isExperimentInstalled(experiment.getContextType())) {
                                            //clicked the same experiment
                                        } else {
                                            mMixpanel.timeEvent("install-experiment");


                                            Log.i(TAG, "Starting Experiment " + experiment.getId());
                                            installables.put(experiment, 1);
                                            newExperimentsAdapter.notifyDataSetChanged();

                                            final String url = experiment.getUrl();
                                            final Downloader downloader = new Downloader();
                                            try {
                                                Log.i(TAG, "Downloading Experiment NOW");
                                                downloader.DownloadFromUrl(url, experiment.getFilename());

                                                if (!DynamixService.sessionStarted) {
                                                    DynamixServiceListenerUtility.start();

                                                    for (int i = 0; i < 10; i++) {
                                                        incStatus(experiment, 1000, 2);
                                                    }
                                                } else {
                                                    incStatus(experiment, 0, 20);
                                                }

                                                DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback,
                                                        Constants.EXPERIMENT_PLUGIN_CONTEXT_TYPE,
                                                        Constants.EXPERIMENT_PLUGIN_CONTEXT_TYPE,
                                                        DynamixService.getReadingStorage().getBundle());
                                                DynamixService.removeExperiment();
                                                DynamixService.setExperiment(experiment);
                                                incStatus(experiment, 0, 5);
                                                newExperimentsAdapter.notifyDataSetChanged();
                                                incStatus(experiment, 5000, 5);
                                                DynamixService.startExperiment();
                                                DynamixService.stopFramework();
                                                DynamixService.setRestarting(true);
                                                DynamixService.setTitleBarRestarting(true);
                                                incStatus(experiment, 5000, 10);
                                                DynamixService.startFramework();
                                                incStatus(experiment, 0, 10);
                                                newExperimentsAdapter.notifyDataSetChanged();
                                                incStatus(experiment, 7000, 10);
                                                DynamixServiceListenerUtility.start();
                                                DynamixService.setRestarting(false);
                                                DynamixService.setTitleBarRestarting(false);
                                                incStatus(experiment, 0, 10);
                                                newExperimentsAdapter.notifyDataSetChanged();
                                                try {
                                                    final JSONObject props = new JSONObject();
                                                    props.put("install", experiment.getId());
                                                    mMixpanel.track("install-experiment", props);
                                                } catch (JSONException ignore) {
                                                }
                                                runOnUiThread(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                adapter.notifyDataSetChanged();
                                                            }
                                                        }
                                                );
                                                experiments.add(experiment);
                                                newExperimentsAdapter.remove(experiment);
                                                newExperimentsAdapter.notifyDataSetChanged();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                if (!DynamixService.isNetworkAvailable()) {
                                                    Toast.makeText(DynamixService.getAndroidContext(),
                                                            "Please Check Internet Connection!",
                                                            Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(DynamixService.getAndroidContext(),
                                                            "Please Check Internet Connection!",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                                throw new Exception("Failed to Download Experiment");
                                            } finally {
                                                installables.remove(experiment);
                                                newExperimentsAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    } catch (Exception e) {
                                        //
                                    }
                                }
                                return null;
                            }

                            private void incStatus(Experiment experiment, int time, int val) {
                                if (time > 0) {
                                    try {
                                        Thread.sleep(time);
                                    } catch (InterruptedException e) {
                                    }
                                }
                                installables.put(experiment, installables.get(experiment) + val);
                                newExperimentsAdapter.notifyDataSetChanged();
                            }

                            @Override
                            protected void onPostExecute(final List<Experiment> newExperiments) {
                                //
                            }
                        }.execute();


                    }
                }
        );
        final Button updateExperimentsButton = (Button) findViewById(R.id.btn_update_experiments);
        updateExperimentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.i(TAG, runnableUpdate.getStatus().name());
                if (runnableUpdate.getStatus().equals(AsyncTask.Status.FINISHED)) {
                    new AsyncTask<Void, String, List<Experiment>>() {
                        @Override
                        protected List<Experiment> doInBackground(Void... params) {
                            final Communication communication = new Communication();
                            try {
                                return communication.getExperiments();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(final List<Experiment> newExperiments) {
                            newExperimentsAdapter.clear();
                            for (final Experiment experiment : newExperiments) {
                                if (DynamixService.getExperiment() != null
                                        && experiment.getId().equals(DynamixService.getExperiment().getId())
                                        && DynamixService.isExperimentInstalled(experiment.getContextType())) {
                                    //ignore this experiment
                                } else {
                                    newExperimentsAdapter.add(experiment);
                                }

                            }
                            newExperimentsAdapter.notifyDataSetChanged();
                            adapter.notifyDataSetChanged();
                        }
                    }.execute();
                }
            }
        });
    }


    /**
     * Local class used as a data-source for ContextPlugins. This class extends
     * a typed Generic ArrayAdapter and overrides getView in order to update the
     * UI state.
     *
     * @author Darren Carlson
     */
    private class InstalledExperimentAdapter extends
            EmptyListSupportAdapter<Experiment> {
        ImageLoader imageLoader = App.getInstance().getImageLoader();

        public InstalledExperimentAdapter(Context context,
                                          int textViewResourceId, List<Experiment> experiments,
                                          String emptyTitle, String emptyMessage) {
            super(context, textViewResourceId, experiments, emptyTitle, emptyMessage);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (super.isListEmpty()) {
                final LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View v = vi.inflate(R.layout.iconless_row, null);
                final TextView tt = (TextView) v.findViewById(R.id.toptext);
                final TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                tt.setText(getEmptyTitle());
                bt.setText(getEmptyMessage());
                return v;
            } else {
                final LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View v = vi.inflate(R.layout.experiment_icon_row, null);
                Experiment experiment;
                try {
                    experiment = this.getItem(position);// tODO:ArrayIndexOutOfBoundsException
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return convertView;
                }
                if (experiment != null) {
                    final TextView titleTextView = (TextView) v.findViewById(R.id.ex1_title);
                    final TextView pluginsTextView = (TextView) v.findViewById(R.id.ex1_plugins);
                    final TextView startDateTextView = (TextView) v.findViewById(R.id.ex1_start_date);
                    if (titleTextView != null) {
                        titleTextView.setText(experiment.getName());
                    }
                    if (startDateTextView != null) {
                        // ex_start_date
                        final String formattedDate = "Added: " + sdf.format(new Date(experiment.getTimestamp()));
                        startDateTextView.setText(formattedDate);
                    }
                    if (pluginsTextView != null) {
                        // ex_plugins
                        final StringBuilder genreStr = new StringBuilder("Sensors: ");
                        final List<String> pluginNames = new ArrayList<>();
                        for (final String contextType : experiment.getSensorDependencies().split(",")) {
                            final Plugin plugin = DynamixService.getDiscoveredPluginByContextType(contextType);
                            if (plugin != null) {
                                pluginNames.add(plugin.getName());
                            }
                        }
                        genreStr.append(android.text.TextUtils.join(", ", pluginNames));
                        pluginsTextView.setText(genreStr.toString());
                    }
                    final NetworkImageView icon = (NetworkImageView) v.findViewById(R.id.icon);
                    if (icon != null) {
                        icon.setImageUrl("http://images.sensorflare.com/resources/Location.png", imageLoader);
                    }
                } else {
                    Log.e(TAG, "Could not get ContextPlugin for position: " + position);
                }
                return v;
            }
        }
    }
}
