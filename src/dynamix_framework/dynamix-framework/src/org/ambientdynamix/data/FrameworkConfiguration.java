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

import java.io.FileInputStream;
import java.util.Properties;

import org.ambientdynamix.core.FrameworkConstants;
import org.ambientdynamix.util.RepositoryInfo;

import android.util.Log;

/**
 * Represents general configuration settings for the Dynamix Framework.
 *
 * @author Darren Carlson
 */
public class FrameworkConfiguration {
    public final static String TAG = FrameworkConfiguration.class.getSimpleName();
    // Configuration keys
    public static final String DATABASE_PATH = "database.path";
    public static final String ALLOW_FRAMEWORK_UPDATES = "allow.framework.updates";
    public static final String ALLOW_CONTEXT_PLUGIN_UPDATES = "allow.context.plugin.updates";
    public static final String CONTEXT_CACHE_MAX_EVENTS = "context.cache.max.events";
    public static final String CONTEXT_CACHE_MAX_DURATION_MILLS = "context.cache.max.duration.mills";
    public static final String CONTEXT_CACHE_CULL_INTERVAL_MILLS = "context.cache.cull.interval.mills";
    public static final String ALLOW_REMOTE_CONFIGURATION = "allow.remote.configuration";
    public static final String ALLOW_USER_SERVER_MANAGEMENT = "allow.user.server.management";
    public static final String APP_LIVELINESS_CHECK_MILLS = "app.liveliness.check.mills";
    public static final String APP_INACTIVITY_TIMEOUT_MILLS = "app.inactivity.timeout.mills";
    public static final String PRIMARY_DYNAMIX_SERVER_ALIAS = "primary.dynamix.server.alias";
    public static final String PRIMARY_DYNAMIX_SERVER_URL = "primary.dynamix.server.url";
    public static final String BACKUP_DYNAMIX_SERVER_ALIAS = "backup.dynamix.server.alias";
    public static final String BACKUP_DYNAMIX_SERVER_URL = "backup.dynamix.server.url";
    public static final String PRIMARY_REMOTE_CONFIG_SERVER_ALIAS = "primary.remote.config.server.alias";
    public static final String PRIMARY_REMOTE_CONFIG_SERVER_URL = "primary.remote.config.server.url";
    public static final String BACKUP_REMOTE_CONFIG_SERVER_ALIAS = "backup.remote.config.server.alias";
    public static final String BACKUP_REMOTE_CONFIG_SERVER_URL = "backup.remote.config.server.url";
    public static final String INSTALLER_WORKER_COUNT = "installer.workers.count";
    public static final String DEFAULT_BUFFER_SIZE = "default.buffer.size";
    public static final String HEAP_MEMORY_PROTECTION_THRESHOLD = "heap.memory.protection.threshold";
    public static final String ALLOW_PRIMARY_REPO_DEACTIVATE = "allow.primary.context.plugin.repo.deactivate";
    public static final String ALLOW_ADDITIONAL_CONTEXT_PLUGIN_REPOS = "allow.additional.context.plugin.repos";
    public static final String ALLOW_SELF_SIGNED_CERTS_DEFAULT = "allow.self.signed.certs.default";
    // Private data
    private String databaseFilePath = "data/database.dat";
    private int appLivelinessCheckIntervalMills = 5000;
    private int contextCacheMaxEvents = 250;
    private int contextCacheMaxDurationMills = 3600000;
    private int contextCacheCullIntervalMills = 2500;
    private int appInactivityTimeoutMills = 15000;
    private int installerWorkersCount = 2;
    private int defaultBufferSize = 8192;
    private int heapMemoryProtectionThreshold = 90;
    private boolean allowPrimaryContextPluginRepoDeactivate = true;
    private boolean allowAdditionalContextPluginRepos = true;
    private boolean allowSelfSignedCertsDefault = false;
    private boolean localContextPluginDiscoveryEnabled = true;

    public FrameworkConfiguration() {
    }

    /**
     * Factory method that creates a FrameworkConfiguration using the specified properties file path. This method
     * intelligently parses the specified properties file and throws detailed exceptions if errors are found.
     *
     * @param propsFilePath The path to the properties file Returns a configured FrameworkConfiguration based on the specified
     *                      properties file.
     * @throws Exception If the properties file cannot be parsed (includes a detailed error message).
     */
    public static FrameworkConfiguration createFromPropsFile(String propsFilePath) throws Exception {
        // Create a new FrameworkConfiguration
        FrameworkConfiguration config = new FrameworkConfiguration();
        // Create a Properties entity
        Properties props = new Properties();
        // Load the config settings using the propsFilePath
        props.load(new FileInputStream(propsFilePath));
        // Set basic configuration options
        config.setDatabaseFilePath(validate(props, DATABASE_PATH));
        config.setContextCacheMaxEvents(Integer.parseInt(validate(props, CONTEXT_CACHE_MAX_EVENTS)));
        config.setContextCacheMaxDurationMills(Integer.parseInt(validate(props, CONTEXT_CACHE_MAX_DURATION_MILLS)));
        config.setContextCacheCullIntervalMills(Integer.parseInt(validate(props, CONTEXT_CACHE_CULL_INTERVAL_MILLS)));
        config.setAppLivelinessCheckIntervalMills(Integer.parseInt(validate(props, APP_LIVELINESS_CHECK_MILLS)));
        config.setAppInactivityTimeoutMills(Integer.parseInt(validate(props, APP_INACTIVITY_TIMEOUT_MILLS)));
        config.setInstallerWorkersCount(Integer.parseInt(validate(props, INSTALLER_WORKER_COUNT)));
        config.setDefaultBufferSize(Integer.parseInt(validate(props, DEFAULT_BUFFER_SIZE)));
        config.setHeapMemoryProtectionThreshold(Integer.parseInt(validate(props, HEAP_MEMORY_PROTECTION_THRESHOLD)));
        config.setAllowPrimaryContextPluginRepoDeactivate(Boolean.parseBoolean(validate(props,
                ALLOW_PRIMARY_REPO_DEACTIVATE)));
        config.setAllowAdditionalContextPluginRepos(Boolean.parseBoolean(validate(props,
                ALLOW_ADDITIONAL_CONTEXT_PLUGIN_REPOS)));
        config.setAllowSelfSignedCertsDefault(Boolean.parseBoolean(validate(props, ALLOW_SELF_SIGNED_CERTS_DEFAULT)));
        // if Dynamix updates are allowed, setup the update servers
        // if context plug-in updates are allowed, setup the context plug-in repos
        // config.setDefaultLocalPluginPath(props.getProperty(DEFAULT_LOCAL_PLUGIN_REPO_URL));
        // Setup the optional local repo
        return config;
    }

    /**
     * Returns the time period of inactivity necessary for a bound application to listed as inactive (in milliseconds)
     */
    public int getAppInactivityTimeoutMills() {
        return appInactivityTimeoutMills;
    }

    /**
     * Returns how often Dynamix should check if registered applications are alive (in milliseconds)
     */
    public int getAppLivelinessCheckIntervalMills() {
        return appLivelinessCheckIntervalMills;
    }

    /**
     * Returns How often the context event cache should scan for and remove expired events (in milliseconds)
     */
    public int getContextCacheCullIntervalMills() {
        return contextCacheCullIntervalMills;
    }

    /**
     * Returns the max duration that a context event may be cached (in milliseconds)
     */
    public int getContextCacheMaxDurationMills() {
        return contextCacheMaxDurationMills;
    }

    /**
     * Returns the max number of context events that may be cached (Note that 0 implies disabled)
     */
    public int getContextCacheMaxEvents() {
        return contextCacheMaxEvents;
    }

    /**
     * Returns the path of the database from the root of the Dynamix installation directory, including the database's
     * filename and extension.
     */
    public String getDatabaseFilePath() {
        return databaseFilePath;
    }

    /**
     * Sets the time period of inactivity necessary for a bound application to listed as inactive (in milliseconds)
     */
    public void setAppInactivityTimeoutMills(int appInactivityTimeoutMills) {
        this.appInactivityTimeoutMills = appInactivityTimeoutMills;
    }

    /**
     * Sets how often Dynamix should check if bound applications are alive (in milliseconds)
     */
    public void setAppLivelinessCheckIntervalMills(int appLivelinessCheckIntervalMills) {
        this.appLivelinessCheckIntervalMills = appLivelinessCheckIntervalMills;
    }

    /**
     * Sets how often the context event cache should scan for and remove expired events (in milliseconds)
     */
    public void setContextCacheCullIntervalMills(int contextCacheCullIntervalMills) {
        this.contextCacheCullIntervalMills = contextCacheCullIntervalMills;
    }

    /**
     * Sets the max duration that a context event may be cached (in milliseconds)
     */
    public void setContextCacheMaxDurationMills(int contextCacheMaxDurationMills) {
        this.contextCacheMaxDurationMills = contextCacheMaxDurationMills;
    }

    /**
     * Sets the max number of context events that may be cached (Note that 0 implies disabled)
     */
    public void setContextCacheMaxEvents(int contextCacheMaxEvents) {
        this.contextCacheMaxEvents = contextCacheMaxEvents;
    }


    /**
     * Sets path of the database from the root of the Dynamix installation directory, including the database's filename
     * and extension.
     */
    public void setDatabaseFilePath(String databaseFilePath) {
        this.databaseFilePath = databaseFilePath;
    }

    /**
     * Returns the total number of installer workers allowed.
     */
    public int getInstallerWorkersCount() {
        return installerWorkersCount;
    }

    /**
     * Sets the total number of installer workers allowed.
     */
    public void setInstallerWorkersCount(int installerWorkersCount) {
        this.installerWorkersCount = installerWorkersCount;
    }

    /**
     * Returns the default buffer size.
     */
    public int getDefaultBufferSize() {
        return defaultBufferSize;
    }

    /**
     * Sets the default buffer size.
     */
    public void setDefaultBufferSize(int defaultBufferSize) {
        this.defaultBufferSize = defaultBufferSize;
    }

    /**
     * Returns the percentage of process heap memory (as an int - e.g., 90 = 90%) that may be consumed before Dynamix
     * implements memory protection (e.g., by dropping events).
     */
    public int getHeapMemoryProtectionThreshold() {
        return heapMemoryProtectionThreshold;
    }

    /**
     * Sets the percentage of process heap memory (as an int - e.g., 90 = 90%) that may be consumed before Dynamix
     * implements memory protection (e.g., by dropping events).
     */
    public void setHeapMemoryProtectionThreshold(int heapMemoryProtectionThreshold) {
        this.heapMemoryProtectionThreshold = heapMemoryProtectionThreshold;
    }

    /**
     * Returns true if the primary context plug-in repo can be deactivated by the user; false otherwise.
     */
    public boolean allowPrimaryContextPluginRepoDeactivate() {
        if (FrameworkConstants.ADMIN_RELEASE)
            return true;
        else
            return allowPrimaryContextPluginRepoDeactivate;
    }

    /**
     * Set true if the primary context plug-in repo can be deactivated by the user; false otherwise.
     */
    public void setAllowPrimaryContextPluginRepoDeactivate(boolean allowPrimaryContextPluginRepoDeactivate) {
        this.allowPrimaryContextPluginRepoDeactivate = allowPrimaryContextPluginRepoDeactivate;
    }

    /**
     * Returns true if additional context plugin repos can be added in addition to the primary repo; false otherwise.
     */
    public boolean allowAdditionalContextPluginRepos() {
        return allowAdditionalContextPluginRepos;
    }

    /**
     * Set true if additional context plugin repos can be added in addition to the primary repo; false otherwise.
     */
    public void setAllowAdditionalContextPluginRepos(boolean allowAdditionalContextPluginRepos) {
        this.allowAdditionalContextPluginRepos = allowAdditionalContextPluginRepos;
    }

    /**
     * Returns true if the default for allowing self signed certs is true; false otherwise.
     */
    public boolean allowSelfSignedCertsDefault() {
        return allowSelfSignedCertsDefault;
    }

    /**
     * Set true if the default for allowing self signed certs is true; false otherwise.
     */
    public void setAllowSelfSignedCertsDefault(boolean allowSelfSignedCerts) {
        this.allowSelfSignedCertsDefault = allowSelfSignedCerts;
    }

    /**
     * Returns true if local context plug-in discovery is enabled; false otherwise.
     */
    public boolean isLocalContextPluginDiscoveryEnabled() {
        return localContextPluginDiscoveryEnabled;
    }

    /**
     * Utility method that extracts the requested propString from the incoming Properties, throwing a detailed exception
     * if the string cannot be found.
     *
     * @param props      The Properties file.
     * @param propString The Property string to extract Returns the extracted property string.
     * @throws Exception If the property string cannot be found.
     */
    private static String validate(Properties props, String propString) throws Exception {
        String s = props.getProperty(propString);
        if (s == null)
            throw new Exception("Could not find: " + propString);
        return s;
    }
}
