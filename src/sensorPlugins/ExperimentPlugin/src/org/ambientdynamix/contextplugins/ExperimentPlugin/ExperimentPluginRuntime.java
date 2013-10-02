package org.ambientdynamix.contextplugins.ExperimentPlugin;

import java.util.List;
import java.util.UUID;

import org.ambientdynamix.api.application.ContextEvent;
import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.ContextSupportInfo;
import org.ambientdynamix.api.application.IContextInfo;
import org.ambientdynamix.api.application.IDynamixFacade;
import org.ambientdynamix.api.application.IDynamixListener;
import org.ambientdynamix.api.application.Result;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.ReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import eu.smartsantander.androidExperimentation.jsonEntities.Reading;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

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
			
			this.r= (Reading) b.getSerializable("org.ambientdynamix.contextplugins.GpsPlugin");
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