package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;

public class securityTab extends Activity{

	private boolean tabActive = false;
 
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.security);    
        LinearLayout list= (LinearLayout) findViewById(R.id.checklist);        
        String plistString=(getApplicationContext().getSharedPreferences("pluginObjects", 0)).getString("pluginObjects", "");
        if(plistString.equals("")) return;
        PluginList pList=(new Gson()).fromJson(plistString,  PluginList.class);  
        if (DynamixService.getAllContextPluginInfo()!=null){
        for(ContextPluginInformation plugin : DynamixService.getAllContextPluginInfo()){
        	CheckBox option = (CheckBox) new CheckBox(this.getBaseContext());
        	option.setText(plugin.getPluginName());
        	option.setEnabled(false);        	
        	if (plugin.getInstallStatus()== PluginInstallStatus.INSTALLED){
        		option.setSelected(true);
        	}else{
        		option.setSelected(false);
        	}    
            list.addView(option);            	
        }
        }             
        tabActive = true;
    }
    
    
    public boolean isTabActive()
    {
    	return this.tabActive;
    }
    
 
	
	@Override
	public void onResume(){
		super.onResume();
		LinearLayout list= (LinearLayout) findViewById(R.id.checklist);
		list.removeAllViews();
		for(ContextPluginInformation plugin : DynamixService.getAllContextPluginInfo()){
			TextView option = (TextView) new TextView(this.getBaseContext());        	
        	option.setEnabled(false);        	
        	if (plugin.getInstallStatus()== PluginInstallStatus.INSTALLED){
        		option.setText(plugin.getPluginName() +":INSTALLED");
        	}else{
        		option.setText(plugin.getPluginName() +":Disabled");
        	}
        	
            list.addView(option);            	
        }
		
	}
}
