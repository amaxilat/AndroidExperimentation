package org.ambientdynamix.contextplugins.NoiseLevelPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import eu.smartsantander.androidExperimentation.jsonEntities.Reading;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class NoiseLevelPluginRuntime extends AutoReactiveContextPluginRuntime {
	
    private final String TAG = this.getClass().getSimpleName();
	private Context context;

	private String status;
	 static final private double EMA_FILTER = 0.6;
     private MediaRecorder mRecorder = null;
     private double mEMA = 0.0;    
    private Handler handler;
	private String reading = "unknown";
    
    
    private long SENSOR_POLL_INTERVAL=5000;
    
    private Runnable runnable = new Runnable() {
		@Override
		public void run() {	
			  broadcastNoiseLevel(null);
		      handler.postDelayed(this, SENSOR_POLL_INTERVAL);
		}
	};
    
    
	public void broadcastNoiseLevel(UUID requestId) {
		if (requestId!=null)
			Log.w(TAG, "NoiseLevel Broadcast:"+requestId);
		else
			Log.w(TAG, "NoiseLevel Broadcast Timer!");
		try {
		    mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	        mRecorder.setOutputFile("/dev/null"); 
	        mRecorder.prepare();
	        mRecorder.start();
	        double ma=mRecorder.getMaxAmplitude();
	        Thread.sleep(250);
	        ma=mRecorder.getMaxAmplitude();	        
	        Thread.sleep(250);
	        ma+=mRecorder.getMaxAmplitude();	        
	        Thread.sleep(250);
	        ma+=mRecorder.getMaxAmplitude();
	        ma=ma/4;
	        double value=(ma/2700.0);
	        Log.w(TAG,"NoiseLevel Max Anplitute:"+ ma);
	        mEMA = EMA_FILTER * value + (1.0 - EMA_FILTER) * mEMA;
	        this.reading=String.valueOf(mEMA);
	        mRecorder.stop();
	        mRecorder.release();
		} catch (Exception e) {
			Log.w("NoiseLevel Plugin Error", e.toString());
			this.reading = "EXCEPTION";
			this.status="invalid";
		}
	        
		Log.w(TAG,"NoiseLevel Plugin:"+ this.reading);
		PluginInfo info = new PluginInfo();
		info.setState(this.status);
		List<Reading> r=new ArrayList<Reading>();
		r.add(new Reading(Reading.Datatype.String, this.reading, PluginInfo.CONTEXT_TYPE));
		info.setPayload(r);		
		Log.w(TAG, "NoiseLevel Plugin:"+ info.getPayload());
		if (requestId!=null){
			sendContextEvent(requestId, new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
			Log.w(TAG,"NoiseLevel Plugin from Request:"+ info.getPayload());
		}else{ 
			sendBroadcastContextEvent(new SecuredContextInfo(info,	PrivacyRiskLevel.LOW), 60000);
			Log.w(TAG,"NoiseLevel Plugin Broadcast:"+ info.getPayload());
		}
	}
		
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		this.setPowerScheme(powerScheme);
		this.context = this.getSecuredContext();
		reading = "";
		handler = new Handler();
		Log.w(TAG, "NoiseLevel Inited!");
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType)
	{	
		broadcastNoiseLevel(requestId);
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config){
		handleContextRequest(requestId,contextType);
	}	
	
	@Override
	public void start()
	{
			Log.d(TAG, "NoiseLevel Plugin Started!");			 
	}
	
	@Override
	public void stop()
	{
		Log.d(TAG, "NoiseLevel Plugin Stopped!");	
	}

	@Override
	public void destroy() {
 
		this.stop();
		Log.d(TAG, "NoiseLevel Plugin Destroyed!");	
	}

	@Override
	public void updateSettings(ContextPluginSettings settings) {}

	@Override
	public void setPowerScheme(PowerScheme scheme) {}

	@Override
	public void doManualContextScan() {}

	 
	
	
 
	
 
	
	 
}