package eu.smartsantander.androidExperimentation.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;

import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;

public class jobsTab extends Activity {

	SimpleAdapter simpleAdpt2;
	List<HashMap<String,String>> experimentsOptionsL=new ArrayList<HashMap<String,String>>();	
	HashMap<String, String> experimentsOptions = new HashMap<String, String>();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jobs);
        
		ListView list2 = (ListView) findViewById(R.id.experiment_list); 	
		simpleAdpt2 = new SimpleAdapter(this, experimentsOptionsL, android.R.layout.simple_list_item_1, new String[] {"experiment"}, new int[] {android.R.id.text1});
		list2.setAdapter(simpleAdpt2);
    }
    
 
    @Override
	public void onResume() {
		super.onResume();
 		experimentsOptions.clear();experimentsOptionsL.clear();
 		List<Experiment> exps=DynamixService.getPhoneProfiler().getExperiments();
 		if (exps==null) return;
 		for (Experiment e : exps){			
			experimentsOptions.put("experiment", "ID: "+e.getId() + ", Title:"+e.getName());
			experimentsOptionsL.add(experimentsOptions);
			experimentsOptions=new HashMap<String, String>();
		}	
		
		simpleAdpt2.notifyDataSetChanged();
	}
}
