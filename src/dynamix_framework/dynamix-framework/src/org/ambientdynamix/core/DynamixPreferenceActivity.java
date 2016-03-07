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
package org.ambientdynamix.core;

import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.data.DynamixPreferences;
import org.ambientdynamix.util.RepositoryInfo;
import org.ambientdynamix.util.Utils;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

/**
 * Provides the user interface for adjusting Dynamix Framework settings. This class interacts with static control
 * methods provided by the DynamixService.
 * 
 * @see DynamixService
 * @author Darren Carlson
 */
public class DynamixPreferenceActivity extends PreferenceActivity {
	/*
	 * Note: This class is based on examples from: - http://www.old.kaloer.com/android-preferences/ -
	 * http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/PreferencesFromCode.html
	 * http://www.javacodegeeks.com/2011/01/android-quick-preferences-tutorial.html
	 * http://mobile.tutsplus.com/tutorials/android/android-application-preferences/
	 */
	// Private data
	private final String TAG = this.getClass().getSimpleName();
	static final String PLUGIN_POWER_SCHEME = "plugin_power_scheme";
	static final String TOGGLE_DEBUG_MODE = "toggle_debug_mode";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(null);
		createPreferenceHierarchy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Returns a PreferenceScreen configured with Dynamix user-based settings
	 */
	private void createPreferenceHierarchy() {
		SharedPreferences existingPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		setPreferenceScreen(root);
		PreferenceCategory basicOptionsCat = new PreferenceCategory(this);
		basicOptionsCat.setTitle("Basic Settings");
		root.addPreference(basicOptionsCat);
		PreferenceCategory updateSettingsCat = new PreferenceCategory(this);
		updateSettingsCat.setTitle("Update Settings");
		root.addPreference(updateSettingsCat);
		CheckBoxPreference toggleAutoInstall = new CheckBoxPreference(this);
		toggleAutoInstall.setKey(DynamixPreferences.AUTO_CONTEXT_PLUGIN_INSTALL);
		toggleAutoInstall.setTitle("Auto Plug-in Install");
		toggleAutoInstall.setSummary("Automatically install Context Plug-in when required by applications");
		toggleAutoInstall.setDefaultValue(true);
		basicOptionsCat.addPreference(toggleAutoInstall);
		CheckBoxPreference toggleAutoStart = new CheckBoxPreference(this);
		toggleAutoStart.setKey(DynamixPreferences.AUTO_START_DYNAMIX);
		toggleAutoStart.setTitle("Auto Boot");
		toggleAutoStart.setSummary("Automatically boot Dynamix when the device starts");
		toggleAutoStart.setDefaultValue(true);
		basicOptionsCat.addPreference(toggleAutoStart);
		
		CheckBoxPreference toggleWifiOnly = new CheckBoxPreference(this);
		toggleWifiOnly.setKey(DynamixPreferences.USE_WIFI_NETWORK_ONLY);
		toggleWifiOnly.setTitle("WIFI Only");
		toggleWifiOnly.setSummary("Only update Dynamix when connected to WIFI");
		toggleWifiOnly.setDefaultValue(true);
		basicOptionsCat.addPreference(toggleWifiOnly);
		
		if(FrameworkConstants.ADMIN_RELEASE){
			CheckBoxPreference toggleCertCollect = new CheckBoxPreference(this);
			toggleCertCollect.setKey(DynamixPreferences.CERT_COLLECT);
			toggleCertCollect.setTitle("Collect Certs");
			toggleCertCollect.setSummary("Auto-authorize Web Client Certs");
			toggleCertCollect.setDefaultValue(false);
			basicOptionsCat.addPreference(toggleCertCollect);
			
			CheckBoxPreference toggleExportKeystore = new CheckBoxPreference(this);
			toggleExportKeystore.setTitle("Export KeyStore");
			toggleExportKeystore.setSummary("Exports Dynamix KeyStore to the SDCARD");
			toggleExportKeystore.setDefaultValue(true);
			toggleExportKeystore.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					try {
						DynamixService.exportKeyStoreToSDCARD();
					} catch (Exception e) {
						Log.w(TAG, e);
					}
					return false;
				}
			});
			basicOptionsCat.addPreference(toggleExportKeystore);
		}
		
		
		CheckBoxPreference toggleWebConnector = new CheckBoxPreference(this);
		toggleWebConnector.setKey(DynamixPreferences.WEB_CONNECTOR);
		toggleWebConnector.setTitle("Web Connector");
		toggleWebConnector.setSummary("Allow Web Applications to use Dynamix");
		toggleWebConnector.setDefaultValue(true);
		toggleWebConnector.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enabled = (Boolean) newValue;
				if (enabled) {
					return DynamixService.startWebConnectorUsingConfigData();
				} else {
					DynamixService.stopWebConnector();
					return true;
				}
			}
		});
		basicOptionsCat.addPreference(toggleWebConnector);
		ListPreference listPref = new ListPreference(this);
		listPref.setPersistent(false);
		listPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PowerScheme newScheme = PowerScheme.getPowerSchemeForID(Integer
						.parseInt((String) newValue));
				preference.setSummary(newScheme.getName());
				DynamixService.setNewPowerScheme(newScheme);
				return true;
			}
		});
		PowerScheme[] schemes = PowerScheme.PowerSchemes;
		String[] schemeNames = new String[schemes.length];
		String[] schemeIDs = new String[schemes.length];
		for (int i = 0; i < schemes.length; i++) {
			schemeNames[i] = schemes[i].toString();
			schemeIDs[i] = Integer.toString(schemes[i].getId());
		}
		listPref.setKey(PLUGIN_POWER_SCHEME);
		listPref.setEntries(schemeNames);
		listPref.setEntryValues(schemeIDs);
		listPref.setDialogTitle("Power Scheme");
		listPref.setTitle("Power Scheme");
		listPref.setSummary(DynamixService.SettingsManager.getPowerScheme().toString());
		listPref.setValue(String.valueOf(DynamixService.SettingsManager.getPowerScheme().getId()));
		basicOptionsCat.addPreference(listPref);
		CheckBoxPreference toggleAppUninstall = new CheckBoxPreference(this);
		toggleAppUninstall.setKey(DynamixPreferences.AUTO_APP_UNINSTALL);
		toggleAppUninstall.setTitle("Auto App Uninstall");
		toggleAppUninstall.setSummary("Remove context firewall settings for apps that are uninstalled by Android.");
		toggleAppUninstall.setDefaultValue(true);
		basicOptionsCat.addPreference(toggleAppUninstall);
		CheckBoxPreference toggleBackgroundMode = new CheckBoxPreference(this);
		toggleBackgroundMode.setKey(DynamixPreferences.BACKGROUND_MODE);
		toggleBackgroundMode.setTitle("Background Mode");
		toggleBackgroundMode.setSummary("Continue to model context, even when the screen is off");
		toggleBackgroundMode.setDefaultValue(true);
		basicOptionsCat.addPreference(toggleBackgroundMode);
		CheckBoxPreference toggleSelfSignedCerts = new CheckBoxPreference(this);
		toggleSelfSignedCerts.setKey(DynamixPreferences.ACCEPT_SELF_SIGNED_CERTS);
		toggleSelfSignedCerts.setTitle("Allow Self-signed Certs");
		toggleSelfSignedCerts.setSummary("Allow Dynamix to trust servers using self-signed identity certificates.");
		toggleSelfSignedCerts.setDefaultValue(DynamixService.getConfig().allowSelfSignedCertsDefault());
		toggleSelfSignedCerts.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enabled = Boolean.getBoolean(newValue.toString());
				DynamixService.getConfig().setAllowSelfSignedCertsDefault(enabled);
				if (enabled)
					Utils.acceptAllSelfSignedSSLcertificates();
				else
					Utils.denyAllSelfSignedSSLcertificates();
				return true;
			}
		});
		basicOptionsCat.addPreference(toggleSelfSignedCerts);
		/*
		 * Handle self-signed cert preference changes
		 */
		toggleSelfSignedCerts.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if ((Boolean) newValue) {
					Utils.acceptAllSelfSignedSSLcertificates();
				} else {
					Utils.denyAllSelfSignedSSLcertificates();
				}
				return true;
			}
		});
		
		CheckBoxPreference toggleSound = new CheckBoxPreference(this);
		toggleSound.setKey(DynamixPreferences.AUDIBLE_ALERTS);
		toggleSound.setTitle("Audible Alerts");
		toggleSound.setSummary("Play an audio notification for new Dynamix messages");
		toggleSound.setDefaultValue(false);
		basicOptionsCat.addPreference(toggleSound);
		CheckBoxPreference toggleVibration = new CheckBoxPreference(this);
		toggleVibration.setKey(DynamixPreferences.VIBRATION_ALERTS);
		toggleVibration.setTitle("Vibration Alerts");
		toggleVibration.setSummary("Vibrate notification for new Dynamix messages");
		toggleVibration.setDefaultValue(false);
		basicOptionsCat.addPreference(toggleVibration);
		CheckBoxPreference toggleAutoContextPlugUpdateCheck = new CheckBoxPreference(this);
		toggleAutoContextPlugUpdateCheck.setKey(DynamixPreferences.AUTO_CONTEXT_PLUGIN_UPDATES);
		toggleAutoContextPlugUpdateCheck.setTitle("Plug-in Update Check");
		toggleAutoContextPlugUpdateCheck
				.setSummary("Automatically check for context plug-ins updates using the Dynamix plug-in repository");
		toggleAutoContextPlugUpdateCheck.setDefaultValue(false);
		toggleAutoContextPlugUpdateCheck.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean val = (Boolean) newValue;
				Log.i(TAG, "toggleAutoContextPlugUpdateCheck: " + val);
				if (val)
					DynamixService.updateContextPluginUpdateTimer(DynamixPreferences
							.getContextPluginUpdateInterval(DynamixPreferenceActivity.this));
				else
					DynamixService.stopContextPluginUpdateTimer();
				return true;
			}
		});
		updateSettingsCat.addPreference(toggleAutoContextPlugUpdateCheck);
		ListPreference cpui = new ListPreference(this);
		cpui.setKey(DynamixPreferences.CONTEXT_PLUGIN_UPDATE_INTERVAL);
		cpui.setTitle("Update Interval");
		cpui.setSummary("Define how often to check for context plug-in updates");
		cpui.setDefaultValue("60000");
		cpui.setEntries(R.array.updateInterval);
		cpui.setEntryValues(R.array.updateIntervalValues);
		updateSettingsCat.addPreference(cpui);
		cpui.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				DynamixService.updateContextPluginUpdateTimer(Integer.parseInt((String) newValue));
				return true;
			}
		});
		cpui.setDependency(DynamixPreferences.AUTO_CONTEXT_PLUGIN_UPDATES);
		CheckBoxPreference toggleDynamixDiscovery = new CheckBoxPreference(this);
		toggleDynamixDiscovery.setKey(DynamixPreferences.DYNAMIX_PLUGIN_DISCOVERY_ENABLED);
		toggleDynamixDiscovery.setTitle("Use Dynamix Repository");
		toggleDynamixDiscovery.setSummary("Enable or disable access to the Dynamix repository");
		toggleDynamixDiscovery.setDefaultValue(true);
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// DYNAMIX REPO ENABLE/DISABLE
		toggleDynamixDiscovery.setEnabled(DynamixService.getConfig().allowPrimaryContextPluginRepoDeactivate());
		// toggleDynamixDiscovery.setEnabled(true);
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		updateSettingsCat.addPreference(toggleDynamixDiscovery);
		final EditTextPreference editNetworkDiscovery = new EditTextPreference(this);
		editNetworkDiscovery.setEnabled(false);
		editNetworkDiscovery.setKey(DynamixPreferences.PRIMARY_CONTEXT_PLUGIN_REPO_PATH);
		editNetworkDiscovery.setTitle("Dynamix Repository Path");
		RepositoryInfo server = DynamixService.getConfig().getPrimaryContextPluginRepo();
		editNetworkDiscovery.setDefaultValue(server.getUrl());
		editNetworkDiscovery.setSummary(existingPrefs.getString(editNetworkDiscovery.getKey(), server.getUrl()));
		editNetworkDiscovery.setDialogTitle("Define Repository Path");
		editNetworkDiscovery.setDialogMessage("Provide the path to the Dynamix plug-in repository");
		editNetworkDiscovery.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				editNetworkDiscovery.setSummary(newValue.toString());
				return true;
			}
		});
		updateSettingsCat.addPreference(editNetworkDiscovery);
		editNetworkDiscovery.setDependency(DynamixPreferences.DYNAMIX_PLUGIN_DISCOVERY_ENABLED);
		/**
		 * TODO: There should be a mechanism whereby multiple external context plugin repos can be defined.
		 */
		CheckBoxPreference toggleExternalDiscovery = new CheckBoxPreference(this);
		toggleExternalDiscovery.setKey(DynamixPreferences.EXTERNAL_PLUGIN_DISCOVERY_ENABLED);
		toggleExternalDiscovery.setTitle("Use External Repository");
		toggleExternalDiscovery.setSummary("Enable or disable access to an external plug-in repository");
		toggleExternalDiscovery.setDefaultValue(false);
		toggleExternalDiscovery.setEnabled(DynamixService.getConfig().allowAdditionalContextPluginRepos());
		updateSettingsCat.addPreference(toggleExternalDiscovery);
		final EditTextPreference editExternalDiscovery = new EditTextPreference(this);
		editExternalDiscovery.setEnabled(DynamixService.getConfig().allowAdditionalContextPluginRepos());
		editExternalDiscovery.setKey(DynamixPreferences.EXTERNAL_CONTEXT_PLUGIN_REPO_PATH);
		editExternalDiscovery.setTitle("External Repository Path");
		editExternalDiscovery.setDialogMessage("Provide a path to a external plug-in descriptor");
		editExternalDiscovery.setDefaultValue("http://");
		editExternalDiscovery.setSummary(existingPrefs.getString(DynamixPreferences.EXTERNAL_CONTEXT_PLUGIN_REPO_PATH,
				"http://"));
		editExternalDiscovery.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				editExternalDiscovery.setSummary(newValue.toString());
				return true;
			}
		});
		updateSettingsCat.addPreference(editExternalDiscovery);
		editExternalDiscovery.setDependency(toggleExternalDiscovery.getKey());
		/*
		 * !!! IMPORTANT !!! If you want to programatically set dependencies, you need to call addPreference before the
		 * 'setDependency' call due to internal Android checking.
		 */
		// editNetworkDiscovery.setDependency(toggleAutoContextPlugUpdateCheck.getKey());
		CheckBoxPreference toggleLocalDiscovery = new CheckBoxPreference(this);
		toggleLocalDiscovery.setKey(DynamixPreferences.LOCAL_CONTEXT_PLUGIN_DISCOVERY);
		toggleLocalDiscovery.setTitle("Use Android Distributed Online Repository");
		toggleLocalDiscovery.setSummary("Enable or disable Android Distributed plug-in discovery");
		toggleLocalDiscovery.setDefaultValue(true);
		toggleLocalDiscovery.setEnabled(DynamixService.getConfig().allowAdditionalContextPluginRepos());
		updateSettingsCat.addPreference(toggleLocalDiscovery);
		final EditTextPreference editLocalDiscovery = new EditTextPreference(this);
		editLocalDiscovery.setKey(DynamixPreferences.LOCAL_CONTEXT_PLUGIN_REPO_PATH);
		editLocalDiscovery.setTitle("Local Android Distributed Repository Path");
		editLocalDiscovery.setDefaultValue(DynamixService.getConfig().getLocalPluginRepo().getUrl());
		if (existingPrefs != null) {
			editLocalDiscovery.setSummary(existingPrefs.getString(editLocalDiscovery.getKey(), DynamixService
					.getConfig().getLocalPluginRepo().getUrl()));
		} else
			editLocalDiscovery.setSummary(DynamixService.getConfig().getLocalPluginRepo().getUrl());
		editLocalDiscovery.setDialogTitle("Define Discovery Path");
		editLocalDiscovery
				.setDialogMessage("Provide a path to plug-in descriptors relative to external storage root (can be a directory or specific file)");
		editLocalDiscovery.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				editLocalDiscovery.setSummary(newValue.toString());
				return true;
			}
		});
		updateSettingsCat.addPreference(editLocalDiscovery);
		/*
		 * !!! IMPORTANT !!! If you want to programatically set dependencies, you need to call addPreference before the
		 * 'setDependency' call due to internal Android checking. Link:
		 * http://groups.google.com/group/android-developers/browse_thread/thread/9db470e8ecd65d86
		 */
		editLocalDiscovery.setDependency(toggleLocalDiscovery.getKey());
	}
}
