/*
 * Copyright (C) The Ambient Dynamix Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambientdynamix.contextplugins.gps_experiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ambientdynamix.api.application.ErrorCodes;
import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import com.google.gson.*;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


public class gps_experiment extends AutoReactiveContextPluginRuntime {
	// Static logging TAG
	private final String TAG = this.getClass().getSimpleName();
	// Our secure context
	private Context context;
	
	List<String> dependencies = new ArrayList<String>()
	{
		{
			add("org.ambientdynamix.contextplugins.GpsPlugin");
		}
	};	
	
	public static String CONTEXT_TYPE = "org.ambientdynamix.contextplugins.gps_experiment";
	
	private String dependency2 = "org.ambientdynamix.contextplugins.GpsPlugin";
	
	private int samples = 1;
	private int sample_counter = 0;
	
	private String position = "-1";
	private Bundle results;
	
	private String state;
	private boolean running;
	private Handler handler;
	private Runnable runnable = new Runnable()
	{
		@Override
		public void run()
		{
			if(running)
			{	
				doJob();
				handler.postDelayed(this, 10000);
			}
		}
	};
	
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		// Set the power scheme
		this.setPowerScheme(powerScheme);
		// Store our secure context
		this.context = this.getSecuredContext();	
		results = new Bundle();

		position = "-1";		
		state = "not_ready";
		handler = new Handler();
		running = false;
	}

	// handle incoming context request
	@Override
	public void handleContextRequest(UUID requestId, String contextType)
	{		
		// Check for proper context type
		if (contextType.equalsIgnoreCase(CONTEXT_TYPE)==false)
		{
			sendContextScanError(requestId, "NO_CONTEXT_SUPPORT for " + contextType, ErrorCodes.NO_CONTEXT_SUPPORT);
		}
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config)
	{			
		// get command
		String command = (String) config.get("command");
				
		if( command.equals("ping") )
		{
			sendState();
		}
		else if( command.equals("do") )
		{
			startDoJob();
		}		 
		else if( command.equals(dependency2) )
		{
			position = config.getString("data");	
		}
		else if( command.equals("start") )
		{
			start();
		}
		else if( command.equals("stop") )
		{
			stop();
		}
		else
		{
			Log.i(TAG, "command not supported");
		}

	}	
	
	@Override
	public void start()
	{
		Log.d(TAG, "ready!");	
		setState("ready");
	}
	
	@Override
	public void stop()
	{	
		/*
		 * At this point, the plug-in should stop scanning for context and/or handling context requests; however, we
		 * should retain resources needed to run again.
		 */
		
		running = false;
		setState("stopped");
		Log.d(TAG, "Stopped!");
	}

	@Override
	public void destroy() {
		/*
		 * At this point, the plug-in should stop and release any resources. Nothing to do in this case except for stop.
		 */
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
		// Not supported
	}
	
	private void startDoJob()
	{
		running = true;
		setState("running");
		handler.postDelayed(runnable, 10000);
	}
	
	private void doJob()
	{
		Log.i(TAG, "doing happy job");
		
		
		long currentTime = System.currentTimeMillis();			
		results.putString(Long.toString(currentTime), "GPS"+position);   
 
	    
		sample_counter++;
		if(sample_counter >= samples)
		{
			running = false;
			setState("finished");			
		}
	}
		
	private void setState(String state)
	{
		this.state = state;
		sendState();
	}
	
	private void sendState()
	{
		ExperimentPluginInfo info = new ExperimentPluginInfo(this.state);
		
		info.setContextType(this.CONTEXT_TYPE);
		info.setDependencies(this.dependencies);
		info.setData(this.results);	
		this.sendBroadcastContextEvent(new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
	}  
}