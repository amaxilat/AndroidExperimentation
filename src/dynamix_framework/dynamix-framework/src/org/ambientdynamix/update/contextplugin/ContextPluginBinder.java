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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.ambientdynamix.api.application.AppConstants.ContextPluginType;
import org.ambientdynamix.api.application.VersionInfo;
import org.ambientdynamix.api.contextplugin.ContextPlugin;
import org.ambientdynamix.api.contextplugin.PluginConstants.PLATFORM;
import org.ambientdynamix.api.contextplugin.PluginConstants.UpdatePriority;
import org.ambientdynamix.api.contextplugin.security.Permission;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.util.RepositoryInfo;
import org.ambientdynamix.util.Utils;

import android.util.Log;

import gr.cti.android.experimentation.model.Plugin;

/**
 * Simple Framework binder class for XML parsing.
 *
 * @author Darren Carlson
 */
//@Element(name = "contextPlugin")
public class ContextPluginBinder {
    private final String TAG = getClass().getSimpleName();

    final String id = "";
    final String platform = "android";
    final String pluginVersion = "1.0.0";
    final String minPlatformVersion = "2.0";
    final String maxPlatformVersion = "3.0";
    final String minFrameworkVersion = "0.9.47";
    final String maxFrameworkVersion = "0.9.48";
    final String provider = "Dynamix Project";
    final boolean requiresConfiguration = false;
    final boolean hasConfigurationView = false;
    final String runtimeFactoryClass = "";
    final String name = "";
    final String description = "";
    final String pluginType = "REACTIVE";
    final List<String> supportedContextTypes = new ArrayList<>();
    final List<String> permissions = new ArrayList<>();
    String installUrl = "";
    String updateUrl = "";
    final String repoType = "";

    public DiscoveredContextPlugin createDiscoveredPlugin(RepositoryInfo source) throws Exception {
        ContextPlugin newPlug = new ContextPlugin();
        newPlug.setId(id);
        newPlug.setRepoSource(source);
        newPlug.setTargetPlatform(PLATFORM.getPlatformFromString(platform));
        newPlug.setVersionInfo(VersionInfo.createVersionInfo(pluginVersion));
        newPlug.setMinPlatformApiLevel(VersionInfo.createVersionInfo(minPlatformVersion));
        // Check for optional max platform
        if (maxPlatformVersion != null && maxPlatformVersion.length() > 0)
            newPlug.setMaxPlatformApiLevel(VersionInfo.createVersionInfo(maxPlatformVersion));
        newPlug.setMinFrameworkVersion(VersionInfo.createVersionInfo(minFrameworkVersion));
        // Check for optional max Dynamix framework
        if (maxFrameworkVersion != null && maxFrameworkVersion.length() > 0)
            newPlug.setMaxFrameworkVersion(VersionInfo.createVersionInfo(maxFrameworkVersion));
        newPlug.setProvider(provider);
        newPlug.setRequiresConfiguration(requiresConfiguration);
        newPlug.setHasConfigurationView(hasConfigurationView);
        newPlug.setRuntimeFactoryClass(runtimeFactoryClass);
        newPlug.setName(name);
        newPlug.setDescription(description);
        // Setup the plug-in's type
        newPlug.setContextPluginType(Utils.getEnumFromString(ContextPluginType.class, pluginType));
        // Setup supported privacy risk levels

        Map<PrivacyRiskLevel, String> riskLevelsMap = new HashMap<>();
//		for (PrivacyRiskLevelBinder rl : supportedPrivacyRiskLevels)
//		{
        PrivacyRiskLevel l = PrivacyRiskLevel.getLevelForString("LOW");
        if (l != null && !riskLevelsMap.containsKey(l))
            riskLevelsMap.put(l, description);
        else
            Log.w(TAG, "PrivacyRiskLevel null or duplicated: " + l);
//		}
        newPlug.setSupportedPrivacyRiskLevels(riskLevelsMap);
        // Setup supported context types

        supportedContextTypes.add("org.ambientdynamix.contextplugins.addplugin");

        newPlug.setSupportedContextTypes(supportedContextTypes);
        newPlug.setPermissions(new LinkedHashSet<Permission>());
        // Setup permissions

        for (String permissionString : permissions) {
            Permission p = Permission.createPermission(permissionString);
            if (p != null) {
                // TODO: For now we grant all permissions - update this
                p.setPermissionGranted(true);
                newPlug.getPermissions().add(p);
                Log.w(TAG, "For testing, we are automatically granting Permission: " + permissionString + " to "
                        + newPlug);
            }
        }
        Log.w(TAG, "Automatically granting Permissions.LAUNCH_CONTEXT_ACQUISITION_ACTIVITY");
        newPlug.getPermissions().add(Permission.createPermission("Permissions.LAUNCH_CONTEXT_ACQUISITION_ACTIVITY"));

        // Setup feature dependencies
/*		newPlug.setFeatureDependencies(new LinkedHashSet<DynamixFeatureInfo>());
        if (featureDependencies != null)
			for (FeatureDependencyBinder f : featureDependencies) {
				newPlug.getFeatureDependencies().add(new DynamixFeatureInfo(f.name, f.required));
			}
*/
        newPlug.setInstallUrl(installUrl);
        newPlug.setUpdateUrl(updateUrl);
        // Validate the plugin and add it to the list of ContextPluginUpdates
        if (Utils.validateContextPlugin(newPlug)) {

            String messsage = "we are the cool guys";

            //		String messsage = updateMessage.message;
            //		UpdatePriority priority = UpdatePriority.valueOf(updateMessage.priority);

            UpdatePriority priority = UpdatePriority.valueOf("NORMAL");

            return new DiscoveredContextPlugin(newPlug, messsage, priority);

        } else {
            throw new Exception("Context Plugin Invalid");
        }
    }

    //Organicity Modification
    public DiscoveredContextPlugin createDiscoveredPlugin(RepositoryInfo source, Plugin plugInfo) throws Exception {
        String id = String.valueOf(plugInfo.getContextType());
        String runtimeFactoryClass = plugInfo.getRuntimeFactoryClass();
        String name = plugInfo.getName();
        String description = plugInfo.getDescription();
        String installUrl = plugInfo.getInstallUrl();

        ContextPlugin newPlug = new ContextPlugin();
        newPlug.setId(id);
        newPlug.setRepoSource(source);
        newPlug.setTargetPlatform(PLATFORM.getPlatformFromString(platform));
        newPlug.setVersionInfo(VersionInfo.createVersionInfo(pluginVersion));
        newPlug.setMinPlatformApiLevel(VersionInfo.createVersionInfo(minPlatformVersion));
        // Check for optional max platform
        if (maxPlatformVersion != null && maxPlatformVersion.length() > 0)
            newPlug.setMaxPlatformApiLevel(VersionInfo.createVersionInfo(maxPlatformVersion));
        newPlug.setMinFrameworkVersion(VersionInfo.createVersionInfo(minFrameworkVersion));
        // Check for optional max Dynamix framework
        if (maxFrameworkVersion != null && maxFrameworkVersion.length() > 0)
            newPlug.setMaxFrameworkVersion(VersionInfo.createVersionInfo(maxFrameworkVersion));
        newPlug.setProvider(provider);
        newPlug.setRequiresConfiguration(requiresConfiguration);
        newPlug.setHasConfigurationView(hasConfigurationView);
        newPlug.setRuntimeFactoryClass(runtimeFactoryClass);
        newPlug.setName(name);
        newPlug.setDescription(description);
        // Setup the plug-in's type
        newPlug.setContextPluginType(Utils.getEnumFromString(ContextPluginType.class, pluginType));
        // Setup supported privacy risk levels

        Map<PrivacyRiskLevel, String> riskLevelsMap = new HashMap<>();
        PrivacyRiskLevel l = PrivacyRiskLevel.getLevelForString("LOW");
        if (l != null && !riskLevelsMap.containsKey(l))
            riskLevelsMap.put(l, description);
        else
            Log.w(TAG, "PrivacyRiskLevel null or duplicated: " + l);

        newPlug.setSupportedPrivacyRiskLevels(riskLevelsMap);
        // Setup supported context types

        supportedContextTypes.add(id);

        newPlug.setSupportedContextTypes(supportedContextTypes);
        newPlug.setPermissions(new LinkedHashSet<Permission>());
        // Setup permissions

        for (String permissionString : permissions) {
            Permission p = Permission.createPermission(permissionString);
            if (p != null) {
                // TODO: For now we grant all permissions - update this
                p.setPermissionGranted(true);
                newPlug.getPermissions().add(p);
                Log.w(TAG, "For testing, we are automatically granting Permission: " + permissionString + " to "
                        + newPlug);
            }
        }
        Log.w(TAG, "Automatically granting Permissions.LAUNCH_CONTEXT_ACQUISITION_ACTIVITY");
        newPlug.getPermissions().add(Permission.createPermission("Permissions.LAUNCH_CONTEXT_ACQUISITION_ACTIVITY"));

        newPlug.setInstallUrl(installUrl);
        newPlug.setUpdateUrl(updateUrl);
        // Validate the plugin and add it to the list of ContextPluginUpdates
        Log.d(TAG, "Validating new plug-in: " + newPlug);
        if (Utils.validateContextPlugin(newPlug)) {
            Log.d(TAG, "Plug-in is valid.");

            if (1 == 1) {
                String messsage = "we are the cool guys";

                UpdatePriority priority = UpdatePriority.valueOf("NORMAL");

                return new DiscoveredContextPlugin(newPlug, messsage, priority);
            } else {
                return new DiscoveredContextPlugin(newPlug);
            }
        } else {
            throw new Exception("Context Plugin Invalid");
        }
    }


    //Organicity Modification
    public ContextPlugin createContextPlugin(RepositoryInfo source, Plugin plugInfo) throws Exception {
        String id = String.valueOf(plugInfo.getContextType());
        String runtimeFactoryClass = plugInfo.getRuntimeFactoryClass();
        String name = plugInfo.getName();
        String description = plugInfo.getDescription();
        String installUrl = plugInfo.getInstallUrl();

        ContextPlugin newPlug = new ContextPlugin();
        newPlug.setId(id);
        newPlug.setRepoSource(source);
        newPlug.setTargetPlatform(PLATFORM.getPlatformFromString(platform));
        newPlug.setVersionInfo(VersionInfo.createVersionInfo(pluginVersion));
        newPlug.setMinPlatformApiLevel(VersionInfo.createVersionInfo(minPlatformVersion));
        // Check for optional max platform
        if (maxPlatformVersion != null && maxPlatformVersion.length() > 0)
            newPlug.setMaxPlatformApiLevel(VersionInfo.createVersionInfo(maxPlatformVersion));
        newPlug.setMinFrameworkVersion(VersionInfo.createVersionInfo(minFrameworkVersion));
        // Check for optional max Dynamix framework
        if (maxFrameworkVersion != null && maxFrameworkVersion.length() > 0)
            newPlug.setMaxFrameworkVersion(VersionInfo.createVersionInfo(maxFrameworkVersion));
        newPlug.setProvider(provider);
        newPlug.setRequiresConfiguration(requiresConfiguration);
        newPlug.setHasConfigurationView(hasConfigurationView);
        newPlug.setRuntimeFactoryClass(runtimeFactoryClass);
        newPlug.setName(name);
        newPlug.setDescription(description);
        // Setup the plug-in's type
        newPlug.setContextPluginType(Utils.getEnumFromString(ContextPluginType.class, pluginType));
        // Setup supported privacy risk levels

        Map<PrivacyRiskLevel, String> riskLevelsMap = new HashMap<>();
        PrivacyRiskLevel l = PrivacyRiskLevel.getLevelForString("LOW");
        if (l != null && !riskLevelsMap.containsKey(l))
            riskLevelsMap.put(l, description);
        else
            Log.w(TAG, "PrivacyRiskLevel null or duplicated: " + l);

        newPlug.setSupportedPrivacyRiskLevels(riskLevelsMap);
        // Setup supported context types

        supportedContextTypes.add(id);

        newPlug.setSupportedContextTypes(supportedContextTypes);
        newPlug.setPermissions(new LinkedHashSet<Permission>());
        // Setup permissions

        for (String permissionString : permissions) {
            Permission p = Permission.createPermission(permissionString);
            if (p != null) {
                // TODO: For now we grant all permissions - update this
                p.setPermissionGranted(true);
                newPlug.getPermissions().add(p);
                Log.w(TAG, "For testing, we are automatically granting Permission: " + permissionString + " to "
                        + newPlug);
            }
        }
        Log.w(TAG, "Automatically granting Permissions.LAUNCH_CONTEXT_ACQUISITION_ACTIVITY");
        newPlug.getPermissions().add(Permission.createPermission("Permissions.LAUNCH_CONTEXT_ACQUISITION_ACTIVITY"));

        newPlug.setInstallUrl(installUrl);
        newPlug.setUpdateUrl(updateUrl);
        // Validate the plugin and add it to the list of ContextPluginUpdates
        if (Utils.validateContextPlugin(newPlug)) {
            return newPlug;

        } else {
            throw new Exception("Context Plugin Invalid");
        }
    }

}
