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
package org.ambientdynamix.data;

import java.util.List;

import org.ambientdynamix.api.contextplugin.ContextPlugin;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.core.DynamixApplication;
import org.ambientdynamix.update.contextplugin.DiscoveredContextPlugin;

/**
 * Primary interface for user-based settings management. The use of this interface decouples the Dynamix Framework from
 * its underlying database technology, allowing the database type to be changed if needed.
 * 
 * @author Darren Carlson
 */
public interface ISettingsManager {
	/**
	 * Adds the incoming application to the List of authorized applications.
	 */
	void addAuthorizedApplication(DynamixApplication app);

	/**
	 * Adds the specified ClassLoader to the ISettingsManager. Useful in some object-based database implementations.
	 */
	void addClassLoader(ClassLoader loader);

	/**
	 * Adds the ContextPlugin to the DynamixSettings object, for each authorized and pending application.
	 */
	boolean addContextPlugin(ContextPlugin plugin);

	/**
	 * Adds the incoming application to the List of denied applications.
	 */
	void addDeniedApplication(DynamixApplication deniedApplication);

	/**
	 * Adds the incoming application to the List of pending applications.
	 */
	boolean addPendingApplication(DynamixApplication app);

	/**
	 * Adds the incoming application to the set of authorized applications. Note that the incoming app must be present
	 * in the pending applications List. If it is, the application is removed from the pending list and added to the
	 * authorized applications list. Returns true if the application was authorized; false, otherwise.
	 */
	boolean authorizePendingApplication(DynamixApplication app);

	/**
	 * Returns true if the application ID is authorized; false, otherwise.
	 */
	boolean checkApplicationAuthorized(int appID);

	/**
	 * Returns true if the application ID is pending; false, otherwise.
	 */
	boolean checkApplicationPending(int appID);

	/**
	 * Clears existing settings and creates a new, default settings object.
	 */
	void clearSettings();

	/**
	 * Closes the database and releases any acquired resources.
	 */
	void closeDatabase();

	/**
	 * Returns the application for the provided appID from *either* the authorized or pending application lists. Returns
	 * NULL, if the appID is not found.
	 */
	DynamixApplication getDynamixApplication(int appId);

	/**
	 * Returns the authorized application for the provided appID. Returns NULL, if the appID is not found.
	 */
	DynamixApplication getAuthorizedApplication(int appId);

	/**
	 * Returns a read-only List of authorized applications.
	 */
	List<DynamixApplication> getAuthorizedApplications();

	/**
	 * Returns the ContextPlugin associated with the incoming id.
	 */
	ContextPlugin getContextPlugin(String id);

	/**
	 * Returns a read-only List of installed ContextPlugins.
	 */
	List<ContextPlugin> getInstalledContextPlugins();

	/**
	 * Returns the ContextPluginSettings for the specified ContextPlugin
	 * 
	 * @param plug
	 *            The ContextPlugin requesting settings Returns the ContextPluginSettings for the specified
	 *            ContextPlugin, or null if the ContextPlugin is not found.
	 */
	ContextPluginSettings getContextPluginSettings(ContextPlugin plug);

	/**
	 * Returns the List of pending ContextPlugins, which have been discovered but not installed.
	 */
	List<DiscoveredContextPlugin> getPendingContextPlugins();

	/**
	 * Returns a read-only List of denied applications.
	 */
	List<DynamixApplication> getDeniedApplications();

	/**
	 * Returns a read-only List of pending applications.
	 */
	List<DynamixApplication> getPendingApplications();

	/**
	 * Returns the current Dynamix Framework PowerScheme.
	 */
	PowerScheme getPowerScheme();

	/**
	 * Returns the DynamixSettings object managed by the ISettingsManager
	 */
	DynamixSettings getSettings();

	/**
	 * Returns True if Dynamix had a clean exit; false otherwise.
	 */
	Boolean hadCleanExit();

	/**
	 * Opens the database using the specified database path. This method will always be called by the Dynamix Framework
	 * before all other ISettingsManager methods. Implementations should open the database using the incoming path and
	 * acquire any necessary resources needed to manage settings. the Dynamix Framework will not start if this method
	 * throws an exception.
	 * 
	 * @param path
	 *            The path of the database to open.
	 */
	void openDatabase(String path) throws Exception;

	/**
	 * Removes a DynamixApplication from the List of authorized applications. Returns true if the application was
	 * remove; false, otherwise.
	 */
	boolean removeApplication(DynamixApplication app);

	/**
	 * Removes the specified ClassLoader from the ISettingsManager. Useful in some object-based database
	 * implementations.
	 */
	void removeClassLoader(ClassLoader loader);

	/**
	 * Removes the ContextPlugin from the List of ContextPlugins.
	 */
	boolean removeContextPlugin(ContextPlugin plugin);

	/**
	 * Removes the ContextPluginSettings for the specified ContextPlugin.
	 * 
	 * @param plug
	 *            Returns true if the ContextPluginSettings was removed; false otherwise.
	 */
	boolean removeContextPluginSettings(ContextPlugin plug);

	/**
	 * Replaces the original plugin with the new plugin while maintaining all original settings
	 */
	boolean replaceContextPlugin(ContextPlugin originalPlugin, ContextPlugin newPlugin);

	/**
	 * Sets if Dynamix had a clean exit.
	 * 
	 * @param cleanExit
	 *            True if the exit was clean (no errors); false otherwise.
	 */
	void setCleanExit(Boolean cleanExit);

	/**
	 * Sets the list of available ContextPluginUpdates.
	 */
	void setContextPluginUpdates(List<DiscoveredContextPlugin> updates);

	/**
	 * Sets the current Dynamix Framework PowerScheme.
	 */
	void setPowerScheme(PowerScheme newScheme);

	/**
	 * Replaces the specified ContextPlugin with the new ContextPluginSettings
	 * 
	 * @param plug
	 *            The ContextPlugin's settings to update
	 * @param plugSet
	 *            The new ContextPluginSettings object that should replace the existing settings. Returns true if the
	 *            ContextPluginSettings was stored for the ContextPlugin; false otherwise.
	 */
	boolean storeContextPluginSettings(ContextPlugin plug, ContextPluginSettings settings);

	/**
	 * Updates application privacy policies for all registered applications. Returns true if the policies were updated;
	 * false otherwise.
	 */
	boolean updateAllApplicationPrivacyPolicies();

	/**
	 * Replaces the application with the incoming application, regardless if the app is pending or authorized. Returns
	 * true if the application was updated; false, otherwise.
	 */
	boolean updateApplication(DynamixApplication app);

	/**
	 * Updates the specified ContextPlugin.
	 */
	boolean updateContextPlugin(ContextPlugin plug);
}