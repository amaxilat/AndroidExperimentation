package eu.smartsantander.androidExperimentation.tabs;

import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.ambientdynamix.data.ExperimentAdapter;
import org.ambientdynamix.data.InstalledExperimentAdapter;
import org.ambientdynamix.util.SeparatedListAdapter;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private MixpanelAPI mMixpanel;
    private InstalledExperimentAdapter installedAdapter;
    private ExperimentAdapter newExperimentsAdapter;
    private Map<Experiment, Integer> installables = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_experiment_tab);


        createElements();
        mMixpanel = MixpanelAPI.getInstance(this, Constants.MIXPANEL_TOKEN);
        mMixpanel.identify(String.valueOf(DynamixService.getPhoneProfiler().getPhoneId()));
    }

    private void createElements() {
        plugList = getListView();
        plugList.setClickable(true);

        // create our list and custom adapter
        adapter = new SeparatedListAdapter(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Log.i(TAG, " Inflater:" + inflater);

        installedAdapter = new InstalledExperimentAdapter(this,
                R.layout.list_header, new ArrayList<Experiment>(),
                getString(R.string.no_experiments), "", inflater);
        installedAdapter.setNotifyOnChange(true);

        adapter.addSection(getString(R.string.installed_experiments),
                installedAdapter);

        newExperimentsAdapter = new ExperimentAdapter(this, R.layout.list_header,
                inflater, new ArrayList<Experiment>(), installables, false,
                getString(R.string.no_available_experiments), getString(R.string.tap_find_plugins));
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

        final Button btnFindExps = (Button) findViewById(R.id.btn_update_experiments);
        btnFindExps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
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
                                        if (DynamixService.getExperiment() != null
                                                && experiment.getId().equals(DynamixService.getExperiment().getId())
                                                && DynamixService.isExperimentInstalled(experiment.getContextType())) {
                                            //clicked the same experiment
                                        } else {
                                            mMixpanel.timeEvent("install-experiment");


                                            installables.put(experiment, 1);
                                            newExperimentsAdapter.notifyDataSetChanged();

                                            final String url = experiment.getUrl();
                                            final Downloader downloader = new Downloader();
                                            try {
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
                                                Log.i(TAG, "step1");
                                                newExperimentsAdapter.notifyDataSetChanged();
                                                incStatus(experiment, 5000, 5);
                                                DynamixService.startExperiment();
                                                DynamixService.stopFramework();
                                                DynamixService.setRestarting(true);
                                                DynamixService.setTitleBarRestarting(true);
                                                incStatus(experiment, 5000, 10);
                                                DynamixService.startFramework();
                                                incStatus(experiment, 0, 10);
                                                Log.i(TAG, "step2");
                                                newExperimentsAdapter.notifyDataSetChanged();
                                                incStatus(experiment, 7000, 10);
                                                DynamixServiceListenerUtility.start();
                                                DynamixService.setRestarting(false);
                                                DynamixService.setTitleBarRestarting(false);
                                                incStatus(experiment, 0, 10);
                                                Log.i(TAG, "step3");
                                                newExperimentsAdapter.notifyDataSetChanged();
                                                try {
                                                    final JSONObject props = new JSONObject();
                                                    props.put("install", experiment.getId());
                                                    mMixpanel.track("install-experiment", props);
                                                } catch (JSONException ignore) {
                                                }
                                                Log.i(TAG, "step4");
                                                installedAdapter.add(experiment);
                                                Log.i(TAG, "step5");
//                                                newExperimentsAdapter.remove(experiment);

                                                runOnUiThread(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Log.i(TAG, "step6");
                                                                adapter.notifyDataSetChanged();
                                                                Log.i(TAG, "step7");
                                                                installedAdapter.notifyDataSetChanged();
                                                                Log.i(TAG, "step8");
                                                                newExperimentsAdapter.notifyDataSetChanged();
                                                            }
                                                        }
                                                );
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
    }
}
