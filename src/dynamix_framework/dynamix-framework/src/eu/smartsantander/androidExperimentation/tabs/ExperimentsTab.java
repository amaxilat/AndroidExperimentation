package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.operations.Communication;
import org.ambientdynamix.core.R;

import java.util.ArrayList;
import java.util.List;


public class ExperimentsTab extends Activity {

    private static final String TAG = "EXPERIMENTS_TAB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.experiments);

        Communication communication = new Communication();
        try {
            final List<Experiment> experiments = communication.getExperiments();
            ExperimentsListAdapter experimentsListAdapter = new ExperimentsListAdapter(this, 0, (ArrayList<Experiment>) experiments);
            ListView listView = (ListView) findViewById(R.id.experiments_list_view);
            listView.setAdapter(experimentsListAdapter);
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(ExperimentsTab.this, "Will Launch " + experiments.get(position).getName() + " experiment.", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

        } catch (Exception e) {
        }
    }
}
