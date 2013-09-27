package org.ambientdynamix.contextplugins.GpsPlugin;

import java.util.UUID;

import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.ReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class GpsPluginRuntime extends ReactiveContextPluginRuntime {
	
    private final String TAG = this.getClass().getSimpleName();
	private Context context;
	private String location = "unknown";
    private LocationManager locationManager;
	
		
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		location = "unknown_";
		Log.d(TAG, "Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType)
	{	
		Log.d(TAG, "handleContextRequest!");
		Location gps;
		try {
			locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
			gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (gps == null)
				gps = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (gps != null)
				this.location = gps.getLatitude() + "," + gps.getLongitude();
			else
				this.location = "unknown+";
		} catch (Exception e) {
			Log.i("GPS Plugin Error", e.toString());
			this.location = "unknown++";
		}
	        
		Log.i("GPS Plugin:", this.location);
		GpsPluginInfo info = new GpsPluginInfo(this.location);
		info.setState("OK");
		sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
		
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config){
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
		Log.d(TAG, "Destroyed!");
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