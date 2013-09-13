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
package org.ambientdynamix.contextplugins.batterylevel;

import java.util.UUID;

import org.ambientdynamix.api.application.ErrorCodes;
import org.ambientdynamix.api.contextplugin.AutoReactiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

/**
 * Example auto-reactive plug-in that detects the device's battery level.
 * 
 * @author Darren Carlson
 * 
 */
public class BatteryLevelPluginRuntime extends AutoReactiveContextPluginRuntime {
	private static final int VALID_CONTEXT_DURATION = 60000;
	// Static logging TAG
	private final String TAG = this.getClass().getSimpleName();
	// Our secure context
	private Context context;
	// A BroadcastReceiver variable that is used to receive battery status updates from Android
	private BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			sendBroadcastContextEvent(new SecuredContextInfo(new BatteryLevelInfo(intent), PrivacyRiskLevel.LOW),
					VALID_CONTEXT_DURATION);
		}
	};

	/**
	 * Called once when the ContextPluginRuntime is first initialized. The implementing subclass should acquire the
	 * resources necessary to run. If initialization is unsuccessful, the plug-ins should throw an exception and release
	 * any acquired resources.
	 */
	@Override
	public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
		// Set the power scheme
		this.setPowerScheme(powerScheme);
		// Store our secure context
		this.context = this.getSecuredContext();
	}

	/**
	 * Called by the Dynamix Context Manager to start (or prepare to start) context sensing or acting operations.
	 */
	@Override
	public void start() {
		// Register for battery level changed notifications
		context.registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		Log.d(TAG, "Started!");
	}

	/**
	 * Called by the Dynamix Context Manager to stop context sensing or acting operations; however, any acquired
	 * resources should be maintained, since start may be called again.
	 */
	@Override
	public void stop() {
		// Unregister battery level changed notifications
		context.unregisterReceiver(batteryLevelReceiver);
		Log.d(TAG, "Stopped!");
	}

	/**
	 * Stops the runtime (if necessary) and then releases all acquired resources in preparation for garbage collection.
	 * Once this method has been called, it may not be re-started and will be reclaimed by garbage collection sometime
	 * in the indefinite future.
	 */
	@Override
	public void destroy() {
		this.stop();
		context = null;
		Log.d(TAG, "Destroyed!");
	}

	@Override
	public void handleContextRequest(UUID requestId, String contextType) {
		// Check for proper context type
		if (contextType.equalsIgnoreCase(BatteryLevelInfo.CONTEXT_TYPE)) {
			// Manually access the battery level with a null BroadcastReceiver
			Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			// Send the context event
			sendContextEvent(requestId, new SecuredContextInfo(new BatteryLevelInfo(batteryIntent),
					PrivacyRiskLevel.LOW), VALID_CONTEXT_DURATION);
		} else {
			sendContextRequestError(requestId, "NO_CONTEXT_SUPPORT for " + contextType, ErrorCodes.NO_CONTEXT_SUPPORT);
		}
	}

	@Override
	public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config) {
		// Warn that we don't handle configured requests
		Log.w(TAG, "handleConfiguredContextRequest called, but we don't support configuration!");
		// Drop the config and default to handleContextRequest
		handleContextRequest(requestId, contextType);
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
}