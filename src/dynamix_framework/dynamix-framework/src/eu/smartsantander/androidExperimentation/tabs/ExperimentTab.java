package eu.smartsantander.androidExperimentation.tabs;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;

/**
 * This tab displays ....
 */

public class ExperimentTab extends Activity {

    private TextView expIdTv;
    private TextView expNameTv;
    private TextView expDescriptionTv;
    private TextView experimentPausedTv;
    private TextView experimentSubmitedTv;
    private TextView experimentFromTv;
    private TextView experimentToTv;
    private TextView experimentDependenciesTv;
    private Timer refresher;
    private static ExperimentTab activity;
    private final Handler uiHandler = new Handler();
    private ListView list1;
    private String[] listData = new String[2];
    ArrayAdapter<String> depAdapter;
    private ToggleButton button;

    public static void refreshData() {
        if (activity != null)
            activity.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.setSmartSantanderInfo();
                }
            });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.experiment_tab);
        activity = this;

        expIdTv = (TextView) this.findViewById(R.id.experiment_id_JobTab);
        expNameTv = (TextView) this.findViewById(R.id.experiment_name_JobTab);
        expDescriptionTv = (TextView) this
                .findViewById(R.id.experiment_description_JobTab);
        experimentPausedTv = (TextView) this
                .findViewById(R.id.experiment_status_JobTab);
        experimentSubmitedTv = (TextView) this
                .findViewById(R.id.experiment_submitted_JobTab);
        experimentFromTv = (TextView) this
                .findViewById(R.id.experiment_from_JobTab);
        experimentToTv = (TextView) this
                .findViewById(R.id.experiment_to_JobTab);
        experimentDependenciesTv = (TextView) this
                .findViewById(R.id.experiment_dependencies_JobTab);

        list1 = (ListView) findViewById(R.id.dependencies_list);
        listData[0] = "-";
        listData[1] = "-";
        depAdapter = new ArrayAdapter<>(this, R.layout.list_item, listData);

        list1.setAdapter(depAdapter);

        refresher = new Timer(true);
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                refreshData();
            }
        };
        refresher.scheduleAtFixedRate(t, 0, 1000);

        button = (ToggleButton) findViewById(R.id.cachedMessages);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (button.isChecked()) {
                    listData = DynamixService.getCachedExperimentalMessages();
                    depAdapter = new ArrayAdapter<>(v.getContext(), R.layout.list_item, listData);
                    list1.setAdapter(depAdapter);
                    depAdapter.notifyDataSetChanged();
                    experimentDependenciesTv.setText("Last Messages:");
                } else {
                    if (DynamixService.getExperiment() != null) {
                        listData = DynamixService.getExperiment().getSensorDependencies().split(",");
                        int i = 0;
                        for (String dep : listData) {
                            listData[i] = dep.substring(dep.lastIndexOf(".") + 1).replace("Plugin", "-Sensor");
                            i++;
                        }
                        depAdapter = new ArrayAdapter<>(v.getContext(), R.layout.list_item, listData);
                        list1.setAdapter(depAdapter);
                        depAdapter.notifyDataSetChanged();
                        experimentDependenciesTv.setText("Sensor Dependencies:");
                    }
                }

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setSmartSantanderInfo();
    }

    public void setSmartSantanderInfo() {
        // SmartSantander

        if (DynamixService.getExperiment() != null) {
            expIdTv.setText("Id: " + String.valueOf(DynamixService.getExperiment().getId()));
            expNameTv.setText("Name: " + String.valueOf(DynamixService.getExperiment().getName()));
            expDescriptionTv.setText("Description:\n" + String.valueOf(DynamixService.getExperiment().getDescription()));
            if (DynamixService.isEnabled()) {
                experimentPausedTv.setText(R.string.experimentEnabled);
            } else {
                experimentPausedTv.setText(R.string.experimentDisabled);
            }
            Long tstamp = DynamixService.getExperiment().getTimestamp();
            if (tstamp != 0) {
                Date d = new Date(tstamp);
                experimentSubmitedTv
                        .setText("Submitted: " + d.toLocaleString());
                experimentSubmitedTv.setVisibility(View.VISIBLE);
            } else {
                experimentSubmitedTv.setVisibility(View.GONE);
            }
            tstamp = DynamixService.getExperiment().getFromTime();
            if (tstamp != 0) {
                Date d = new Date(tstamp);
                experimentFromTv.setText("Submitted: " + d.toLocaleString());
                experimentFromTv.setVisibility(View.VISIBLE);
            } else {
                experimentFromTv.setVisibility(View.GONE);
            }
            tstamp = DynamixService.getExperiment().getToTime();
            if (tstamp != 0) {
                Date d = new Date(tstamp);
                experimentToTv.setText("Submitted: " + d.toLocaleString());
                experimentToTv.setVisibility(View.VISIBLE);
            } else {
                experimentToTv.setVisibility(View.GONE);
            }


            if (button.isChecked()) {
                listData = DynamixService.getCachedExperimentalMessages();
                depAdapter = new ArrayAdapter<>(this, R.layout.list_item, listData);
                list1.setAdapter(depAdapter);
                depAdapter.notifyDataSetChanged();
                experimentDependenciesTv.setText("Last Messages:");
            } else {
                listData = DynamixService.getExperiment().getSensorDependencies().split(",");
                int i = 0;
                for (String dep : listData) {
                    listData[i] = dep.substring(dep.lastIndexOf(".") + 1).replace("Plugin", "-Sensor");
                    i++;
                }
                depAdapter = new ArrayAdapter<>(this, R.layout.list_item, listData);
                list1.setAdapter(depAdapter);
                depAdapter.notifyDataSetChanged();
                experimentDependenciesTv.setText("Sensor Dependencies:");
            }

            expNameTv.setVisibility(View.VISIBLE);
            expDescriptionTv.setVisibility(View.VISIBLE);
            experimentPausedTv.setVisibility(View.VISIBLE);
            experimentDependenciesTv.setVisibility(View.VISIBLE);
            list1.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);
        } else {
            expIdTv.setText(R.string.no_experiment_is_installed);
            expNameTv.setVisibility(View.GONE);
            expDescriptionTv.setVisibility(View.GONE);
            experimentPausedTv.setVisibility(View.GONE);
            experimentSubmitedTv.setVisibility(View.GONE);
            experimentFromTv.setVisibility(View.GONE);
            experimentToTv.setVisibility(View.GONE);
            experimentDependenciesTv.setVisibility(View.GONE);
            list1.setVisibility(View.GONE);
            listData = new String[]{"-", "-"};
            depAdapter = new ArrayAdapter<>(this, R.layout.list_item, listData);
            list1.setAdapter(depAdapter);
            depAdapter.notifyDataSetChanged();
            button.setVisibility(View.GONE);
        }

    }

}
