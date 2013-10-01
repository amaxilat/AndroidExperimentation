package org.ambientdynamix.contextplugins.GpsPlugin;

import java.util.UUID;

import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class GpsPluginRuntime extends AutoReactiveContextPluginRuntime {
	
    private final String TAG = this.getClass().getSimpleName();
	private Context context;
	private String location = "unknown";
    private LocationManager locationManager;
    private Handler handler;
    private long SENSOR_POLL_INTERVAL=5000;
    
    private Runnable runnable = new Runnable() {
		@Override
		public void run() {	
			  broadcastGPS(null);
		      handler.postDelayed(this, SENSOR_POLL_INTERVAL);
		}
	};
    
    
	public void broadcastGPS(UUID requestId) {
		Log.w(TAG, "GPS Broadcast!");
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
			Log.w("GPS Plugin Error", e.toString());
			this.location = "unknown++";
		}
	        
		Log.w("GPS Plugin:", this.location);
		PluginInfo info = new PluginInfo();
		info.setState("ACTIVE");
		info.setPayload(this.location);		
		if (requestId!=null)
			sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
		else 
			sendBroadcastContextEvent(new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
	}
		
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		location = "unknown_";
		handler = new Handler();
		Log.w(TAG, "GPS Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType)
	{	
		Log.w(TAG, "GPS Broadcast handleContextRequest!");
		broadcastGPS(requestId);
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config){
		handleContextRequest(requestId,contextType);
	}	
	
	@Override
	public void start()
	{
			Log.d(TAG, "GPS Plugin Started!");	
		 
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

	@Override
	public void doManualContextScan() {
		// TODO Auto-generated method stub
		
	}

	 
	
	
 
	
 
	
	 
}