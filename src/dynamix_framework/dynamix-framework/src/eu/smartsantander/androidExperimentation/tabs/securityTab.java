package eu.smartsantander.androidExperimentation.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.gson.Gson;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;

/**
 * This tab displays rules about the sensing plugins installed
 *
 */



public class securityTab extends Activity {

	List<HashMap<String,String>> sensorOptionsL=new ArrayList<HashMap<String,String>>();	
	HashMap<String, String> sensorOptions = new HashMap<String, String>();
	SimpleAdapter simpleAdptl;
	
 

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.security);
		
		ListView list1 = (ListView) findViewById(R.id.checklist); 	
		simpleAdptl = new SimpleAdapter(this, sensorOptionsL, android.R.layout.simple_list_item_1, new String[] {"sensor"}, new int[] {android.R.id.text1});
		list1.setAdapter(simpleAdptl);
		
	}

	@Override
	public void onResume() {
		super.onResume();
		sensorOptionsL.clear();
		for (ContextPluginInformation plugin : DynamixService.getAllContextPluginInfo()) {
			sensorOptions=new HashMap<String,String>();
			if (plugin.getInstallStatus() == PluginInstallStatus.INSTALLED) {
				sensorOptions.put("sensor",plugin.getPluginName() + ":INSTALLED");
			} else {
				sensorOptions.put("sensor",plugin.getPluginName() + ":DISABLED");				
				
			}
			sensorOptionsL.add(sensorOptions);	
			sensorOptions= new HashMap<String, String>();				
		}
		
		simpleAdptl.notifyDataSetChanged();
 	}
	
	
	
}
