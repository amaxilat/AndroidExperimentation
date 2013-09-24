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

import com.google.gson.Gson;

import org.ambientdynamix.core.R;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;

public class securityTab extends Activity{

	private boolean tabActive = false;
 	private SharedPreferences pref;
	private Editor editor;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.security);    
        LinearLayout list= (LinearLayout) findViewById(R.id.checklist);        
        String plistString=(getApplicationContext().getSharedPreferences("pluginObjects", 0)).getString("pluginObjects", "");
        if(plistString.equals("")) return;
        PluginList pList=(new Gson()).fromJson(plistString,  PluginList.class);              
        for(Plugin plugin : pList.getPluginList()){
        	CheckBox option = (CheckBox) new CheckBox(this.getBaseContext());
        	option.setText(plugin.getName());
        	option.setId(plugin.getId());
        	option.setOnClickListener(new OnClickListener()
            {
            	@Override
            	public void onClick(View v)
            	{
            		String value=((CheckBox) v).getText().toString();
            		if (((CheckBox) v).isChecked())
            		{
            			editor.putBoolean(value,true);
            		}
            		else
            		{
            			editor.putBoolean(value, false);
            		}
            		
            		editor.commit();
            		sendPermissionsChangedIntent();
            	}
        	});
            list.addView(option);
            	
        }
        pref = getApplicationContext().getSharedPreferences("sensors", 0); // 0 - for private mode
        editor = pref.edit();                     
        setRules();                
        tabActive = true;
    }
    
    private void setRules()
    {    	
    	editor.commit();
    	String plistString=(getApplicationContext().getSharedPreferences("pluginObjects", 0)).getString("pluginObjects", "");
        PluginList pList=(new Gson()).fromJson(plistString,  PluginList.class);
        
        if( !(pref.contains("firstTime")) )
        {
        	editor.putBoolean("firstTime", false);       
            for(Plugin plugin : pList.getPluginList()){
        		editor.putBoolean(plugin.getName(), false);
        	}
        	editor.commit();
        }
        for(Plugin plugin : pList.getPluginList()){        
        	Boolean enabled = pref.getBoolean(plugin.getName(), false);
        	CheckBox option = (CheckBox) findViewById(plugin.getId());
        	option.setChecked(enabled);
        }
     
    } 
         
    
    public boolean isTabActive()
    {
    	return this.tabActive;
    }
    
	// send intent to MainActivity to tell to scheduler to refresh sensor permissions
	private void sendPermissionsChangedIntent()
	{
	    Intent i = new Intent();
	    i.setAction("sensors_permissions_changed");
	    sendBroadcast(i);
	} 
    
}
