package eu.smartsantander.androidExperimentation.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.CheckBox;

public class SensorProfiler extends Thread implements Runnable {

	private Handler handler;
	private Communication communication;
	private PhoneProfiler phoneProfiler;
	private Context context;	

	
	// get TAG name for reporting to LogCat
	private final String TAG = this.getClass().getSimpleName();

	private SharedPreferences pref;
	private Editor editor;
	
	//private List<String> sensors;
	private List<String> permissions;
	private Map<String,Boolean> sensorsPermissions=new HashMap<String, Boolean>();
	private Map<String,String> sensorsContextTypes=new HashMap<String, String>();
	private String sensorRules="";
	private PluginList pList;
	private  NetworkStateReceiver mReceiver;
	

	
	public SensorProfiler(Handler handler, Context context, Communication communication, PhoneProfiler phoneProfiler)
	{		
		this.handler = handler;
		this.context = context;
		this.communication = communication;
		this.phoneProfiler = phoneProfiler;
		
		
		String plistString=(this.context.getSharedPreferences("pluginObjects", 0)).getString("pluginObjects", "");
        if(plistString.equals("")) return;
        pList=(new Gson()).fromJson(plistString,  PluginList.class);       
        
        for(Plugin plugin : pList.getPluginList()){
        	// map sensors to contextTypes
    		sensorsContextTypes.put(plugin.getName(), plugin.getContextType());
        }
				
        pref = context.getApplicationContext().getSharedPreferences("sensors", 0); // 0 - for private mode
        editor = pref.edit();		
        mReceiver = new NetworkStateReceiver();        
		//sensors= new ArrayList<String>();
		//sensors = getAvailableSensors(context);
		
		// get sensor permissions
		permissions = new ArrayList<String>();
		getPermissions();
		setPermissions();
	}
	
	public void run()
	{			
		try
		{
			Log.d(TAG, "running");
			Thread.sleep(1000); //This could be something computationally intensive.
			
	        // register network status receiver
	        IntentFilter filter = new IntentFilter();
	        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	        context.registerReceiver(mReceiver, filter);
			
			if( isNetworkAvailable() )
			{
				sendThreadMessage("internet_status:internet_ok");
			}
			else
			{
				sendThreadMessage("internet_status:no_internet");
			}
					
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	// network changed receiver
	public class NetworkStateReceiver extends BroadcastReceiver
	{
	    public void onReceive(Context context, Intent intent)
	    {
	    	networkStatusChanged();
	    }
	}
	    
	// make the sensors permissions available to other modules
	public Map<String, Boolean> getSensorsPermissions()
	{		
		return sensorsPermissions;
	}
	
	public String getSensorRules()
	{
		return sensorRules;
	}
	
	// set the user permissions about the sensors
	private void setPermissions()
	{	
		sensorRules = "";
		sensorsPermissions.clear();
		
		for(Plugin sensor : pList.getPluginList())
		{
			if(permissions.contains(sensor.getName()))
			{
				sensorsPermissions.put( sensorsContextTypes.get(sensor.getName()) , true);
				sensorRules = sensorRules + sensor.getContextType() + ",";
			}
			else
			{
				sensorsPermissions.put( sensorsContextTypes.get(sensor.getName()) , false);
			}
		}
	}
	
	// get user permissions about the sensors
	private void getPermissions()
	{		
		permissions.clear();	
		editor.commit();    	
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
	        	 if (enabled==true) permissions.add(plugin.getName());
	        }	
	}
	
	// get list of the available sensor types
	/*private List<String> getAvailableSensors(Context context)
	{
		List<String> listSensorType = new ArrayList<String>();

		SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> listSensor = sensorManager.getSensorList(Sensor.TYPE_ALL);
	       
	    for(int i=0; i<listSensor.size(); i++)
	    {
	    	// find sensor type
	    	int type = listSensor.get(i).getType();
	    	String type_s="";
	    		    	
	    	switch(type)
	    	{
	    		case Sensor.TYPE_ACCELEROMETER : { type_s = "accelerometer"; break; }
	 //   		case Sensor.TYPE_TEMPERATURE : { type_s = "batteryTemperature"; break; }
	    		case Sensor.TYPE_MAGNETIC_FIELD : { type_s = "magnetic field"; break; }
	    		case Sensor.TYPE_ORIENTATION : { type_s = "orientation"; break;  }
	    		default : { type_s = "uknown" ; break; }
	    	}

	    	// add it to sensor list
	    	listSensorType.add(type_s);
	    }
	      
	    // always available
	    listSensorType.add("batteryLevel");
	    listSensorType.add("batteryTemperature");
	    listSensorType.add("gpsPosition");
	    listSensorType.add("wifiBSSID");
	    
	    return listSensorType;
	}*/
	
	// checks if there is a network interface - call and a service to make sure it goes to the internet 
	private boolean isNetworkAvailable()
	{
	    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	private void networkStatusChanged()
	{
		if( isNetworkAvailable() )
		{
			sendThreadMessage("internet_status:internet_ok");
		}
		else
		{
			sendThreadMessage("internet_status:no_internet");
		}
	}
	
	public void sensorsPermissionsChanged()
	{
		getPermissions();
		setPermissions();
	}
	
	public void sendThreadMessage(String message)
	{
		Message msg = new Message();
		msg.obj = message;
		handler.sendMessage(msg);
	}
}
