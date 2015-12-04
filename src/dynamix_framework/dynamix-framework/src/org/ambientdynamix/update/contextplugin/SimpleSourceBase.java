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
package org.ambientdynamix.update.contextplugin;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.VersionInfo;
import org.ambientdynamix.api.contextplugin.PluginConstants.PLATFORM;
import org.ambientdynamix.core.BaseActivity;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.util.RepositoryInfo;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.util.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import eu.smartsantander.androidExperimentation.operations.Communication;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.Toast;

public class SimpleSourceBase {
    private final String TAG = this.getClass().getSimpleName();
    private final String URLS = Constants.URL;

    protected List<DiscoveredContextPlugin> createDiscoveredPlugins(
            RepositoryInfo repo, InputStream input, PLATFORM platform,
            VersionInfo platformVersion, VersionInfo frameworkVersion,
            boolean processSingle) throws Exception {

        // SmartSantander Modifications
        Log.i("AndroidExperimentation", "Start Plugin Discovery");
        String jsonPluginList = "";
        final List<DiscoveredContextPlugin> plugs = new ArrayList<>();
        try {
            PluginList pluginList = getPluginList();
            Log.i(TAG, "Plugin List set");
            final List<Plugin> plugList = pluginList.getPluginList();
            for (Plugin plugInfo : plugList) {
                if (isEnabled(plugInfo)) {
                    continue;
                }
                Log.i(TAG, "Found Plugin:" + plugInfo.getName());
                ContextPluginBinder plugBinder = new ContextPluginBinder();
                DiscoveredContextPlugin plug = plugBinder
                        .createDiscoveredPlugin(repo, plugInfo);
                plugs.add(plug);
            }
            return plugs;
        } catch (Exception e) {
            Log.w(TAG, "Exception creating discovered plugins: " + e.getMessage());
            BaseActivity.toast("Exception creating discovered plugins:" + e.getMessage(), Toast.LENGTH_LONG);
            return plugs;
        }
    }

    Boolean isEnabled(Plugin plugInfo) {
        for (ContextPluginInformation plugin : DynamixService
                .getAllContextPluginInfo()) {
            if (plugin.getPluginName().equals(plugInfo.getName())) {
                return true;
            }
        }
        return false;
    }

    public PluginList getPluginList() throws Exception {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/dynamix");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Communication communication = new Communication();
        List<Plugin> pluginList = communication.sendGetPluginList(
                String.valueOf(DynamixService.getPhoneProfiler().getPhoneId()));
        DynamixService.setDiscoveredPlugins(pluginList);
        Plugin pluginXML = null;
        for (Plugin plugin : pluginList) {
            Log.i(TAG, "Found Plugin1:" + plugin.getName());
            Constants.checkFile(plugin.getFilename(), plugin.getInstallUrl());
            if (plugin.getName().equals("plugs.xml")) {
                pluginXML = plugin;
            }
        }
        pluginList.remove(pluginXML);
        PluginList plist = new PluginList();
        plist.setPluginList(pluginList);
        Log.i(TAG, "Found Plugins:" + pluginList.size());


        final SharedPreferences pref = DynamixService.getAndroidContext()
                .getApplicationContext().getSharedPreferences("runningJob", 0); // 0
        // -
        // for
        // private
        // mode
        final Editor editor = (DynamixService.getAndroidContext().getSharedPreferences(
                "pluginObjects", 0)).edit();
        final String plistString = (new Gson()).toJson(plist, PluginList.class);
        editor.putString("pluginObjects", plistString);
        editor.apply();

        return plist;

    }
}
