package org.ambientdynamix.contextplugins.ExperimentPlugin;

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
	private Reading r;
	private Bundle b;
 
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		b=new Bundle();
		Log.w(TAG, "Experiment Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType)
	{	
		Log.w(TAG, "Experiment Workload Started!");
		try {
			
			String jsonReading= b.getString("org.ambientdynamix.contextplugins.GpsPlugin");
			this.r= Reading.fromJson(jsonReading);
			PluginInfo info = new PluginInfo();
			info.setState("ACTIVE");			
			if (this.r!=null){
				Log.w("Experiment Message:", r.toJson());
				info.setPayload(new Reading(Reading.Datatype.String, this.r.toJson()));		
				sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
			}else{
				info.setPayload(new Reading(Reading.Datatype.String, ""));		
				sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
			}
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
		Log.d(TAG, "Experiment Destroyed!");
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