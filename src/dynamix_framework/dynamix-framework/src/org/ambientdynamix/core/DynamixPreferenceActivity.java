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
 * @author Darren Carlson
 * @see DynamixService
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

        if (FrameworkConstants.ADMIN_RELEASE) {
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
    }
}
