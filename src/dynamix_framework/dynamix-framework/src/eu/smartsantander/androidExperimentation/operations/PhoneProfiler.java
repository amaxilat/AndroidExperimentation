package eu.smartsantander.androidExperimentation.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.core.DynamixService;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class PhoneProfiler extends Thread implements Runnable {
	private SharedPreferences pref;
	private Editor editor;	

	private int PHONE_ID;

	private final String TAG = this.getClass().getSimpleName();

	public PhoneProfiler() {
		this.PHONE_ID = 0;
	}

	public void run() {
		try {
			Log.d(TAG, "running");
			Thread.sleep(5000);  
			pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("phoneId",0);
			editor = pref.edit();
			if ((pref.contains("phoneId"))) {
				this.PHONE_ID = pref.getInt("phoneId", 0);
				if (this.PHONE_ID < 1) {
					setPhoneId(Constants.PHONE_ID_UNITIALIZED);
				}
			} else {
				setPhoneId(Constants.PHONE_ID_UNITIALIZED);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void register() {	
		if ( DynamixService.isDeviceRegistered()==true)
			return;
		int phoneId = Constants.PHONE_ID_UNITIALIZED;
		int serverPhoneId;
		
		try {
			serverPhoneId = DynamixService.getCommunication().registerSmartphone(phoneId,getSensorRules());
			DynamixService.getPhoneProfiler().setPhoneId(serverPhoneId);
		} catch (Exception e) {
			DynamixService.getPhoneProfiler().setPhoneId(Constants.PHONE_ID_UNITIALIZED);
		}		
	}
	
	public int getPhoneId() {
		return this.PHONE_ID;
	}

	public void setPhoneId(int PHONE_ID) {
		this.PHONE_ID = PHONE_ID;
		editor.putInt("phoneId", this.PHONE_ID);
		editor.commit();
	}

	public String getSensorRules(){
		String sensorRules="";
		for(ContextPluginInformation plugin :  DynamixService.getAllContextPluginInfo())
		{
			if(plugin.getInstallStatus()==PluginInstallStatus.INSTALLED)
			{
				sensorRules = sensorRules + plugin.getContextPluginType() + ",";
			}
			
		}
		return sensorRules;		
	}

}
