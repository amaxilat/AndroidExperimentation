package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.ambientdynamix.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.smartsantander.androidExperimentation.App;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.operations.Communication;
import eu.smartsantander.androidExperimentation.operations.Downloader;
import eu.smartsantander.androidExperimentation.operations.DynamixServiceListenerUtility;
import eu.smartsantander.androidExperimentation.util.Constants;

/**
 * This tab displays ....
 */

public class NewExperimentTab extends Activity {
    private final static String TAG = "NewExperimentTab";

    private AsyncTask<Void, String, List<Experiment>> runnableUpdate;
    private ExperimentSelectAdapter experimentSelectAdapter;
    private List<Experiment> experiments;
    private Integer installingNow;
    private SimpleDateFormat sdf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_experiment_tab);

        sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        experiments = new ArrayList<>();

        experimentSelectAdapter = new ExperimentSelectAdapter(this);
        final ListView experimentListView = (ListView) findViewById(R.id.experiments_list);
        experimentListView.setAdapter(experimentSelectAdapter);

        runnableUpdate = new AsyncTask<Void, String, List<Experiment>>() {
            @Override
            protected List<Experiment> doInBackground(Void... params) {
                final Communication communication = new Communication();
                try {
                    return communication.getExperiments();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(final List<Experiment> newExperiments) {
                if (experiments != null) {
                    experiments.clear();
                    experiments.addAll(newExperiments);
                    experimentSelectAdapter.notifyDataSetChanged();
                    for (final Experiment experiment : newExperiments) {
                        Log.i(TAG, experiment.getName());
                    }
                }
            }
        };

        runnableUpdate.execute();

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
                                Log.e(TAG, e.getMessage(), e);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(final List<Experiment> newExperiments) {
                            if (experiments != null) {
                                experiments.clear();
                                experiments.addAll(newExperiments);
                                experimentSelectAdapter.notifyDataSetChanged();
                                for (final Experiment experiment : newExperiments) {
                                    Log.i(TAG, experiment.getName());
                                }
                            }
                        }
                    }.execute();
                }
            }
        });
    }


    class ExperimentSelectAdapter extends BaseAdapter {
        final private Activity activity;

        public ExperimentSelectAdapter(final Activity activity) {
            this.activity = activity;
        }

        @Override
        public int getCount() {
            if (experiments != null) {
                return experiments.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            return experiments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return experiments.get(position).getId();
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {

            final View finalConvertView;
            if (convertView == null) {
                final LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                finalConvertView = inflater.inflate(R.layout.experiments_list_row, null);
            } else {
                finalConvertView = convertView;
            }
            final ImageLoader imageLoader = App.getInstance().getImageLoader();

            //get elements
            final NetworkImageView thumbNail = (NetworkImageView) finalConvertView.findViewById(R.id.thumbnail);
            final TextView titleTextView = (TextView) finalConvertView.findViewById(R.id.ex_title);
            final TextView statusTextView = (TextView) finalConvertView.findViewById(R.id.ex_status);
            final TextView pluginsTextView = (TextView) finalConvertView.findViewById(R.id.ex_plugins);
            final TextView startDateTextView = (TextView) finalConvertView.findViewById(R.id.ex_start_date);

            // getting movie data for the row
            final Experiment experiment = experiments.get(position);
            if (experiment != null) {
                // ex_image
                thumbNail.setImageUrl("http://images.sensorflare.com/resources/Location.png", imageLoader);

                // ex_title
                titleTextView.setText(experiment.getName());

                // ex_status
                if (installingNow != null && installingNow.equals(experiment.getId())) {
                    final String statusString = "Status: Installing...";
                    statusTextView.setText(statusString);
                } else if (DynamixService.getExperiment() != null
                        && experiment.getId().equals(DynamixService.getExperiment().getId())
                        && DynamixService.isExperimentInstalled(experiment.getContextType())) {
                    //clicked the same experiment
                    final String statusString = "Status: Running";
                    statusTextView.setText(statusString);
                } else {
                    final String statusString = "Status: " + ("1".equals(experiment.getStatus()) ? "Available" : "Disabled");
                    statusTextView.setText(statusString);
                }


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

                // ex_start_date
                final String formattedDate = "Added: " + sdf.format(new Date(experiment.getTimestamp()));
                startDateTextView.setText(formattedDate);
                finalConvertView.setOnClickListener(

                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new AsyncTask<Void, String, List<Experiment>>() {


                                    @Override
                                    protected List<Experiment> doInBackground(Void... params) {
                                        final String[] smarDeps = DynamixService.getPhoneProfiler().getSensorRules().split(",");
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
                                            activity.runOnUiThread(
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
                                                    Log.i(TAG, "Starting Experiment " + experiment.getId());
                                                    installingNow = experiment.getId();
                                                    activity.runOnUiThread(
                                                            new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    experimentSelectAdapter.notifyDataSetChanged();
                                                                }
                                                            }
                                                    );

                                                    final String url = experiment.getUrl();
                                                    final Downloader downloader = new Downloader();
                                                    try {
                                                        Log.i(TAG, "Downloading Experiment NOW");
                                                        downloader.DownloadFromUrl(url, experiment.getFilename());

                                                        if (!DynamixService.sessionStarted) {
                                                            DynamixServiceListenerUtility.start();
                                                            Thread.sleep(10000);
                                                        }

                                                        DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback,
                                                                Constants.EXPERIMENT_PLUGIN_CONTEXT_TYPE,
                                                                Constants.EXPERIMENT_PLUGIN_CONTEXT_TYPE,
                                                                DynamixService.getReadingStorage().getBundle());
                                                        DynamixService.removeExperiment();
                                                        DynamixService.setExperiment(experiment);
                                                        Thread.sleep(5000);
                                                        DynamixService.startExperiment();
                                                        DynamixService.stopFramework();
                                                        DynamixService.setRestarting(true);
                                                        DynamixService.setTitleBarRestarting(true);
                                                        Thread.sleep(5000);
                                                        DynamixService.startFramework();
                                                        Thread.sleep(7000);
                                                        DynamixServiceListenerUtility.start();
                                                        DynamixService.setRestarting(false);
                                                        DynamixService.setTitleBarRestarting(false);
                                                        installingNow = null;
                                                        activity.runOnUiThread(
                                                                new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        experimentSelectAdapter.notifyDataSetChanged();
                                                                    }
                                                                }
                                                        );
                                                    } catch (Exception e) {
                                                        Log.e(TAG, e.getMessage(), e);
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
                                                    }
                                                }
                                            } catch (Exception e) {
                                                //
                                            }
                                        }
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(final List<Experiment> newExperiments) {
                                        //
                                    }
                                }.execute();


                            }
                        }
                );
                return finalConvertView;
            }
            return null;
        }


    }

}
