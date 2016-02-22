package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.smartsantander.androidExperimentation.App;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.operations.Communication;

/**
 * This tab displays ....
 */

public class NewExperimentTab extends Activity {
    private final static String TAG = "NewExperimentTab";

    private AsyncTask<Void, String, List<Experiment>> runnableUpdate;
    private ExperimentSelectAdapter experimentSelectAdapter;
    private List<Experiment> experiments;
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
            final Experiment e = experiments.get(position);
            if (e != null) {
                // ex_image
                thumbNail.setImageUrl("http://images.sensorflare.com/resources/Location.png", imageLoader);

                // ex_title
                titleTextView.setText(e.getName());

                // ex_status
                final String statusString = "Status: " + ("1".equals(e.getStatus()) ? "Running" : "Disabled");
                statusTextView.setText(statusString);

                // ex_plugins
                final StringBuilder genreStr = new StringBuilder("Sensors: ");
                final List<String> pluginNames = new ArrayList<>();
                for (final String contextType : e.getSensorDependencies().split(",")) {
                    final Plugin plugin = DynamixService.getDiscoveredPluginByContextType(contextType);
                    if (plugin != null) {
                        pluginNames.add(plugin.getName());
                    }
                }
                genreStr.append(android.text.TextUtils.join(", ", pluginNames));
                pluginsTextView.setText(genreStr.toString());

                // ex_start_date
                final String formattedDate = "Added: " + sdf.format(new Date(e.getTimestamp()));
                startDateTextView.setText(formattedDate);
                return finalConvertView;
            }
            return null;
        }


    }

}
