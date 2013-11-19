package eu.smartsantander.androidExperimentation.operations;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.core.DynamixService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class PhoneProfiler extends Thread implements Runnable {
	private SharedPreferences pref;
	private Editor editor;	
	private Boolean started=false;
	private int PHONE_ID=Constants.PHONE_ID_UNITIALIZED;
	private List<Experiment> experiments =new ArrayList<Experiment>();

	private final String TAG = this.getClass().getSimpleName();

	public PhoneProfiler() {
		this.PHONE_ID = Constants.PHONE_ID_UNITIALIZED;
	}

 
	
	public Boolean getStarted(){
		return started;
	}
	public void run() {		
		startJob();
		started=true;
	}

	public void startJob(){
		try {
			Log.d(TAG, "running");
			Thread.sleep(5000);  
			pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations",0);
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
			if(serverPhoneId<=0)
				serverPhoneId=Constants.PHONE_ID_UNITIALIZED;
			else {
				DynamixService.getPhoneProfiler().setPhoneId(serverPhoneId);
				setLastOnlineLogin();
			}
			
			
		} catch (Exception e) {
			DynamixService.getPhoneProfiler().setPhoneId(Constants.PHONE_ID_UNITIALIZED);
		}		
	}
	
	public int getPhoneId() {
		return this.PHONE_ID;
	}

	public void setPhoneId(int PHONE_ID) {
		this.PHONE_ID = PHONE_ID;
		if (editor==null){
			pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations",0);
			editor = pref.edit();	
		}
		
		editor.putInt("phoneId", this.PHONE_ID);
		editor.putString("experiment", "");
		editor.commit();
	}

	
	public String getSensorRules(){
		String sensorRules="";
		for(ContextPluginInformation plugin :  DynamixService.getAllContextPluginInfo())
		{
			if(plugin.getInstallStatus()==PluginInstallStatus.INSTALLED)
			{
				sensorRules = sensorRules + plugin.getPluginId() + ",";
			}
			
		}
		return sensorRules;		
	}

	
	public void experimentPush(Experiment exp){
		if (editor==null){
			pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations",0);
			editor = pref.edit();	
		}
		String experimentsJson=pref.getString("experiments","");
		if (experimentsJson!=null && experimentsJson.length()>0){
			Type listType = new TypeToken<ArrayList<Experiment>>() { }.getType();
			experiments= (new Gson()).fromJson(experimentsJson, listType);
		}
		if (experiments==null){
			experiments=new ArrayList<Experiment>();
		}
		experiments.add(exp);			
		experimentsJson= (new Gson()).toJson(experiments);
		editor.putString("experiments", experimentsJson);
		editor.commit();
	}
	
	public List<Experiment> getExperiments(){
		return experiments;
	}
	
	// keep stats for total time connected to the service
	
	public void setLastOnlineLogin() {
				
		Date dat = new Date();
		
		if (editor==null){
			pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations",0);
			editor = pref.edit();	
		}
		
		editor.putLong("lastOnlineLoginDate", dat.getTime());
		editor.commit();
			
	}
	
	public Date getLastOnlineLogin() {
		
		Date lastLoginDate;
		
		if (editor==null){
			pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations",0);
		}
		
		lastLoginDate = new Date(pref.getLong("lastOnlineLoginDate", 0));
			
		return lastLoginDate;
	}
}
