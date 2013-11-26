package org.ambientdynamix.contextplugins.ExperimentPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.ReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import eu.smartsantander.androidExperimentation.jsonEntities.Reading;

public class ExperimentPluginRuntime extends ReactiveContextPluginRuntime {
	
    private final String TAG = this.getClass().getSimpleName();
	private Context context;
	private Reading gpsReading;
	private Reading noiseReading;
	private Bundle b;
 
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		b=new Bundle();
		Log.w(TAG, "ExperimentPluginNoiseGpsScan Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType)
	{	
		Log.w(TAG, "ExperimentPluginNoiseGpsScan Workload Started!");
		try {
			
			String jsonReading= b.getString("org.ambientdynamix.contextplugins.GpsPlugin");
			String jsonReadingNoiseScan= b.getString("org.ambientdynamix.contextplugins.NoiseLevelPlugin");
			this.gpsReading= Reading.fromJson(jsonReading);
			this.noiseReading= Reading.fromJson(jsonReadingNoiseScan);
			List<Reading> readings=new ArrayList<Reading>();
			PluginInfo info = new PluginInfo();
			info.setState("ACTIVE");	
				
			if (this.gpsReading!=null){
				Log.w("Experiment Message:", gpsReading.toJson());						
				readings.add(new Reading(Reading.Datatype.String, this.gpsReading.toJson(),PluginInfo.CONTEXT_TYPE));
			}else{
				readings.add(new Reading(Reading.Datatype.String, "",PluginInfo.CONTEXT_TYPE));						
			}
			if (this.noiseReading!=null){
				Log.w("Experiment Message:", noiseReading.toJson());						
				readings.add(new Reading(Reading.Datatype.String, this.noiseReading.toJson(),PluginInfo.CONTEXT_TYPE));
			}else{
				readings.add(new Reading(Reading.Datatype.String, "",PluginInfo.CONTEXT_TYPE));						
			}
			
			info.setPayload(readings);
			sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
		} catch (Exception e) {
			Log.w("Experiment Workload Error", e.toString());
		}
	}
	

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config){
		b.clear();
		b.putAll(config);
		handleContextRequest(requestId,contextType);
	}	
	
	@Override
	public void start()
	{		
		
	}
	
	@Override
	public void stop()
	{
 	
	}

	@Override
	public void destroy() {
		/*
		 * At this point, the plug-in should stop and release any resources. Nothing to do in this case except for stop.
		 */
		this.stop();
		Log.d(TAG, "ExperimentPluginNoiseGpsScan Destroyed!");
	}

	@Override
	public void updateSettings(ContextPluginSettings settings) {
		// Not supported
	}

	@Override
	public void setPowerScheme(PowerScheme scheme) {
		// Not supported
	}

	 
	
	
 
	
 
	
	 
}