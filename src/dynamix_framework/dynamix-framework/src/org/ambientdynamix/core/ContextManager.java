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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.api.application.ContextEvent;
import org.ambientdynamix.api.application.ContextSupportInfo;
import org.ambientdynamix.api.application.ContextSupportResult;
import org.ambientdynamix.api.application.ErrorCodes;
import org.ambientdynamix.api.application.IDynamixListener;
import org.ambientdynamix.api.application.Result;
import org.ambientdynamix.api.contextplugin.ContextInfoSet;
import org.ambientdynamix.api.contextplugin.ContextPlugin;
import org.ambientdynamix.api.contextplugin.ContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.IContextPluginRuntimeFactory;
import org.ambientdynamix.api.contextplugin.IPluginContextListener;
import org.ambientdynamix.api.contextplugin.IPluginFacade;
import org.ambientdynamix.api.contextplugin.NfcListener;
import org.ambientdynamix.api.contextplugin.PluginAlert;
import org.ambientdynamix.api.contextplugin.PluginConstants;
import org.ambientdynamix.api.contextplugin.PluginConstants.EventType;
import org.ambientdynamix.api.contextplugin.PluginState;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.Permission;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContext;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;
import org.ambientdynamix.core.EventCommand.CheckAppLiveliness;
import org.ambientdynamix.core.EventCommand.ContextRequestFailed;
import org.ambientdynamix.core.FrameworkConstants.StartState;
import org.ambientdynamix.data.ContextEventCache;
import org.ambientdynamix.data.ContextEventCacheEntry;
import org.ambientdynamix.data.DynamixPreferences;
import org.ambientdynamix.event.PluginDiscoveryResult;
import org.ambientdynamix.event.SimpleEventHandler;
import org.ambientdynamix.event.SourcedContextInfoSet;
import org.ambientdynamix.event.StreamController;
import org.ambientdynamix.security.PluginPrivacySettings;
import org.ambientdynamix.util.ContextPluginRuntimeWrapper;
import org.ambientdynamix.util.ContextRequest;
import org.ambientdynamix.util.EmptyContextPluginRuntime;
import org.ambientdynamix.util.PluginLooperThread;
import org.ambientdynamix.util.Utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * The ContextManager manages context acquisition and modeling within a Dynamix device by orchestrating the runtime
 * behavior of a set of dynamically installed ContextPlugins and their associated ContextPluginRuntimes. This class
 * implements IPluginContextListener in order to receive events from dependent ContextPluginRuntimes. It also implements
 * IPluginFacade, so that it can securely provision Android services to ContextPlugins. The ContextManager integrates a
 * ContextEventCache to cache and securely provision received ContextEvent data.
 *
 * @author Darren Carlson
 * @see IPluginContextListener
 * @see IPluginFacade
 */
class ContextManager implements IPluginContextListener, IPluginFacade {
    // Private data
    private final static String TAG = ContextManager.class.getSimpleName();
    private final Map<ContextPlugin, ContextPluginRuntimeWrapper> pluginMap = new ConcurrentHashMap<>();
    private final Map<ContextPlugin, SecuredContext> securedContextMap = new ConcurrentHashMap<>();
    private final Map<ContextPlugin, PluginStats> statMap = new ConcurrentHashMap<>();
    private final Map<ContextPlugin, List<NfcListener>> nfcListeners = new ConcurrentHashMap<>();
    private static final Map<ContextPlugin, PluginLooperThread> threadMap = new ConcurrentHashMap<>();
    private final Map<UUID, List<ContextRequest>> requestMap = new ConcurrentHashMap<>();
    private final Map<ContextPlugin, Activity> configActivityMap = new ConcurrentHashMap<>();
    private final Map<ContextPlugin, Activity> acquisitionActivityMap = new ConcurrentHashMap<>();
    private final Map<ContextPlugin, PlugStopper> stopperMap = new ConcurrentHashMap<>();
    /*
     * For pendingPluginStop: They key is the target plug-in, and the mapping (Boolean) is whether to destroy the
     * plug-in when the pending stop is executed.
     */
    private final Map<ContextPlugin, Boolean> pendingPluginStop = new ConcurrentHashMap<>();
    private static PowerScheme scheme = PowerScheme.BALANCED;
    private final Context context;
    private final ContextEventCache contextCache;
    private volatile StartState startState = StartState.STOPPED;
    private final Handler uiHandler = new Handler();
    private int progressCount = 0;
    private ProgressDialog progressDialog = null;
    private Timer progressMonitorTimer = null;

    /**
     * Creates a new ContextManager.
     *
     * @param context          The Android context.
     * @param scheme           The initial PowerScheme.
     * @param maxCacheCapacity The maximum capacity of the ContextDataCache.
     * @param maxCacheMills    How long to cache context events (in milliseconds)
     * @param cullInterval     How often to cull the context cache (in milliseconds)
     */
    protected ContextManager(Context context, PowerScheme scheme, int maxCacheCapacity, int maxCacheMills,
                             int cullInterval) {
        this.context = context;
        ContextManager.scheme = scheme;
        contextCache = new ContextEventCache(maxCacheMills, cullInterval, maxCacheCapacity);
    }

    /**
     * @return An appropriate Thread priority for the current PowerScheme
     */
    protected static int getThreadPriorityForPowerScheme() {
        if (scheme == PowerScheme.HIGH_PERFORMANCE)
            return Thread.MAX_PRIORITY;
        if (scheme == PowerScheme.POWER_SAVER)
            return Thread.MIN_PRIORITY;
        return Thread.NORM_PRIORITY;
    }

    /**
     * Clears the specified ContextPlugin's statistics
     *
     * @param plug The Plugin to clear.
     * @return True if the stats were cleared; false otherwise.
     */
    public boolean clearPluginStats(ContextPlugin plug) {
        synchronized (statMap) {
            PluginStats stats = statMap.get(plug);
            if (stats != null) {
                Log.d(TAG, "Clearing statistics for: " + plug);
                stats.clear();
                return true;
            }
            return false;
        }
    }

    /**
     * Programmatically closes a previously launched configuration activity.
     *
     * @param sessionId The sessionId of the ContextPluginRuntime wishing to close its configuration Activity.
     */
    @Override
    public void closeConfigurationView(UUID sessionId) {
        Log.v(TAG, "closeConfigurationView for " + sessionId);
        ContextPlugin plug = getContextPlugin(sessionId);
        if (plug != null) {
            if (configActivityMap.containsKey(plug)) {
                Activity act = configActivityMap.remove(plug);
                Log.d(TAG, "Closing Activity: " + act);
                Intent i = new Intent();
                i.putExtra("plug", plug);
                if (act.getParent() != null) {
                    act.getParent().setResult(Activity.RESULT_OK, i);
                } else
                    act.setResult(Activity.RESULT_OK, i);
                act.finish();
            } else {
                Log.w(TAG, "closeConfigurationView could not find an activity for plugin: " + plug);
            }
        } else
            Log.w(TAG, "closeConfigurationView could not find a plugin for UUID: " + sessionId);
    }

    /**
     * Programmatically closes a previously launched context acquisition activity.
     *
     * @param sessionId The sessionId of the ContextPluginRuntime wishing to close its context acquisition Activity.
     */
    @Override
    public void closeContextAcquisitionView(UUID sessionId) {
        Log.v(TAG, "closeContextAcquisitionView for " + sessionId);
        ContextPlugin plug = getContextPlugin(sessionId);
        if (plug != null) {
            synchronized (acquisitionActivityMap) {
                if (acquisitionActivityMap.containsKey(plug)) {
                    Activity act = acquisitionActivityMap.remove(plug);
                    Log.d(TAG, "Closing Activity: " + act);
                    act.finish();
                } else {
                    Log.w(TAG, "closeContextAcquisitionView could not find an activity for plugin: " + plug);
                }
            }
        } else
            Log.w(TAG, "closeContextAcquisitionView could not find a plugin for UUID: " + sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContextPluginSettings getContextPluginSettings(UUID sessionId) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionId);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            return DynamixService.SettingsManager.getContextPluginSettings(plug);
        } else
            Log.w(TAG, "getContextPluginSettings blocked for invalid sessionId: " + sessionId);
        return null;
    }

    /**
     * Returns the PluginStats for the specified ContextPlugin, or null if the plug-in was not found.
     */
    public PluginStats getPluginStats(ContextPlugin plug) {
        return statMap.get(plug);
    }

    /**
     * Returns the SecuredContext for the specified sessionId, or null if the sessionId was not found.
     */
    @Override
    public synchronized Context getSecuredContext(UUID sessionId) {
        /*
         * Important: we have to maintain control over the secured context and NOT give a new one each time a plug-in
		 * asks for it because they could possibly cache it and achieve permissions that changed over time.
		 */
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionId);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            PluginLooperThread plt = threadMap.get(plug);
            if (plt != null) {
                synchronized (securedContextMap) {
                    // Check if we've already created a SecuredContext for this plug
                    SecuredContext sc = securedContextMap.get(plug);
                    if (sc != null) {
                        return sc;
                    } else {
                        ClassLoader cl = DynamixService.getContextPluginClassLoader(plug);
                        if (cl != null) {

                            // -- SmartSantander modification here //todo fix this to generic

                            String plugId = plug.getId();
                            Log.i(TAG, "AD:SmartSantander Installing Plugin " + plugId);

                            switch (plugId) {
                                case "org.ambientdynamix.contextplugins.batteryLevelPlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.batteryTemperaturePlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.GpsPlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.WifiPlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.WifiScanPlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.TemperaturePlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.PressurePlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.HumidityPlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                case "org.ambientdynamix.contextplugins.StepCounterPlugin":
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), false);
                                    break;
                                default:
                                    sc = new SecuredContext(context, uiHandler, plt.getLooper(),
                                            DynamixService.getContextPluginClassLoader(plug), true);
                                    securedContextMap.put(plug, sc);
                                    break;
                            }

                            //

                            return sc;
                        } else {
                            Log.w(TAG, "Could not find ClassLoader for: " + plug);
                        }
                    }
                }
            } else
                Log.w(TAG, "Could not find PluginLooperThread for: " + plug);
        } else
            Log.w(TAG, "getSecuredContext blocked for invalid sessionId: " + sessionId);
        Log.w(TAG, "No SecuredContext could be created for: " + plug);
        return null;
    }

    /**
     * Returns the PluginState for the specified sessionId, or null if the sessionId was not found.
     */
    @Override
    public PluginState getState(UUID sessionId) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionId);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            ContextPluginRuntimeWrapper wrapper = pluginMap.get(plug);
            if (wrapper != null)
                return wrapper.getState();
            else
                Log.w(TAG, "getState encountered null ContextPluginRuntimeWrapper for: " + plug);
        } else
            Log.w(TAG, "getState blocked for invalid sessionId: " + sessionId);
        return null;
    }

    /**
     * Returns true if the ContextManager is started; false otherwise.
     */
    public boolean isStarted() {
        updateManagerState();
        return startState == StartState.STARTED;
    }

    /**
     * Returns true if the ContextManager is stopped; false otherwise.
     */
    public boolean isStopped() {
        updateManagerState();
        return startState == StartState.STOPPED;
    }

    /**
     * Returns true if the ContextManager is paused; false otherwise.
     */
    public boolean isPaused() {
        updateManagerState();
        return startState == StartState.PAUSED;
    }

    /**
     * Handles context scan failure events generated by managed ContextPluginRuntimes in response to specific context
     * scan requests.
     *
     * @param sessionId    The UUID sessionId of the ContextPluginRuntime generating the event.
     * @param requestId    The requestId that caused the error.
     * @param errorMessage The error message
     */
    @Override
    public void onContextRequestFailed(UUID sessionId, UUID requestId, String errorMessage, int errorCode) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionId);
        // Update stats
        PluginStats stats = statMap.get(plug);
        stats.handleContextScanFailed(errorMessage);
        // Translate the requestId into a list of specific requests (removing the list)
        List<ContextRequest> requests = requestMap.remove(requestId);
        // If we get a plug, we have a valid sessionId, so continue...
        if (plug != null) {
            // Notify each app that the context scan failed
            for (ContextRequest request : requests) {
                SessionManager.sendEventCommand(request.getApp(), request.getListener(), new ContextRequestFailed(
                        requestId.toString(), errorMessage, errorCode));
            }
        } else
            Log.w(TAG, "Could not find ContextPlugin for sessionId: " + sessionId);
    }

    /**
     * Handles SecuredEvents coming in from managed ContextPluginRuntimes. This method first makes sure that we've
     * received an event from a registered ContextPluginRuntime. Next, it verifies event data and notifies context
     * listeners (Applications) about the new context data. Finally, the method caches the event using the
     * ContextDataCache.
     *
     * @param sessionId The UUID sessionId of the ContextPluginRuntime generating the event.
     * @param infoSet   The ContextDataSet generated by the ContextPluginRuntime.
     */
    @Override
    public synchronized void onPluginContextEvent(UUID sessionId, final ContextInfoSet infoSet) {
        // Grab the plugin
        final ContextPlugin plug = getContextPlugin(sessionId);
        // If we get a plug, we have a valid sessionId, so continue...
        if (plug != null) {
            // Run event handling on a thread so it doesn't block Dynamix
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Verify that the context type is actually supported by the plugin
                    if (!plug.getSupportedContextTypes().contains(infoSet.getContextType()))
                        Log.e(TAG, "Context type: " + infoSet.getContextType() + " is not supported by plugin: " + plug);
                    // Get the context support associated with the infoSet's context type for all sessions.
                    List<ContextSupport> contextSupport = new Vector<>();
                    for (DynamixSession session : SessionManager.getAllSessions()) {
                        contextSupport.addAll(session.getContextSupport(infoSet.getContextType()));
                    }
                    // Create a SourcedContextDataSet using the ContextPlugin
                    SourcedContextInfoSet sourcedSet = null;
                    try {
                        sourcedSet = new SourcedContextInfoSet(infoSet, plug, true);
                    } catch (Exception e1) {
                        Log.e(TAG, "Error creating SourcedContextInfoSet (out of memory?) for: " + plug + ": " + e1);
                        if (infoSet.getEventType() == EventType.UNICAST) {
                            Log.w(TAG, "Informing requesting applications about the SourcedContextInfoSet error");
                            // Translate the responseId into a specific request list for the event
                            List<ContextRequest> requests = requestMap.remove(infoSet.getResponseId());
                            if (requests != null) {
                                // Process each request
                                for (ContextRequest request : requests) {
                                    // Grab the request's associated DynamixSession
                                    DynamixSession session = SessionManager.getSession(request.getApp());
                                    if (session != null) {
                                        SessionManager.sendEventCommand(
                                                request.getApp(),
                                                request.getListener(),
                                                new ContextRequestFailed(infoSet.getResponseId().toString(), e1
                                                        .toString(), ErrorCodes.INTERNAL_PLUG_IN_ERROR));
                                    }
                                }
                            }
                        } else
                            Log.d(TAG, "SourcedContextInfoSet error had a BROADCAST target... ignoring");
                    }
                    // Make sure that we have event data to process
                    if (sourcedSet.getContextInfoSet() != null
                            && sourcedSet.getContextInfoSet().getSecuredContextInfo().size() > 0) {
                        // Update the plug-ins stats
                        PluginStats stats = statMap.get(plug);
                        stats.handlePluginContextEvent(sourcedSet);
                        // Create a map of listeners and associated events
                        Map<IDynamixListener, List<ContextEvent>> eventMap = new HashMap<>();
                        // Handle event data of type BROADCAST
                        if (infoSet.getEventType() == EventType.BROADCAST) {
                            // Cache the SourcedContextInfoSet
                            contextCache.cacheEvent(plug, sourcedSet);
                            // Only process if we have support registrations
                            if (contextSupport != null && contextSupport.size() > 0) {
                                /*
                                 * For each ContextSupport, get the highest fidelity event suitable for the context
								 * support's DynamixApplication owner, adding it to the eventMap.
								 */
                                for (ContextSupport sub : contextSupport) {
                                    // Verify the support is of the correct data type
                                    if (sub.getContextType().equalsIgnoreCase(sourcedSet.getContextType())) {
                                        // Extract the highest fidelity context data available for the support's owner
                                        ContextEvent event = null;
                                        try {
                                            event = createContextEventForApplication(sub.getDynamixApplication(),
                                                    sourcedSet, infoSet.getResponseId());
                                        } catch (Exception e) {
                                            Log.e(TAG, "Exception when creating ContextEvent: " + e);
                                        }
                                        if (event != null) {
                                            // Subscriber is allowed to receive the event, so update its list of events
                                            if (eventMap.containsKey(sub.getDynamixListener())) {
                                                // Application already exists in the eventMap... so just extract its
                                                // event list
                                                List<ContextEvent> eventList = eventMap.get(sub.getDynamixListener());
                                                // Create the List of ContextEvents, if necessary
                                                if (eventList == null)
                                                    eventList = new ArrayList<>();
                                                // Add the ContextEvent to the application's list of events
                                                eventList.add(event);
                                            } else {
                                                // Application does not yet exist in the eventMap... so add it
                                                List<ContextEvent> eventList = new ArrayList<>();
                                                eventList.add(event);
                                                eventMap.put(sub.getDynamixListener(), eventList);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Handle event data of type UNICAST
                            if (infoSet.getEventType() == EventType.UNICAST) {
                                // Translate the responseId into a specific request list for the event
                                List<ContextRequest> requests = requestMap.remove(infoSet.getResponseId());
                                if (requests != null) {
                                    // Process each request
                                    for (ContextRequest request : requests) {
                                        // Grab the request's associated DynamixSession
                                        DynamixSession session = SessionManager.getSession(request.getApp());
                                        if (session != null) {
                                            // Access all the context support related to the request's listener
                                            List<ContextSupport> listenerSubs = session.getContextSupport(request
                                                    .getListener());
                                            if (listenerSubs != null) {
                                                for (ContextSupport sup : listenerSubs) {
                                                    // Check if the support matches the incoming dataSet's context type
                                                    if (sup.getContextType().equalsIgnoreCase(infoSet.getContextType())) {
                                                        // Cache the SourcedContextDataSet for the support's listener
                                                        contextCache.cacheEvent(sup.getDynamixListener(), plug, sourcedSet);
                                                        // Found a valid support registration, so try to create a
                                                        // ContextEvent
                                                        ContextEvent event = null;
                                                        try {
                                                            event = createContextEventForApplication(request.getApp(),
                                                                    sourcedSet, infoSet.getResponseId());
                                                        } catch (Exception e) {
                                                            Log.e(TAG, "Exception when creating ContextEvent: " + e);
                                                        }
                                                        if (event != null) {
                                                            // Application does not yet exist in the eventMap... so add it
                                                            List<ContextEvent> eventList = new ArrayList<>();
                                                            eventList.add(event);
                                                            eventMap.put(sup.getDynamixListener(), eventList);
                                                        } else {
                                                        /*
                                                         * TODO: Should we throw an Authentication exception here?
														 * Perhaps the app can ask if it has permission
														 */
                                                            Log.v(TAG, "App was blocked from receiving event");
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Log.w(TAG, "Could not find session for: " + request.getApp());
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Could not find receipients for responseId: " + infoSet.getResponseId());
                                }
                            } else {
                                throw new RuntimeException("Unknown event type: " + infoSet.getEventType());
                            }
                        }
                        /*
                         * Finally, notify the context listeners of the new events
						 */
                        Log.v(TAG, "onPluginContextEvent generated an eventMap of size: " + eventMap.size());

                        if (eventMap.size() > 0) {
                            SessionManager.notifyContextListeners(eventMap);
                        }
                    } else {
                        Log.w(TAG, "eventData was NULL... this should not happen!");
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } else
            Log.w(TAG, "Call blocked for invalid sessionId: " + sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setPluginConfiguredStatus(UUID sessionId, boolean configured) {
        // Get the ContextPlugin based on the sessionId
        ContextPlugin plug = getContextPlugin(sessionId);
        // If we get a plug, then the caller has an valid UUID, so continue...
        if (plug != null) {
            // Found the plug, so set its configured state
            plug.setConfigured(configured);
            // Update the SettingsManager
            DynamixService.SettingsManager.updateContextPlugin(plug);
            // Start the plugin, if needed
            startPlugin(plug);
        } else
            Log.w(TAG, "setPluginConfiguredStatus blocked for invalid sessionId: " + sessionId);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean storeContextPluginSettings(UUID sessionId, ContextPluginSettings settings) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionId);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            return DynamixService.SettingsManager.storeContextPluginSettings(plug, settings);
        } else
            Log.w(TAG, "storeContextPluginSettings blocked for invalid sessionId: " + sessionId);
        return false;
    }

    /**
     * Initializes the incoming plug-in by creating its runtime thread, creating its runtime and starting the plug-in,
     * if necessary. Note that plug-ins must be installed, enabled and in state NEW.
     *
     * @param plug      The target ContextPlugin
     * @param factory   The target ContextPlugin's factory
     * @param settings  The target ContextPlugin's settings
     * @param finalizer An optional finalizer to run at completion (may be null)
     */
    protected synchronized void initializeContextPlugin(final ContextPlugin plug,
                                                        final IContextPluginRuntimeFactory factory, final ContextPluginSettings settings, final Runnable finalizer) {
        Log.d(TAG, "Initialize plug-in: " + plug);
        // Only init if installed
        if (plug.isInstalled()) {
            // Only init if enabled
            if (plug.isEnabled()) {
                // Only init if we have a factory
                if (factory != null) {
                    // Handle pluginMap updating
                    synchronized (pluginMap) {
                        if (!pluginMap.containsKey(plug)) {
                            Log.d(TAG, "Adding a new runtime wrapper for " + plug);
                            pluginMap.put(plug, new ContextPluginRuntimeWrapper());
                        } else {
                            // Plug-in is already in the pluginMap, make sure it's new
                            ContextPluginRuntimeWrapper tmpWrapper = pluginMap.get(plug);
                            if (tmpWrapper.getState() != PluginState.NEW) {
                                Log.w(TAG,
                                        "Ignoring init for existing plug-in " + plug + " with state: "
                                                + tmpWrapper.getState());
                                return;
                            } else {
                                Log.d(TAG,
                                        "Found existing runtime wrapper for " + plug + " with state: "
                                                + tmpWrapper.getState());
                            }
                        }
                    }
                    // Setup the plug-in's runtime thread
                    final ContextPluginRuntimeWrapper pluginWrapper = pluginMap.get(plug);
                    PluginLooperThread t = null;
                    synchronized (threadMap) {
                        Log.d(TAG, "Creating runtime thread for " + plug);
                        if (threadMap.containsKey(plug)) {
                            Log.e(TAG, "NEW plug-in already had thread... this should not happen. Plugin: " + plug);
                            // PluginLooperThread oldThread = threadMap.remove(plug);
                            // oldThread.quit();
                        } else {
                            // Register the plugin's looper thread
                            t = registerLooperThreadForPlug(plug);
                        }
                    }
                    /*
                     * Create the plug-in's runtime using its Looper thread
					 */
                    t.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Set initializing state
                            pluginWrapper.setState(PluginState.INITIALIZING);
                            /*
                             * For security, ensure that the incoming IContextPluginRuntimeFactory is implemented by
							 * org.ambientdynamix.api.contextplugin.ContextPluginRuntimeFactory
							 */
                            if (factory instanceof org.ambientdynamix.api.contextplugin.ContextPluginRuntimeFactory) {
                                // Create the new ContextPluginRuntime using the incoming factory
                                ContextPluginRuntime runtime = null;
                                try {
                                    runtime = factory.makeContextPluginRuntime(plug, ContextManager.this,
                                            new SimpleEventHandler(), UUID.randomUUID());
                                } catch (Exception e1) {
                                    Log.w(TAG, "Exception during makeContextPluginRuntime: " + e1);
                                    runtime = null; // Make sure the runtime is null so the method exits
                                }
                                // Make sure we got a runtime
                                if (runtime != null) {
                                    // Setup the pluginWrapper using the runtime
                                    pluginWrapper.setContextPluginRuntime(runtime);
									/*
									 * Initialize the ContextPluginRuntime using the event handler, power scheme and
									 * settings.
									 */
                                    try {
                                        Log.v(TAG, "Initializing runtime for: " + plug);
                                        // Try to initialize the runtime
                                        runtime.init(scheme, settings);
                                        // Init succeeded, so setup INITIALIZED state
                                        pluginWrapper.setState(PluginState.INITIALIZED);
                                        // Clear any previous status messages
                                        runtime.clearStatusMessage();
                                        Log.v(TAG, "Runtime is initialized for: " + plug);
										/*
										 * Add ourselves as a context listener... remember, we can only add listeners
										 * after INIT!
										 */
                                        runtime.addContextListener(ContextManager.this);
                                        // Setup plug-in statistics
                                        synchronized (statMap) {
                                            PluginStats stats = statMap.get(plug);
                                            if (stats == null) {
                                                // Need new PluginStats
                                                stats = new PluginStats(plug, 20);
                                                statMap.put(plug, stats);
                                            }
                                        }
                                        Log.d(TAG, "Checking for pending stop...");
                                        // Check for a pending stop request
                                        if (pendingPluginStop.keySet().contains(plug)) {
                                            Boolean destroy = pendingPluginStop.remove(plug);
                                            stopPlugin(plug, true, destroy);
                                        } else {
                                            Log.d(TAG, "Checking for start...");
                                            // Start the plugin (startPlugin checks for proper start state)
                                            startPlugin(plug);
                                        }
                                        // Run the finalizer, if specified
                                        if (finalizer != null)
                                            finalizer.run();
                                    } catch (Exception e) {
                                        // Error initializing the plugin. Clean up.
                                        Log.w(TAG, "Problem initializing plug-in " + plug + " | exception was " + e);
                                        pluginWrapper.setState(PluginState.ERROR);
                                        runtime.setStatusMessage(e.toString());
                                        // Remove any pending stop requests, since we'll disable the plug-in
                                        if (pendingPluginStop.keySet().contains(plug)) {
                                            Boolean destroy = pendingPluginStop.remove(plug);
                                        }
                                        // Disable plug-in...
                                        disablePluginOnError(plug, "Plugin could not be initialized and was disabled: "
                                                + plug, true);
                                    }
                                } else {
                                    Log.w(TAG, "ContextPluginRuntime was null after factory creation!");
                                    pluginWrapper.setState(PluginState.ERROR);
                                    disablePluginOnError(plug, "Plugin could not be initialized and was disabled: "
                                            + plug, true);
                                }
                            } else {
                                Log.w(TAG,
                                        "factory not an instanceof org.ambientdynamix.api.contextplugin.ContextPluginRuntimeFactory");
                                pluginWrapper.setState(PluginState.ERROR);
                                disablePluginOnError(plug, "Plugin could not be initialized and was disabled: " + plug,
                                        true);
                            }
                        }
                    });
                } else {
                    if (DynamixService.isFrameworkStarted())
                        Log.w(TAG, "factory was NULL in initializeContextPlugin");
                    else
                        Log.d(TAG, "initializeContextPlugin called when Dynamix is disabled, ignoring request for "
                                + plug);
                }
            } else
                Log.d(TAG, "Ignoring initializeContextPlugin request for disabled plug-in: " + plug);
        } else
            Log.d(TAG, "Ignoring initializeContextPlugin for uninstalled plug-in: " + plug);
    }

    /**
     * Returns true if the context type is supported by the currently installed plug-ins; false otherwise.
     */
    protected boolean isContextTypeSupported(String contextType) {
        synchronized (pluginMap) {
            for (ContextPlugin plug : pluginMap.keySet()) {
                if (plug.supportsContextType(contextType))
                    return true;
            }
        }
        return false;
    }

    /**
     * Adds context support to an app using any ContextPlugins supporting the requested contextDataType. Note that this
     * method will only return valid ContextPlugins, meaning that ContextPlugins with installation problems will not be
     * returned.
     *
     * @param app         the application requesting context support.
     * @param contextType A String representing the desired context support type. Returns a List of ContextPlugins enlisted to
     *                    provide the requested support, or an empty List if no context support is available.
     */
    protected synchronized List<ContextSupport> addContextSupport(DynamixApplication app, IDynamixListener listener,
                                                                  String contextType, String pluginId) {
        // Create an empty list of supporting plugins to return
        List<ContextSupport> supportInfo = new Vector<>();
        // Grab the DynamixSession for the app
        DynamixSession session = SessionManager.getSession(app);
        // Create a combined list of plug-ins from the plugMap and available plug-ins (no duplicates!)
        List<ContextPlugin> completePluginList = new ArrayList<>();
        // First, add all installed plug-ins
        completePluginList.addAll(DynamixService.getInstalledContextPlugins());
        // Next, check the pluginMap for any stragglers (new, installing, etc).
        synchronized (pluginMap) {
            for (ContextPlugin plug : pluginMap.keySet())
                if (!completePluginList.contains(plug))
                    completePluginList.add(plug);
        }
		/*
		 * Scan through the completePluginList looking for plug-ins that support the specified contextType.
		 */
        for (ContextPlugin plug : completePluginList) {
            // If a pluginId is specified, only setup support for that particular plug-in
            if (pluginId != null)
                if (!plug.getId().equalsIgnoreCase(pluginId))
                    break;
			/*
			 * Check if the ContextPlugin supports the requested contextDataType. It's ok to setup context support for
			 * installing plug-ins.
			 */
            if (plug.supportsContextType(contextType)) {
                // Update the ContextPlugin's session with the requested context support
                if (session != null) {
                    // Add the new ContextSupport to the session
                    ContextSupport sub = new ContextSupport(session, listener, plug, contextType);
                    if (session.addContextSupport(listener, sub)) {
                        // Add the support to the list returned to the caller
                        supportInfo.add(sub);
						/*
						 * Call startPlugin to ensure the plug-in is started, since we added context support
						 */
                        startPlugin(plug);
                    }
                } else
                    Log.w(TAG, "Dynamix Service could not find an DynamixSession for: " + app);
            }
        }
//		/*
//		 * Try to auto-install supporting plugins if we haven't found any and the app is allowed to install plug-ins.
//		 */
//        if (supportInfo.isEmpty()) {
//            // Check if auto-install is allowed
////            if (DynamixPreferences.autoContextPluginInstallEnabled(context)) {
////                Log.d(TAG, "addContextSupport did not find support for context type: " + contextType);
////                Log.d(TAG, "Checking for context support in available updates... ");
////                List<ContextPlugin> installPlugs = new ArrayList<>();
////                // Check through the previously discovered plug-ins that have not yet been installed
////                for (PluginDiscoveryResult discoveryResult : UpdateManager.getNewContextPlugins()) {
////                    // Check if the plug-in supports the specified context type
////                    if (discoveryResult.getDiscoveredPlugin().getContextPlugin().supportsContextType(contextType)) {
////                        Log.i(TAG, discoveryResult.getDiscoveredPlugin().getContextPlugin()
////                                + " supports context type: " + contextType);
////                        // Set PENDING_INSTALL so that add context support calls work properly
////                        discoveryResult.getDiscoveredPlugin().getContextPlugin()
////                                .setInstallStatus(PluginInstallStatus.PENDING_INSTALL);
////                        // Add the plug-in to the install list
////                        installPlugs.add(discoveryResult.getDiscoveredPlugin().getContextPlugin());
////                        // Also, create a ContextSupport
////                        ContextSupport sub = new ContextSupport(session, listener, discoveryResult
////                                .getDiscoveredPlugin().getContextPlugin(), contextType);
////                        // Add the support to the session
////                        if (session.addContextSupport(listener, sub)) {
////                            // Add the support to the list returned to the caller
////                            supportInfo.add(sub);
////                        }
////                    }
////                }
////				/*
////				 * Install the discovered plug-ins.
////				 */
////                DynamixService.installPlugins(installPlugs, null);
////            } else {
////                //Log.w(TAG, "Not installing context support because auto context plug-in install is disabled");
////            }
//        }
        return supportInfo;
    }

    /**
     * Removes the context support for the specified app and listener. This method stops the associated plug-in, if the
     * plug-in has no more listeners to support (to conserve power).
     */
    public synchronized Result removeContextSupport(DynamixApplication app, IDynamixListener listener,
                                                    ContextSupportInfo supportInfo) {
        DynamixSession session = SessionManager.getSession(app);
        if (session != null) {
            ContextSupport sub = session.getContextSupport(supportInfo);
            if (sub != null) {
                ContextPlugin plug = sub.getContextPlugin();
                Result r = session.removeContextSupport(listener, sub, true);
                if (r.wasSuccessful()) {
					/*
					 * Call stopPlugin if there are no more context support registrations for the plug-in
					 */
                    if (SessionManager.getContextSupportCount(plug) == 0) {
                        Log.d(TAG, "Stopping plug-in because it has no more context support registrations: " + plug);
                        stopPlugin(plug, false, false);
                    }
                }
                return r;
            } else {
                return new Result("Context Support Not Found", ErrorCodes.NO_CONTEXT_SUPPORT);
            }
        } else {
            Log.w(TAG, "could not find open session for: " + app);
            return new Result("Session Not found", ErrorCodes.SESSION_NOT_FOUND);
        }
    }

    /**
     * Removes all context support for the specified listener.
     */
    public synchronized Result removeAllContextSupport(DynamixApplication app, IDynamixListener listener) {
        DynamixSession session = SessionManager.getSession(app);
        if (session != null) {
            List<ContextSupport> subList = session.getContextSupport(listener);
            for (ContextSupport sub : subList) {
                removeContextSupport(app, listener, sub.getContextSupportInfo());
            }
            return new Result();
        } else {
            Log.w(TAG, "could not find open session for: " + app);
            return new Result("Session Not found", ErrorCodes.SESSION_NOT_FOUND);
        }
    }

    /**
     * Removes all context support for the specified app (all listeners).
     */
    public synchronized Result removeAllContextSupport(DynamixApplication app) {
        DynamixSession session = SessionManager.getSession(app);
        if (session != null) {
            List<ContextSupport> subList = session.getAllContextSupport();
            for (ContextSupport sub : subList) {
                removeContextSupport(app, sub.getDynamixListener(), sub.getContextSupportInfo());
            }
            return new Result();
        } else {
            Log.w(TAG, "could not find open session for: " + app);
            return new Result("Session Not found", ErrorCodes.SESSION_NOT_FOUND);
        }
    }

    /**
     * Removes all context support of a particular context type for the specified listener.
     */
    public synchronized Result removeContextSupportForContextType(DynamixApplication app, IDynamixListener listener,
                                                                  String contextType) {
        DynamixSession session = SessionManager.getSession(app);
        if (session != null) {
            List<ContextSupport> subList = session.getContextSupport(listener);
            boolean foundSub = false;
            for (ContextSupport sub : subList) {
                if (sub.getContextType().equalsIgnoreCase(contextType)) {
                    removeContextSupport(app, listener, sub.getContextSupportInfo());
                    foundSub = true;
                }
            }
            if (foundSub)
                return new Result();
            else
                return new Result("Context Support Not Found", ErrorCodes.NO_CONTEXT_SUPPORT);
        } else {
            Log.w(TAG, "could not find open session for: " + app);
            return new Result("Session Not found", ErrorCodes.SESSION_NOT_FOUND);
        }
    }

    /**
     * Adds the specified ContextPlugin without its associated IContextPluginRuntimeFactory, which is used to create and
     * initialize the ContextPlugin. This method adds an EmptyContextPluginRuntime as a placeholder until a
     * IContextPluginRuntimeFactory can be provided (usually after a dynamic install). When the
     * IContextPluginRuntimeFactory is finally available, use the initializeContextPlugin method to update the plugin
     * with its associated IContextPluginRuntimeFactory.
     *
     * @param plug The new ContextPlugin to add.
     */
    protected boolean addNewContextPlugin(ContextPlugin plug) {
        Log.d(TAG, "addNewContextPlugin for: " + plug + " with InstallStatus: " + plug.getInstallStatus());
        synchronized (pluginMap) {
            // Check if the plugin's runtime is already being managed (i.e., it's listed in the pluginMap)
            if (!pluginMap.containsKey(plug)) {
				/*
				 * Add an EmptyContextPluginRuntime to the pluginMap to serve as a place-holder while the plug-in's
				 * Bundle is being installed.
				 */
                EmptyContextPluginRuntime rt = new EmptyContextPluginRuntime();
                rt.setParentPlugin(plug);
                pluginMap.put(plug, new ContextPluginRuntimeWrapper(rt, PluginState.NEW));
                return true;
            } else
                Log.w(TAG, "addNewContextPlugin found existing plugin: " + plug);
        }
        return false;
    }

    /**
     * Checks if the applications specified in the appMap are still alive (i.e. have valid IBinders), pinging each if
     * they are. If an application is not alive, it is added to a dead application list, which is processed during
     * 'postProcess'. Post processing removes calls 'removeContextListener' on the ContextManager for all discovered
     * dead apps.
     *
     * @param app The apps to check for liveliness
     */
    protected void checkAppLiveliness(DynamixApplication app) {
        SessionManager.sendEventCommand(app, new CheckAppLiveliness());
    }

    /**
     * Returns an immutable list of all ContextPlugins currently managed by the ContextManager.
     */
    protected List<ContextPlugin> getAllContextPlugins() {
        return new ArrayList<>(Collections.unmodifiableSet((this.pluginMap.keySet())));
    }

    /**
     * Returns the ContextPlugin associated with the incoming pluginId.
     *
     * @param pluginId The string identifier of a ContextPlugin Returns the ContextPlugin associated with the incoming
     *                 pluginid
     */
    protected ContextPlugin getContextPlugin(String pluginId) {
        synchronized (pluginMap) {
            for (ContextPlugin plug : pluginMap.keySet()) {
                if (plug.getId().equalsIgnoreCase(pluginId))
                    return plug;
            }
        }
        return null;
    }

    /**
     * Returns the ContextPluginRuntime associated with the incoming ContextPlugin's pluginId.
     *
     * @param pluginId The string identifier of a ContextPlugin.
     */
    protected ContextPluginRuntimeWrapper getContextPluginRuntime(String pluginId) {
        synchronized (pluginMap) {
            for (ContextPlugin plug : pluginMap.keySet()) {
                if (plug.getId().equalsIgnoreCase(pluginId))
                    return pluginMap.get(plug);
            }
        }
        return null;
    }

    /**
     * Returns a ContextSupportResult for the incoming app and listener.
     *
     * @param app      The requesting app.
     * @param listener The listener to return context support for.
     */
    protected ContextSupportResult getContextSupport(DynamixApplication app, IDynamixListener listener) {
        // Grab the cached app from Dynamix
        DynamixSession session = SessionManager.getSession(app);
        List<ContextSupportInfo> subList = new Vector<>();
        if (session != null) {
            List<ContextSupport> subs = session.getContextSupport(listener);
            if (subs != null)
                for (ContextSupport sub : subs) {
                    subList.add(sub.getContextSupportInfo());
                }
        }
        return new ContextSupportResult(subList);
    }

    /**
     * Registers the specified Activity as belonging to the ContextPluginRuntime. Used to programmatically close the
     * Activity later.
     *
     * @param runtime  The ContextPluginRuntime.
     * @param activity The associated Activity.
     */
    protected void registerConfigurationActivity(ContextPluginRuntime runtime, Activity activity) {
        Log.v(TAG, "registerConfigurationActivity for " + runtime);
        ContextPlugin plug = getContextPlugin(runtime.getSessionId());
        if (plug != null) {
            if (configActivityMap.containsKey(plug)) {
                // Close the current activity, since there's already another activity running
                Log.w(TAG, "Exiting existing configuration activity... closing it");
                Activity act = configActivityMap.remove(plug);
                act.finish();
            } else {
                // Store the activity
                configActivityMap.put(plug, activity);
            }
        } else
            Log.w(TAG, "registerConfigurationActivity could not find a plugin for: " + runtime);
    }

    /**
     * Registers the specified Activity as belonging to the ContextPluginRuntime. Used to programmatically close the
     * Activity later.
     *
     * @param runtime  The ContextPluginRuntime.
     * @param activity The associated Activity.
     */
    protected void registerContextAcquisitionActivity(ContextPluginRuntime runtime, Activity activity) {
        Log.v(TAG, "registerContextAcquisitionActivity for " + runtime);
        ContextPlugin plug = getContextPlugin(runtime.getSessionId());
        if (plug != null) {
            synchronized (acquisitionActivityMap) {
                // Check if the runtime is already bound to an Activity
                if (!acquisitionActivityMap.containsKey(plug)) {
                    acquisitionActivityMap.put(plug, activity);
                } else {
					/*
					 * The runtime is already bound to an Activity. Log warnings.
					 */
                    Activity existing = acquisitionActivityMap.get(runtime);
                    if (existing != null) {
                        if (existing.equals(activity))
                            Log.d(TAG, runtime + " is already bound to context acquisition activity: " + activity);
                        else
                            Log.w(TAG, runtime + " is bound, but not to context acquisition activity: " + activity);
                    } else
                        Log.w(TAG, "registerContextAcquisitionActivity could not find existing Activity for: "
                                + runtime);
                }
            }
        } else
            Log.w(TAG, "registerContextAcquisitionActivity could not find a plugin for: " + runtime);
    }

    /**
     * Unregisters the specified context acquisition Activity from the runtime.
     *
     * @param runtime The runtime unregister.
     */
    protected void unregisterContextAcquisitionActivity(ContextPluginRuntime runtime) {
        Log.d(TAG, "unregisterContextAcquisitionActivity for " + runtime);
        if (runtime != null) {
            ContextPlugin plug = getContextPlugin(runtime.getSessionId());
            if (plug != null) {
                Activity a = acquisitionActivityMap.remove(plug);
                if (a != null)
                    Log.d(TAG, "Unregistered context acquisition activity for " + runtime);
                else
                    Log.d(TAG, "Could not find context acquisition activity... probably closed by " + runtime);
            } else
                Log.w(TAG, "unregisterContextAcquisitionActivity could not find a plugin for: " + runtime);
        } else
            Log.w(TAG, "unregisterContextAcquisitionActivity received null runtime");
    }

    /**
     * Unregisters the specified configuration Activity from the runtime.
     *
     * @param runtime The runtime to unregister.
     */
    protected void unRegisterConfigurationActivity(ContextPluginRuntime runtime) {
        Log.d(TAG, "unRegisterConfigurationActivity for " + runtime);
        if (runtime != null) {
            ContextPlugin plug = getContextPlugin(runtime.getSessionId());
            if (plug != null) {
                Activity a = configActivityMap.remove(plug);
                if (a != null)
                    Log.d(TAG, "Unregistered plugin configuration activity for " + runtime);
                else
                    Log.d(TAG, "Could not find plugin configuration activity... probably closed by " + runtime);
            } else
                Log.w(TAG, "unRegisterConfigurationActivity could not find a plugin for: " + runtime);
        } else
            Log.w(TAG, "unRegisterConfigurationActivity received null runtime");
    }

    /**
     * Registers a new request UUID for the specified DynamixApplication and IDynamixListener.
     */
    protected UUID registerRequestUUID(DynamixApplication app, IDynamixListener listener, ContextPlugin plug) {
        synchronized (requestMap) {
            Vector<ContextRequest> request = new Vector<>();
            request.add(new ContextRequest(app, listener, plug));
            UUID id = UUID.randomUUID();
            requestMap.put(id, request);
            return id;
        }
    }

    /**
     * Removes all events for a given IDynamixListener and contextType, regardless of expiration time.
     */
    protected void removeCachedContextEvents(IDynamixListener listener, String contextType) {
        contextCache.removeContextEvents(listener, contextType);
    }

    /**
     * Removes a previously added ContextPlugin from ContextManager handling.
     *
     * @param plug The ContextPlugin to remove.
     */
    protected synchronized void removeContextPlugin(ContextPlugin plug) {
        stopPlugin(plug, true, true);
    }

    /**
     * Replaces the originalPlug with the newPlug. Removes previously cached events for the originalPlug, but maintains
     * the originalPlug's context support registrations. The method calls 'addContextPlugin' using the newPlug, which
     * installs the newPlug with an EmptyContextPluginRuntime. Once the newPlug's Bundle is available, the
     * 'initializeContextPlugin' method should be called to install the plug-in's factory and complete the install
     * process.
     *
     * @return True if the original plug-in was replaced by the new plug-in; false otherwise.
     */
    protected boolean replaceContextPlugin(ContextPlugin originalPlug, ContextPlugin newPlug) {
        Log.d(TAG, "Updating " + originalPlug + " with: " + newPlug);
        if (originalPlug.isInstalled()) {
            // Remove any of the existing ContextPlugin's events that might remain in the contextCache
            Log.d(TAG, "Removing cached events for: " + originalPlug);
            contextCache.removeContextEvents(originalPlug);
            // Remove management for originalPlug
            stopPlugin(originalPlug, true, true);
            // Add the newPlug
            return addNewContextPlugin(newPlug);
        } else
            Log.w(TAG, "Original plugin was not installed: " + originalPlug);
        return false;
    }

    /**
     * Resends all cached SecuredEvents in the contextCache to the specified IDynamixListener.
     */
    protected void resendCachedEvents(DynamixApplication app, IDynamixListener listener) {
        doResendCachedEvents(app, listener, null, -1);
    }

    /**
     * Resends the cached SecuredEvents in the contextCache to the specified IDynamixListener that have occurred in the
     * specified number of previousMills.
     */
    protected void resendCachedEvents(DynamixApplication app, IDynamixListener listener, int previousMills) {
        doResendCachedEvents(app, listener, null, previousMills);
    }

    /**
     * Resends all cached SecuredEvents (of type contextType) in the contextCache to the specified IDynamixListener.
     */
    protected void resendCachedEvents(DynamixApplication app, IDynamixListener listener, String contextType) {
        doResendCachedEvents(app, listener, contextType, -1);
    }

    /**
     * Resends the cached SecuredEvents (of type contextType) in the contextCache to the specified IDynamixListener that
     * have occurred in the specified number of previousMills.
     */
    protected void resendCachedEvents(DynamixApplication app, IDynamixListener listener, String contextType,
                                      int previousMills) {
        doResendCachedEvents(app, listener, contextType, previousMills);
    }

    /**
     * Changes the ContextManager's PowerScheme to the specified newScheme. This method also tells each dependent
     * ContextPlugin to use the new PowerScheme.
     *
     * @param newScheme The new PowerScheme to use.
     */
    protected synchronized void setPowerScheme(PowerScheme newScheme) {
        // Remember the original PowerScheme for later state handling.
        PowerScheme originalScheme = scheme;
        // Save the new PowerScheme.
        scheme = newScheme;
        // Tell each ContextPluginRuntime to use the new PowerScheme
        for (ContextPluginRuntimeWrapper runtime : pluginMap.values()) {
            try {
                runtime.getContextPluginRuntime().setPowerScheme(newScheme);
            } catch (Exception e) {
                Log.w(TAG, runtime + " threw an exception during setPowerScheme: " + e.toString());
            }
        }
        // Restart if we were PowerScheme.MANUAL
        if (scheme != PowerScheme.MANUAL && originalScheme == PowerScheme.MANUAL)
            startContextManager();
    }

    /**
     * Starts context handling for all ContextPlugins.
     */
    protected synchronized void startContextManager() {
        synchronized (startState) {
            if (startState == StartState.STOPPED || startState == StartState.PAUSED) {
                Log.d(TAG, "ContextManager starting! PluginMap count: " + pluginMap.size());
                startState = StartState.STARTING;
                // Start our contextCache
                contextCache.start();
                // Start our plug-ins
                synchronized (pluginMap) {
                    if (pluginMap.isEmpty()) {
                        // No plug-ins to start
                        synchronized (startState) {
                            startState = StartState.STARTED;
                        }
                    } else {
                        // Start each ContextPlugin we're managing.
                        for (ContextPlugin plug : pluginMap.keySet()) {
                            startPlugin(plug);
                        }
                    }
                }
            } else
                Log.w(TAG, "Cannot start context manager from state: " + startState);
        }
    }

    /**
     * Start the specified ContextPlugin.
     *
     * @param plug The ContextPlugin to start.
     */
    protected synchronized boolean startPlugin(final ContextPlugin plug) {
        //Log.d(TAG, "startPlugin for: " + plug);
        // Only start the ContextPlugin if the ContextManager is started or starting
        if (startState == StartState.STARTED || startState == StartState.STARTING) {
            // Make sure the plug-in has at least one context support registration
            if (SessionManager.getContextSupportCount(plug) > 0) {
                // Access the plug-in's wrapper
                final ContextPluginRuntimeWrapper wrapper = pluginMap.get(plug);
                if (wrapper != null) {
                    // Access the plug-in's runtime
                    final ContextPluginRuntime runtime = wrapper.getContextPluginRuntime();
                    if (runtime != null) {
                        // Only start the ContextPlugin if it's enabled
                        if (plug.isEnabled()) {
                            // Only start if the ContextPlugin configured
                            if (!plug.isConfigured()) {
                                Log.w(TAG, "Cannot start unconfigured plugin: " + plug);
                            } else {
                                // Handle start based on the wrapper's state
                                if (wrapper.getState() == PluginState.STARTING) {
                                    Log.d(TAG, "Ignoring start since the ContextPlugin is starting");
                                    return true;
                                } else if (wrapper.getState() == PluginState.STARTED) {
                                    Log.d(TAG, "Ignoring start since the ContextPlugin was already started");
                                    return true;
                                } else if (wrapper.getState() == PluginState.INITIALIZED
                                        || wrapper.getState() == PluginState.ERROR
                                    //|| wrapper.getState() == PluginState.NEW //smartsantander
                                        ) {
                                    Log.d(TAG, "Starting: " + runtime);
                                    // Set STARTING state
                                    wrapper.setState(PluginState.STARTING);
                                    final PluginLooperThread t = threadMap.get(plug);
                                    if (t != null) {
                                        t.handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
													/*
													 * Set a default exception handler to catch any weird problems from
													 * the runtime.
													 */
                                                    t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                                                        @Override
                                                        public void uncaughtException(Thread thread, Throwable ex) {
                                                            Log.e(TAG, "ContextPluginRuntime uncaughtException: " + ex);
                                                            for (PluginLooperThread looperThread : threadMap.values()) {
                                                                if (thread.equals(looperThread)) {
                                                                    ContextPluginRuntimeWrapper problemWrapper = pluginMap
                                                                            .get(looperThread.getContextPlugin());
                                                                    problemWrapper.setState(PluginState.ERROR);
                                                                    disablePluginOnError(
                                                                            looperThread.getContextPlugin(),
                                                                            looperThread.getContextPlugin()
                                                                                    + " caused an error and was disabled",
                                                                            true);
                                                                    return;
                                                                }
                                                            }
                                                            Log.w(TAG, "Could not find problem plug-in for exception: "
                                                                    + ex);
                                                        }
                                                    });
                                                    // Check for a pending stop request
                                                    if (pendingPluginStop.keySet().contains(plug)) {
                                                        Boolean destroy = pendingPluginStop.remove(plug);
                                                        stopPlugin(plug, true, destroy);
                                                    } else {
														/*
														 * We need to set STARTED on the wrapper since the
														 * 'runtime.start()' method may block.
														 */
                                                        wrapper.setState(PluginState.STARTED);
                                                        updateManagerState();
														/*
														 * Make sure we're still started or starting before starting the
														 * plug-in
														 */
                                                        if (startState == StartState.STARTED
                                                                || startState == StartState.STARTING) {
                                                            Log.d(TAG, "Start executing for: " + runtime);
                                                            wrapper.setExecuting(true);
                                                            runtime.start();
                                                            wrapper.setExecuting(false);
                                                            Log.d(TAG, "Start finished executing for: " + runtime);
                                                        } else
                                                            Log.w(TAG,
                                                                    "ContextManager was stopped before plug-in could be started");
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "ContextPluginRuntime Exception: " + e);
                                                    wrapper.setExecuting(false);
                                                    wrapper.setState(PluginState.ERROR);
                                                    disablePluginOnError(plug, plug
                                                            + " caused an error and was disabled", true);
                                                    updateManagerState();
                                                }
                                            }
                                        });
                                    } else {
                                        Log.w(TAG, "Could not find thread for: " + plug);
                                        // Update state
                                        updateManagerState();
                                        return false;
                                    }
                                    // Update state
                                    updateManagerState();
                                    // Return true
                                    return true;
                                } else
                                    Log.w(TAG, "Cannot start " + plug + " from state: " + wrapper.getState());
                            }
                        } else
                            Log.w(TAG, "Cannot start disabled plugin: " + plug);
                    } else
                        Log.w(TAG, "Could not find runtime for " + plug);
                } else
                    Log.w(TAG, "Could not find wrapper for: " + plug);
            } else {
                Log.d(TAG, "Not starting " + plug + " because it has no context support registrations");
            }
        } else
            Log.w(TAG, "Cannot start " + plug + " when the context manager is in state " + startState);
        // Update date
        updateManagerState();
        // Return false
        return false;
    }

    /*
     * Updates the ContextManagers StartState based on the plug-ins in the pluginMap.
     */
    private synchronized void updateManagerState() {
        synchronized (startState) {
            // We have to sync on pluginMap since there may be multiple PluginStoppers running simultaneously.
            synchronized (pluginMap) {
				/*
				 * Handle stopping
				 */
                if (startState == StartState.STOPPING) {
                    // When stopping, we only set started to false when ALL plugins have been removed.
                    if (pluginMap.isEmpty()) {
                        startState = StartState.STOPPED;
                    } else {
                        for (ContextPlugin plug : pluginMap.keySet())
                            Log.d(TAG, "Waiting for plug-in to stop: " + plug);
                    }
                }
				/*
				 * Handle pausing
				 */
                if (startState == StartState.PAUSING) {
                    if (pluginMap.isEmpty())
                        startState = StartState.PAUSED;
                    else {
                        boolean allInitialized = true;
                        for (ContextPluginRuntimeWrapper wrapper : pluginMap.values()) {
                            // Check for pause state on the plug-in
                            if (wrapper.getState() == PluginState.INITIALIZING
                                    || wrapper.getState() == PluginState.STARTING
                                    || wrapper.getState() == PluginState.STARTED
                                    || wrapper.getState() == PluginState.STOPPING) {
                                allInitialized = false;
                                if (wrapper.getContextPluginRuntime() != null
                                        && wrapper.getContextPluginRuntime().getParentPlugin() != null) {
                                    Log.d(TAG, "Waiting for plug-in to stop (for pause): "
                                            + wrapper.getContextPluginRuntime().getParentPlugin() + ", which is in state "
                                            + wrapper.getState());
                                }
                            }
                        }
                        if (allInitialized) {
                            startState = StartState.PAUSED;
                        }
                    }
                }
				/*
				 * Handle starting
				 */
                if (startState == StartState.STARTING) {
                    // Assume we're started at first
                    boolean started = true;
                    for (ContextPlugin plug : pluginMap.keySet()) {
                        // We only consider enabled plug-ins when starting
                        if (plug.isEnabled()) {
                            // Check for configured status
                            if (plug.isConfigured()) {
                                ContextPluginRuntimeWrapper runtime = pluginMap.get(plug);
                                // Make sure the plug-in does not have an error
                                if (runtime.getState() != PluginState.ERROR) {
									/*
									 * We only consider plug-ins that have at least one context support registration.
									 * Otherwise, they are not started to conserve power.
									 */
                                    if (SessionManager.getContextSupportCount(plug) > 0)
                                        // Check if the plug-in is already started
                                        if (runtime.getState() != PluginState.STARTED) {
                                            Log.d(TAG, "Waiting for plug-in to start: " + plug);
                                            // We have a plug-in that has not started yet
                                            started = false;
                                            break;
                                        }
                                }
                            }
                        } else
                            Log.d(TAG, "Plug-in is disabled: " + plug);
                    }
                    if (started) {
                        startState = StartState.STARTED;
                    }
                }
            }
        }
    }

    /**
     * Pauses context handling for all ContextPlugins without removing them from management or destroying associated
     * ContextPluginRuntimes. Call startContextHandling to re-start the ContextManager. Call stopContextHandling to
     * completely reset ContextManager state.
     */
    protected synchronized void pauseContextHandling() {
        // Update state
        updateManagerState();
        synchronized (startState) {
            // Only pause if started
            if (startState == StartState.STARTED) {
                Log.d(TAG, "ContextManager is pausing...");
                startState = StartState.PAUSING;
                // Stop each ContextPlugin that we're managing.
                synchronized (pluginMap) {
                    for (ContextPlugin plug : pluginMap.keySet())
                        stopPlugin(plug, false, false);
                }
                // Clear the request cache
                requestMap.clear();
                // Launch the progressMonitorTimer to check for stopped state
                progressMonitorTimer = new Timer();
                progressMonitorTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        checkStopped();
                    }
                }, 0, 500);
            } else
                Log.w(TAG, "Cannot pause context detection from state " + startState);
        }
    }

    /**
     * Stops the ContextManager and removes existing listeners if requested. This method removes all ContextPlugins from
     * the pluginMap and destroys all associated ContextPluginRuntimes. Note: We may be left in an inconsistent state if
     * all plug-ins don't properly stop.
     */
    protected synchronized void stopContextHandling() {
        // Update state
        updateManagerState();
        synchronized (startState) {
            // Only stop if starting, started, or paused
            if (startState == StartState.STARTED || startState == StartState.STARTING
                    || startState == StartState.PAUSED) {
                Log.i(TAG, "ContextManager is stopping... ");
                startState = StartState.STOPPING;
                // Stop the context cache (removes cached events)
                contextCache.stop();
                // Destroy all available plug-ins
                synchronized (pluginMap) {
                    if (pluginMap.values() != null && !pluginMap.isEmpty()) {
                        for (ContextPlugin plug : pluginMap.keySet())
                            stopPlugin(plug, true, true);
                    } else {
                        Log.d(TAG, "No context plug-ins to stop");
                        synchronized (startState) {
                            startState = StartState.STOPPED;
                        }
                    }
                }
                // Clear the request cache
                requestMap.clear();
                // Launch the progressMonitorTimer to check for stopped state
                progressMonitorTimer = new Timer();
                progressMonitorTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        checkStopped();
                    }
                }, 0, 500);
            } else
                Log.w(TAG, "stopContextHandling called when we were not active. State was: " + startState);
        }
    }

    /**
     * Stops the specified ContextPlugin, removing any cached context events and statistics.
     *
     * @param plug The ContextPlugin to stop.
     */
    protected synchronized void stopPlugin(ContextPlugin plug, boolean clearCachedEvents, boolean destroy) {
        Log.d(TAG, "stopPlugin for: " + plug + " with destroy: " + destroy);
        // Access the ContextPlugin's runtime wrapper
        ContextPluginRuntimeWrapper wrapper = pluginMap.get(plug);
        // Make sure we get a wrapper
        if (wrapper != null) {
            // Handle wrapper states
            if (wrapper.getState() == PluginState.NEW) {
				/*
				 * For new plug-ins, just remove from the pluginMap when destroying, since they don't have resources
				 * attached to them.
				 */
                if (destroy)
                    pluginMap.remove(plug);
            } else if (wrapper.getState() == PluginState.STARTED || wrapper.getState() == PluginState.INITIALIZED
                    || wrapper.getState() == PluginState.ERROR) {
                PluginLooperThread t = threadMap.get(plug);
                PlugStopper stopper = new PlugStopper(plug, wrapper, t, context, destroy);
                stopperMap.put(plug, stopper);
                wrapper.setState(PluginState.STOPPING);
                stopper.launch();
            } else if (wrapper.getState() == PluginState.STARTING || wrapper.getState() == PluginState.INITIALIZING) {
                // Clear any existing pending stop request
                pendingPluginStop.remove(plug);
                // Add the new stop request
                pendingPluginStop.put(plug, destroy);
            } else if (wrapper.getState() == PluginState.STOPPING) {
                Log.w(TAG, "Already stopping: " + plug);
                PlugStopper existingStopper = stopperMap.get(plug);
                if (destroy && !existingStopper.destroy) {
                    Log.w(TAG, "Destroy plug-in requested while a non-destructive stop was already in progress for: "
                            + plug);
					/*
					 * We have the situation where an existing PlugStopper is running in "stop" mode, but a new
					 * PlugStopper was requested in "destroy" mode. This is tricky to handle because PlugStoppers set
					 * for "stop" mode automatically reinitialize the plug-in once complete, which is done
					 * asynchronously. In such cases, we set a pending destroy, which is handled after the plug-in
					 * re-initializes when its original PlugStopper completes.
					 */
                    // Clear any existing pending stop request
                    pendingPluginStop.remove(plug);
                    // Add the new stop request
                    pendingPluginStop.put(plug, destroy);
                } else
                    Log.d(TAG, "Already destroying: " + plug);
            } else
                Log.w(TAG, "Cannot stop plugin " + plug + " from state " + wrapper.getState());
            // Remove any cached events from the contextCache for the plugin, if requested
            if (clearCachedEvents)
                contextCache.removeContextEvents(plug);
        } else
            Log.w(TAG, "stopPlugin found NULL runtime for: " + plug + " | " + plug.getInstallStatus());
    }

    /**
     * Unregisters a previously registered request UUID.
     *
     * @param requestId The original request UUID
     * @return True if the requestId was removed; false otherwise.
     */
    protected boolean unregisterRequestUUID(UUID requestId) {
        synchronized (requestMap) {
            return requestMap.remove(requestId) != null;
        }
    }

    /**
     * Updates the specified Context Plug-in with the new Set of Permissions.
     *
     * @param plug        The Context Plug-in to update.
     * @param permissions The complete set of Permissions granted to the Context Plug-in
     * @return True if the permissions were updated; false otherwise.
     */
    protected boolean updateContextPluginPermissions(ContextPlugin plug, Set<Permission> permissions) {
        // Find the plug-in
        ContextPlugin localReference = getContextPlugin(plug.getId());
        if (localReference != null) {
            // Update our local plug-in reference with the new permissions
            localReference.setPermissions(permissions);
            // Update the plug-in's SecuredContext, if available
            SecuredContext sc = securedContextMap.get(plug);
            // SecuredContext can be null if it has not been requested yet by the plug-in
            if (sc != null) {
                sc.updatePermissions(permissions);
            }
            return true;
        } else
            Log.w(TAG, "updateContextPluginPermissions could not find: " + plug);
        return false;
    }

    @Override
    public boolean cancelContextRequestId(UUID sessionID, UUID requestId) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionID);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            // Make sure the requestMap contains the requestId
            if (requestMap.containsKey(requestId)) {
                // Verify that the plug-in is authorized to cancel the request
                boolean authorized = false;
                for (ContextRequest r : requestMap.get(requestId)) {
                    if (r.getPlugin().equals(plug)) {
                        authorized = true;
                        break;
                    }
                }
                // Remove the request, if authorized
                if (authorized) {
                    requestMap.remove(requestId);
                    return true;
                } else {
                    Log.w(TAG, plug + " is not authorized to remove requestId");
                }
            }
        } else
            Log.w(TAG, "cancelContextRequestId received invalid session id");
        return false;
    }

    @Override
    public boolean addNfcListener(UUID sessionID, NfcListener listener) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionID);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            if (listener != null) {
                synchronized (nfcListeners) {
                    if (nfcListeners.containsKey(plug)) {
                        List<NfcListener> listeners = nfcListeners.get(plug);
                        if (!listeners.contains(listener)) {
                            listeners.add(listener);
                        } else
                            Log.w(TAG, "NfcListener already registered for: " + plug);
                    } else {
                        Vector<NfcListener> listeners = new Vector<>();
                        listeners.add(listener);
                        nfcListeners.put(plug, listeners);
                    }
                }
            } else
                Log.w(TAG, "addNfcListener received null listener from " + plug);
        }
        return false;
    }

    @Override
    public boolean removeNfcListener(UUID sessionID, NfcListener listener) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionID);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            synchronized (nfcListeners) {
                if (nfcListeners.containsKey(plug)) {
                    List<NfcListener> listeners = nfcListeners.get(plug);
                    if (listeners.contains(listener)) {
                        return listeners.remove(listener);
                    } else
                        Log.w(TAG, "Could not find specified listener to remove for: " + plug);
                } else
                    Log.w(TAG, "Plugin did not have any listeners registered: " + plug);
            }
        }
        return false;
    }

    /**
     * Sends an notification of a PluginAlert. This method is not finished.
     */
    @Override
    public boolean sendPluginAlert(UUID sessionID, PluginAlert alert) {
        // Get the ContextPlugin using the secure sessionId
        ContextPlugin plug = getContextPlugin(sessionID);
        // If we get a plug, we have a valid UUID, so continue
        if (plug != null) {
            Toast.makeText(context, alert.getAlertMessage(), Toast.LENGTH_LONG);
            if (alert.getRequestedSettingsActivity() != null) {
                String requestedActivity = alert.getRequestedSettingsActivity();
                try {
                    Field f = Settings.class.getDeclaredField(requestedActivity);
                    Log.d(TAG, "Opening Settings Activity Intent: " + requestedActivity);
                    // TODO: Finish this section - for now we do not launch the requested intent
                    // context.startActivity(new Intent(requestedActivity));
                    return true;
                } catch (Exception e) {
                    Log.w(TAG, "Plug-in requested an illegal Settings Activity: " + requestedActivity);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Dispatches a NFC Intent to registered NFC listeners.
     *
     * @param i The NFC Intent.
     */
    protected void dispatchNfcEvent(final Intent i) {
        if (isStarted()) {
            synchronized (nfcListeners) {
                for (ContextPlugin plug : nfcListeners.keySet()) {
                    List<NfcListener> listeners = nfcListeners.get(plug);
                    if (listeners != null) {
                        for (NfcListener listener : listeners) {
                            final NfcListener finalListener = listener;
							/*
							 * Use a daemon thread to send the event to listeners just in case the listener hangs.
							 */
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    finalListener.onNfcEvent(i);
                                }
                            });
                            t.setDaemon(true);
                            t.start();
                        }
                    }
                }
            }
        } else
            Log.w(TAG, "Not dispatching NFC event because we're not started");
    }

    /*
     * Registers a PluginLooperThread with the threadMap for the incoming plug-in.
     */
    private PluginLooperThread registerLooperThreadForPlug(final ContextPlugin plug) {
        synchronized (threadMap) {
            // if(!threadMap.containsKey(plug)){
            PluginLooperThread t = new PluginLooperThread(plug);
            // Set the thread name for performance profiling
            t.setName(plug.getId());
            // Set the plug-in's thread priority
            t.setPriority(getThreadPriorityForPowerScheme());
            // Start the plug-in's thread
            t.start();
            // Wait for the handler to become active
            while (t.handler == null) {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {
                }
            }
            Log.d(TAG, "Registered LooperThread for: " + plug);
            // Update the threadMap with the PluginLooperThread
            PluginLooperThread oldThread = threadMap.put(plug, t);
            if (oldThread != null) {
                Log.w(TAG, "Found existing PluginLooperThread for " + plug);
                oldThread.quit();
            }
			/*
			 * We need the UncaughtExceptionHandler because, if a plug-in is corrupt (e.g., calls methods from old
			 * Dynamix version), Android will throw exceptions that can't be caught below.
			 */
            t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    disablePluginOnError(plug, "PluginLooperThread uncaughtException for: " + plug, true);
                }
            });
            return t;
        }
    }

    /*
     * Called periodically by progressMonitorTimer Timer to check for stopped state (active == false). Active is set to
     * false when state is set by the PlugStoppers Checks if the ContextManager has stopped. Once the ContextManager
     * stops, the method closes any dialog boxes and needed timers.
     */
    private void checkStopped() {
        // Update our startState
        updateManagerState();
        // Handle state
        if (startState == StartState.STOPPED || startState == StartState.PAUSED) {
            stopProgressMonitorTimer();
            closeProgressDialog();
            Log.i(TAG, "ContextManager has stopped");
        } else {
            progressCount++;
            if (progressCount > 50) {
                stopProgressMonitorTimer();
                closeProgressDialog();
                // TODO: throw events to host app when running in embedded mode?
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Utils.showGlobalAlert(DynamixService.getBaseActivity(),
                                "Timeout while stopping plug-ins. Dynamix needs to close.", true);
                    }
                });
            }
            Log.d(TAG, "Waiting for plug-ins to stop... current state is: " + startState);
        }
    }

    /*
     * Closes the progress dialog.
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
            progressDialog = null;
        }
    }

    /*
     * Returns a ContextEvent representing the highest fidelity level appropriate for the incoming
     * SourcedContextDataSet, or null if no ContextEvent can be created (e.g. the app does not have permission).
     */
    private ContextEvent createContextEventForApplication(DynamixApplication app, SourcedContextInfoSet sourcedSet,
                                                          UUID responseId) throws Exception {
        ContextEvent returnEvent = null;
        // Check if this appID is authorized to receive events
        if (DynamixService.SettingsManager.checkApplicationAuthorized(app.getAppID())) {
            // Check if the application is enabled
            if (app.isEnabled()) {
                // Sort the SecureContextData within the sourcedSet into descending order
                Collections.sort(sourcedSet.getSecureContextInfoList(), Collections.reverseOrder());
                SecuredContextInfo allowedContextInfo = null;
                // Admin apps always receive PrivacyRiskLevel.MAX
                if (app.isAdmin()) {
                    PrivacyRiskLevel appMaxFidelity = PrivacyRiskLevel.MAX;
					/*
					 * Find the context data with the highest possible PrivacyRiskLevel for the app's
					 * PluginPrivacySettings
					 */
                    for (SecuredContextInfo securedInfo : sourcedSet.getSecuredContextInfo()) {
                        // Test whether or not the app has permission to receive this event
                        if (appMaxFidelity.compareTo(securedInfo.getPrivacyRisk()) >= 0) {
                            allowedContextInfo = securedInfo;
                            // Break out of the loop since we've found our highest fidelity event (due to sorting)
                            break;
                        }
                    }
                } else {
                    // Find the non-admin app's relevant PluginPrivacySettings
                    for (PluginPrivacySettings privacySettings : app.getPluginPrivacySettings()) {
                        if (privacySettings.getPlugin().equals(sourcedSet.getEventSource())) {
                            // Access the app's max PrivacyRiskLevel and the events PrivacyRiskLevel
                            PrivacyRiskLevel appMaxFidelity = privacySettings.getMaxPrivacyRisk();
							/*
							 * Find the context data with the highest possible PrivacyRiskLevel for the app's
							 * PluginPrivacySettings
							 */
                            for (SecuredContextInfo securedInfo : sourcedSet.getSecuredContextInfo()) {
                                // Test whether or not the app has permission to receive this event
                                if (appMaxFidelity.compareTo(securedInfo.getPrivacyRisk()) >= 0) {
                                    allowedContextInfo = securedInfo;
                                    // Break out of the loop since we've found our highest fidelity event (due to
                                    // sorting)
                                    break;
                                }
                            }
                        }
                    }
                }
                // Setup the event if we found an allowedContextInfo
                if (allowedContextInfo != null) {
                    // Create the event
                    returnEvent = new ContextEvent(allowedContextInfo.getContextInfo(), sourcedSet.getTimestamp(),
                            sourcedSet.getExireMills());
                    // Set auto web encoding state, defaulting to None
                    returnEvent.setNoWebEncoding();
                    if (allowedContextInfo.autoWebEncode())
                        returnEvent.setAutoWebEncode();
                    else if (allowedContextInfo.getWebEncodingFormat() != PluginConstants.NO_WEB_ENCODING) {
                        // Find the matching format in the IContextInfo object
                        for (String format : allowedContextInfo.getContextInfo().getStringRepresentationFormats()) {
                            if (format.toLowerCase().equalsIgnoreCase(returnEvent.getWebEncodingFormat())) {
                                returnEvent.setManualWebEncode(format);
                                break;
                            }
                        }
                    }
					/*
					 * TODO: Streaming setup. This is disabled for now, but we may want to use it for very large events.
					 */
                    if (false) {
                        float threshold = DynamixService.getConfig().getHeapMemoryProtectionThreshold() / 100f;
                        returnEvent.prepStreaming(new StreamController(context, 500, threshold));
                    }
                    // Set the event source
                    returnEvent.setEventSource(sourcedSet.getEventSource().getContextPluginInformation());
					/*
					 * Set the event's target app id and response id. The SessionManager needs this info for error
					 * handling during ContextEvent sending.
					 */
                    returnEvent.setTargetAppId(app.getAppID());
                    if (responseId != null)
                        returnEvent.setResponseId(responseId.toString());
                }
            }
        }
        // Note: this will return null if no appropriate ContextEvent can be made for the app
        return returnEvent;
    }

    /*
     * Utility method for re-sending cached events.
     * @param app The app requesting the events.
     * @param listener The requesting listener
     * @param contextType The type of events to send (or null for all)
     * @param previousMills The time (in milliseconds) to filter events (or -1 for no filter)
     */
    private void doResendCachedEvents(DynamixApplication app, IDynamixListener listener, String contextType,
                                      int previousMills) {
        Log.d(TAG, "doResendCachedEvents for app: " + app + " and listener: " + listener);
        // Iterate over the cached apps
        for (DynamixSession session : SessionManager.getAllSessions()) {
            // Check if the DynamixSession matches our requesting DynamixApplication
            if (session.isSessionOpen() && session.getApp().equals(app)) {
                // Found it... now update the app with cached events
                List<ContextEvent> events = new ArrayList<>();
                List<ContextEventCacheEntry> cacheSnapshot = null;
                if (contextType != null)
                    cacheSnapshot = contextCache.getCachedEvents(contextType);
                else
                    cacheSnapshot = contextCache.getCachedEvents();
                // Process each cached event in the cacheSnapshot, checking if the requesting app should receive it
                for (ContextEventCacheEntry cachedEvent : cacheSnapshot) {
                    if (cachedEvent.hasTargetListener()) {
                        if (cachedEvent.getTargetListener().asBinder().equals(listener.asBinder())) {
                            // The event target's us, so make sure we still hold a context support registration
                            for (ContextSupport sub : session.getContextSupport(listener)) {
                                if (sub.getContextType().equalsIgnoreCase(
                                        cachedEvent.getSourcedContextEventSet().getContextType())) {
                                    // Make sure the current time is not past the event's expiration time
                                    if (previousMills != -1) {
                                        // Get current the system time
                                        Date now = new Date();
                                        if (now.getTime() - previousMills > cachedEvent.getCachedTime().getTime()) {
                                            // Try to create an event for the app
                                            ContextEvent event = null;
                                            try {
                                                event = createContextEventForApplication(app,
                                                        cachedEvent.getSourcedContextEventSet(), cachedEvent
                                                                .getSourcedContextEventSet().getContextInfoSet()
                                                                .getResponseId());
                                            } catch (Exception e) {
                                                Log.w(TAG, "Exception when creating ContextEvent: " + e);
                                                // TODO: Send failed event to app?
                                            }
                                            if (event != null)
                                                events.add(event);
                                        }
                                    } else {
                                        // We hold a context support registration, so try to create the event for the
                                        // receiver
                                        ContextEvent event = null;
                                        try {
                                            event = createContextEventForApplication(app,
                                                    cachedEvent.getSourcedContextEventSet(), cachedEvent
                                                            .getSourcedContextEventSet().getContextInfoSet()
                                                            .getResponseId());
                                        } catch (Exception e) {
                                            Log.w(TAG, "Exception when creating ContextEvent: " + e);
                                            // TODO: Send failed event to app?
                                        }
                                        if (event != null)
                                            events.add(event);
                                    }
                                }
                            }
                        }
                    }
                }
                // Create an empty eventMap for event handling
                Map<IDynamixListener, List<ContextEvent>> eventMap = new HashMap<>();
                // If the SecuredEvent list is not empty, add the events to the eventMap
                if (events.size() > 0) {
                    Log.v(TAG, "Found SecuredEvents for app: Total = " + events.size());
                    eventMap.put(listener, events);
                } else
                    Log.v(TAG, "No cached events found for app");
				/*
				 * Notify the requesting DynamixApplication of the results.
				 */
                SessionManager.notifyContextListeners(eventMap);
                // We're done updating the app, so break out of loop
                break;
            }
        }
    }

    /*
     * Returns the ContextPlugin from the pluginMap for the incoming sessionId.
     * @param sessionId The secure sessionId of the caller Returns the ContextPlugin associated with the sessionId, nor
     * null if no ContextPlugin is found
     */
    private ContextPlugin getContextPlugin(UUID sessionId) {
        synchronized (pluginMap) {
            // Access the plugin runtime using the UUID
            for (ContextPlugin plug : pluginMap.keySet()) {
				/*
				 * NOTE: There may not be a runtime or sessionId at this point, if a plug-in is installing or not
				 * configured.
				 */
                ContextPluginRuntime runtime = pluginMap.get(plug).getContextPluginRuntime();
                if (runtime != null && runtime.getSessionId() != null && runtime.getSessionId().equals(sessionId)) {
                    if (FrameworkConstants.DEBUG)
                        Log.v(TAG, "getContextPlugin for " + sessionId + " found: " + runtime.getParentPlugin());
                    return plug;
                }
            }
            if (FrameworkConstants.DEBUG)
                Log.w(TAG, "No ContextPlugin with a runtime session UUID: " + sessionId);
            return null;
        }
    }

    /*
     * Utility for launching the progress dialog.
     */
    // private void launchProgressDialog(String title, String message) {
    // if (!DynamixService.isEmbedded()) {
    // // Make sure our looper is prepared before showing a ProgressDialog
    // if (Looper.myLooper() == null)
    // Looper.prepare();
    // closeProgressDialog();
    // stopProgressMonitorTimer();
    // if (DynamixService.getBaseActivity() != null)
    // progressDialog = ProgressDialog.show(DynamixService.getBaseActivity(), title, message)
    // }
    // }
	/*
	 * Stops the progress monitor timer.
	 */
    private void stopProgressMonitorTimer() {
        if (progressMonitorTimer != null) {
            progressMonitorTimer.cancel();
            progressMonitorTimer = null;
        }
    }

    /*
     * Utility method for disabling a plug-in after an error.
     */
    private void disablePluginOnError(ContextPlugin plug, String message, boolean destroyPlugin) {
        Log.w(TAG, "disablePluginOnError for " + plug + " with message " + message);
        // Set plug-in disabled
        plug.setEnabled(false);
        // Set the wrapper state to error
        ContextPluginRuntimeWrapper wrapper = pluginMap.get(plug);
        if (wrapper != null)
            wrapper.setState(PluginState.ERROR);
        // Update the plug-ins disabled status using the DynamixService
        DynamixService.updateContextPluginValues(plug, false);
        // Destroy plug-in, if necessary
        if (destroyPlugin)
            stopPlugin(plug, true, true);
        // Notify listeners about the error
        SessionManager.notifyAllContextPluginError(plug, message);
        // Handle UI updates, if not embedded
        if (!DynamixService.isEmbedded()) {
            PluginsActivity.refreshData();
            if (message != null) {
                BaseActivity.toast(message, Toast.LENGTH_LONG);
            }
        }
    }

    private void pluginMethodMonitor(ContextPluginRuntimeWrapper wrapper, TimerTask method, int timeoutMills,
                                     TimerTask timeoutMethod) {
        boolean complete = false;
        if (timeoutMills > 0) {
            Timer methodTimer = new Timer(true);
            methodTimer.schedule(timeoutMethod, timeoutMills);
        }
        Timer methodRunner = new Timer(true);
        methodRunner.schedule(method, 0);
        complete = true;
    }

    /**
     * Private class that handles plug-in stopping and plug-in destroying.
     *
     * @author Darren Carlson
     */
    private class PlugStopper {
        private final String TAG = this.getClass().getSimpleName();
        private final ContextPlugin plug;
        private final ContextPluginRuntimeWrapper runtimeWrapper;
        private final PluginLooperThread thread;
        private final ContextPluginRuntime runtime;
        private final boolean destroy;
        private final Timer timer = new Timer();

        public PlugStopper(ContextPlugin plug, ContextPluginRuntimeWrapper runtimeWrapper, PluginLooperThread thread,
                           Context context, boolean destroy) {
            this.plug = plug;
            this.runtimeWrapper = runtimeWrapper;
            this.thread = thread;
            this.destroy = destroy;
            runtime = runtimeWrapper.getContextPluginRuntime();
            Log.d(TAG, "PlugStopper created for " + plug + " with destroy: " + destroy + " and plug-in state "
                    + runtimeWrapper.getState());
        }

        /*
         * Launch the ThreadStopper, which initiates a timer that checks for stop completion.
         */
        public void launch() {
            Log.d(TAG, "PlugStopper has launched for " + plug + " running on thread " + thread);
            if (thread != null) {
                // Start by nicely asking the plug-in to stop
                Thread t1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (runtime != null) {
                            try {
                                if (destroy) {
                                    Log.d(TAG, "Requesting runtime destroy for: " + plug);
                                    runtimeWrapper.getContextPluginRuntime().destroy();
                                } else {
                                    Log.d(TAG, "Requesting runtime stop for: " + plug);
                                    runtimeWrapper.getContextPluginRuntime().stop();
                                }
                            } catch (Exception e) {
                                String message = plug.getName() + " encountered and exception during stop: " + e;
                                Log.w(TAG, message);
                                disablePluginOnError(plug, message, false);
                                // Set destroy state and cleanup
                                // TODO: This error handling is incomplete... we might still have state hanging
                                // around
                                cleanUp(true);
                            }
                        } else
                            Log.e(TAG, "Runtime was null in PlugStopper for: " + plug);
                    }
                });
				/*
				 * Set an setUncaughtExceptionHandler for handling problems with stop and destroy
				 */
                t1.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable ex) {
                        disablePluginOnError(plug, plug + " caused an error and was disabled", false);
                        cleanUp(true);
                    }
                });
                t1.setDaemon(true);
                t1.start();
                // Create a monitor thread to check the stop progress
                Thread t2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
						/*
						 * Sleep a bit so that plug-ins have a chance to unregister broadcast receivers, etc. Plug-ins
						 * that rely on Android events may not have their start method blocked, hence if we detect
						 * stopped too fast, they may not have a chance to clean up state.
						 */
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        int count = 0;
                        while (runtimeWrapper.isExecuting()) {
                            count++;
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {
                            }
                            if (count > 40)
                                break;
                        }
                        // Check for exit
                        if (runtimeWrapper.isExecuting()) {
                            String message = plug.getName()
                                    + " did not stop in a timely manner... deactivating and interrupting the plug-in";
                            Log.w(TAG, message);
                            // Deactivate plug-in, since it's behaving badly
                            disablePluginOnError(plug, message, false);
                            thread.interrupt();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {
                            }
                            if (runtimeWrapper.isExecuting()) {
                                Log.e(TAG, plug + " did not stop in a timely manner... trying to kill thread");
                                thread.setPriority(Thread.MIN_PRIORITY);
                                // TODO: Perform thread kill here
                            } else
                                cleanUp(false);
                        } else
                            cleanUp(false);
                    }
                });
                t2.setDaemon(true);
                t2.start();
            } else {
                Log.d(TAG, "Plug-in had no runtime thread... it was probably not started: " + plug);
                // Since we don't have a thread to stop, just clean up
                cleanUp(false);
            }
        }

        /*
         * Handles state cleanup for the PlugStopper.
         */
        private void cleanUp(boolean pluginException) {
            Log.d(TAG, "PlugStopper Cleanup for " + plug);
            timer.cancel();
            // Destroy the plugin, if needed
            if (destroy) {
                // Quit the thread
                thread.quit();
                // TODO: Make sure the thread quits properly
                // Remove the plug-in's runtime thread
                threadMap.remove(plug);
                // Remove the context listener
                if (runtime != null)
                    runtime.removeContextListener(ContextManager.this);
                // Remove the plug-in's context support
                SessionManager.removeContextSupportForPlugin(plug, true);
                // Remove the plugin from the pluginMap
                pluginMap.remove(plug);
                // Remove any ncfListeners
                synchronized (nfcListeners) {
                    nfcListeners.remove(plug);
                }
                // Remove any PluginStats
                PluginStats stats = statMap.remove(plug);
                if (stats != null) {
                    stats.clear();
                    stats = null;
                }
                // Remove the plug-ins SecuredContext (if created)
                Log.d(TAG, "Removing SecuredContext for: " + plug);
                synchronized (securedContextMap) {
                    SecuredContext c = securedContextMap.remove(plug);
                    if (c != null) {
                        // Remove all listeners and receivers from the SecureContext
                        c.removeAllListeners();
                    }
                }
                runtimeWrapper.setState(PluginState.DESTROYED);
                Log.d(TAG, "PlugStopper destroyed: " + plug);
            } else {
                if (pluginException)
                    // Set ERROR state
                    runtimeWrapper.setState(PluginState.ERROR);
                else
                    // Set plug-in state back to INITIALIZED
                    runtimeWrapper.setState(PluginState.INITIALIZED);
                Log.d(TAG, "PlugStopper stopped: " + plug);
            }
            // Remove the PlugStopper from the stopperMap
            stopperMap.remove(plug);
            // Update the manager's state
            updateManagerState();
			/*
			 * Process any incoming pendingPluginStop destroy requests that may have come in
			 */
            if (pendingPluginStop.keySet().contains(plug)) {
                Boolean pendingDestroy = pendingPluginStop.remove(plug);
                // If we're not destroying, but a destroy was requested... run the destroy
                if (!destroy && pendingDestroy)
                    stopPlugin(plug, true, pendingDestroy);
            }
        }
    }
}
