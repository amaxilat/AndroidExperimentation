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

import org.ambientdynamix.core.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.smartsantander.androidExperimentation.App;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.operations.Communication;

/**
 * This tab displays ....
 */

public class NewExperimentTab extends Activity {
    private final static String TAG = "NewExperimentTab";

    private Button updateExperiments;
    private ListView list;
    private AsyncTask<Void, String, List<Experiment>> runnableUpdate;
    private ExperimentSelectAdapter adapter;
    private List<Experiment> experiments;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_experiment_tab);
        experiments = new ArrayList<>();
        updateExperiments = (Button) findViewById(R.id.btn_update_experiments);
        list = (ListView) findViewById(R.id.experiments_list);
        adapter = new ExperimentSelectAdapter(this, experiments);
        list.setAdapter(adapter);
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
                    adapter.notifyDataSetChanged();
                    for (final Experiment experiment : newExperiments) {
                        Log.i(TAG, experiment.getName());
                    }
                }
            }
        };

        runnableUpdate.execute();

        updateExperiments.setOnClickListener(new View.OnClickListener() {
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
                                adapter.notifyDataSetChanged();
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
        private Activity activity;
        private LayoutInflater inflater;
        private List<Experiment> experiments;
        ImageLoader imageLoader = App.getInstance().getImageLoader();

        public ExperimentSelectAdapter(Activity activity, List<Experiment> experiments) {
            this.activity = activity;
            this.experiments = experiments;
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
        public View getView(int position, View convertView, ViewGroup parent) {
            if (inflater == null)
                inflater = (LayoutInflater) activity
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (convertView == null)
                convertView = inflater.inflate(R.layout.experiments_list_row, null);

            if (imageLoader == null)
                imageLoader = App.getInstance().getImageLoader();
            NetworkImageView thumbNail = (NetworkImageView) convertView
                    .findViewById(R.id.thumbnail);

            TextView title = (TextView) convertView.findViewById(R.id.title);
            TextView rating = (TextView) convertView.findViewById(R.id.rating);
            TextView genre = (TextView) convertView.findViewById(R.id.genre);
            TextView year = (TextView) convertView.findViewById(R.id.releaseYear);

            // getting movie data for the row
            Experiment e = experiments.get(position);

            // thumbnail image
            thumbNail.setImageUrl("http://images.sensorflare.com/resources/Location.png", imageLoader);

            // title
            title.setText(e.getName());

            // rating
            rating.setText("Status: " + String.valueOf(e.getStatus()));

            // genre
            String genreStr = "";
            for (String str : e.getSensorDependencies().split(",")) {
                String[] parts = str.split("\\.");
                genreStr += parts[parts.length - 1] + ", ";
            }
            genreStr = genreStr.length() > 0 ? genreStr.substring(0,
                    genreStr.length() - 2) : genreStr;
            genre.setText(genreStr);

            // release year
            year.setText(new Date(e.getTimestamp()).toString());

            return convertView;
        }


    }

}
