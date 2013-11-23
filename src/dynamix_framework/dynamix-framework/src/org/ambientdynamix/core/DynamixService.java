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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;

import org.ambientdynamix.api.application.AppConstants.ContextPluginType;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.ErrorCodes;
import org.ambientdynamix.api.application.IDynamixFacade;
import org.ambientdynamix.api.application.IDynamixListener;
import org.ambientdynamix.api.application.IdResult;
import org.ambientdynamix.api.application.Result;
import org.ambientdynamix.api.application.VersionInfo;
import org.ambientdynamix.api.contextplugin.AutoReactiveInteractiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPlugin;
import org.ambientdynamix.api.contextplugin.ContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.InteractiveContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.PluginConstants;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.ReactiveContextPluginRuntime;
import org.ambientdynamix.core.ContextPluginRuntimeMethodRunners.HandleContextRequest;
import org.ambientdynamix.core.FrameworkConstants.StartState;
import org.ambientdynamix.core.UpdateManager.IContextPluginUpdateListener;
import org.ambientdynamix.core.UpdateManager.IDynamixUpdateListener;
import org.ambientdynamix.data.ContextEventCache;
import org.ambientdynamix.data.DB4oSettingsManager;
import org.ambientdynamix.data.DynamixPreferences;
import org.ambientdynamix.data.FrameworkConfiguration;
import org.ambientdynamix.data.ISettingsManager;
import org.ambientdynamix.event.PluginDiscoveryResult;
import org.ambientdynamix.security.BlockedPrivacyPolicy;
import org.ambientdynamix.security.HighTrustPrivacyPolicy;
import org.ambientdynamix.security.HighestTrustPrivacyPolicy;
import org.ambientdynamix.security.LowTrustPrivacyPolicy;
import org.ambientdynamix.security.MediumTrustPrivacyPolicy;
import org.ambientdynamix.security.PrivacyPolicy;
import org.ambientdynamix.security.TrustedCert;
import org.ambientdynamix.update.DynamixUpdates;
import org.ambientdynamix.update.TrustedCertBinder;
import org.ambientdynamix.update.contextplugin.ContextPluginBinder;
import org.ambientdynamix.update.contextplugin.DiscoveredContextPlugin;
import org.ambientdynamix.update.contextplugin.IContextPluginConnector;
import org.ambientdynamix.update.contextplugin.IContextPluginInstallListener;
import org.ambientdynamix.util.AndroidForeground;
import org.ambientdynamix.util.AndroidNotification;
import org.ambientdynamix.util.ContextPluginRuntimeWrapper;
import org.ambientdynamix.util.Utils;
import org.osgi.framework.ServiceEvent;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.DataStorage;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.ReadingStorage;
import eu.smartsantander.androidExperimentation.operations.Communication;
import eu.smartsantander.androidExperimentation.operations.Demon;
import eu.smartsantander.androidExperimentation.operations.DynamixServiceListenerUtility;
import eu.smartsantander.androidExperimentation.operations.PhoneProfiler;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources.NotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

/**
 * The DynamixService is the primary implementation of the Dynamix Framework on the Android platform. Broadly, Dynamix
 * provides dynamic and secure context acquisition, modeling and provisioning for Android-based devices. Dynamix
 * continually analyzes the hardware platform and software capabilities of the device, dynamically discovering,
 * downloading and integrating appropriate context modeling plugins on-the-fly. Context plugins can be installed from a
 * variety of local or network based repositories. Context plugins are executed within a secure sandbox provided by the
 * OSGi security model and the Dynamix Framework security model. During runtime, ContextPlugins operate in conjunction
 * with an associated ContextPluginRuntime, which does the actual context acquisition and modeling work; generating
 * ContextEvents as new (or updated) IContextInfo are discovered. As new context information is discovered, it is
 * provisioned to authorized Dynamix applications via the Dynamix Application API, which includes the IDynamixFacade
 * interface, IDynamixListener interface, IContextInfo interface and ContextEvent class. Users interact with the
 * DynamixService through its management interface, which consists of a set of Android Activities arranged in a
 * Tab-based layout (see the org.ambientdynamix.android package). In addition, Dynamix uses the Android notification
 * system to alert users of Dynamix events and state changes. Importantly, the Dynamix Service extends Android Service,
 * allowing it to continually serve dependent clients (DynamixApplications) as a background service.
 * <p>
 * the DynamixService uses several sub-systems. First, it utilizes the ContextManager to manage dynamically installable
 * ContextPlugins. Related, the Dynamix Service utilizes the OSGIManager to handle dynamic download, instantiation and
 * runtime management of ContextPluginRuntimes. Interactions with Dynamix applications over the Dynamix Application API
 * are handled by the AppFacadeBinder, which communicates over AIDL to 'external' Dynamix applications that are running
 * within their own process. Context events are cached and managed by the ContextDataCache, which can be used to
 * securely retrieve a list of recent context events. Interactions with the Android notification system are handled by
 * the Dynamix Service and AndroidNotification objects. The SettingsManager provides high-performance, coordinated
 * access to Dynamix Framework settings. The SettingsManager uses Dynamix's persistence layer, which provides support
 * for a variety of underlying database technologies through the ISettingsManager interface.
 * 
 * @see OSGIManager
 * @see ContextManager
 * @see ISettingsManager
 * @see AppFacadeBinder
 * @see ContextEventCache
 * @author Darren Carlson
 */
public final class DynamixService extends Service {
	// Private static data
	private final static String TAG = DynamixService.class.getSimpleName();
	private static FrameworkConfiguration config;
	private static OSGIManager OsgiMgr;
	private static ContextManager ContextMgr;
	private static DynamixService service;
	private static final int PENDING_APP_TAB_ID = 1;
	private static final int UPDATES_TAB_ID = 4;
	private static Context androidContext;
	private static volatile boolean startRequested;
	// private static volatile boolean osgiRunning;
	private static volatile boolean androidServiceRunning;
	// private static volatile boolean showBootProgress;
	private static volatile BootState bootState = BootState.NOT_BOOTED;
	private static volatile StartState startState = StartState.STOPPED;
	private static Activity baseActivity;
	private static UUID frameworkSessionId;
	private static Handler uiHandler = new Handler();
	private static CountDownTimer contextPlugUpdateTimer;
	private static AppFacadeBinder facadeBinder = null;
	private static WebFacadeBinder webFacade = null;
	// Protected static data
	protected static ISettingsManager SettingsManager;
	// Private instance data
	private BroadcastReceiver sleepReceiver;
	private BroadcastReceiver wakeReceiver;
	private Timer appChecker;
	private final static Handler uiHandle = new Handler();
	private AndroidForeground foregroundHandler;
	private static ProgressDialog bootProgress = null;
	private ProgressDialog progressDialog = null;
	private static boolean embeddedMode = false; //smartsantander false->true
	private static ClassLoader embeddedHostClassLoader;
	private static List<IDynamixFrameworkListener> frameworkListeners = new ArrayList<DynamixService.IDynamixFrameworkListener>();
	private static DynamixNotificationManager notificationMgr;
	private static PendingIntent RESTART_INTENT;
	private static String keyStorePath;
	public static Context context;
	//private MyReceiver myReceiver;
	
	
	public static IDynamixListener dynamixCallback;
	public static IDynamixFacade dynamix;	
	public static ServiceConnection sConnection; 
	public static ReadingStorage contextReadings=new ReadingStorage();
	
	
	
	//SmartSantanter	
	private static PhoneProfiler phoneProfiler=new PhoneProfiler();
	private static Boolean isInitialized=false;
	private static Communication communication= new Communication();	
	private static Experiment experiment;	
	private static Boolean connectionStatus=false;
	private static LinkedList<String> experimentMessageQueue=new LinkedList<String>();
	private static DataStorage dataStorage=null;
	private static Demon demon=new Demon();
	public static boolean sessionStarted;
	private static boolean restarting=false;
	private static long totalTimeConnectedOnline=0;
	
	public static long getTotalTimeConnectedOnline(){
		return totalTimeConnectedOnline;
	}
	
	public static void addTotalTimeConnectedOnline(long time){
		totalTimeConnectedOnline+=time;
	}
	
	public static boolean getRestarting(){
		return restarting;
	}
	
	public static void setRestarting(boolean state){
		restarting=state;
	}
	
	public static void initDataStorage(Context cnt){
		dataStorage=new DataStorage(cnt,null,null,1);
	}
	
	public static DataStorage getDataStorage(){
		return dataStorage;
	}
	
	public static Long getDataStorageSize(){
		if (dataStorage!=null)
			return dataStorage.size();
		else
			return 0L;
	}
	
	public static synchronized Pair<Long,String> getOldestExperimentalMessage(){		
		return dataStorage.getMessage();
	}
	
	public static synchronized void deleteExperimentalMessage(Long id){
		dataStorage.deleteMessage(id);		
	}
	
	public static synchronized void addExperimentalMessage(String message){
		if (dataStorage!=null)
			dataStorage.addMessage(message);
		else
			Toast.makeText(context, "Fail to Send of Storage Message:" +message, 5000).show();
	}
	
	public static void cacheExperimentalMessage(String message){		 			
		experimentMessageQueue.addLast(message);		
		if(experimentMessageQueue.size()>10)
			experimentMessageQueue.poll();
	}
	
	
	
	public static String[] getCachedExperimentalMessages(){
		if(experimentMessageQueue.size()==0){ 
			return new String[]{""};
		}
		else return experimentMessageQueue.toArray(new String[experimentMessageQueue.size()]);
	}
	
	public static void setExperiment(Experiment exp){
		if (exp!=null)
			phoneProfiler.experimentPush(exp);
		else
			return;
		
		experiment=exp;	
	}
	
	public static void startExperiment(){
		if (experiment==null) return;		 
		Plugin pluginfo=new Plugin();
		pluginfo.setContextType(experiment.getContextType());
		pluginfo.setDescription(experiment.getContextType());
		pluginfo.setFilename(experiment.getFilename());
		pluginfo.setId(experiment.getId());
		pluginfo.setInstallUrl(experiment.getUrl());
		pluginfo.setName(experiment.getName());
		pluginfo.setRuntimeFactoryClass("org.ambientdynamix.contextplugins.ExperimentPlugin.PluginFactory");
		ContextPluginBinder plugBinder = new ContextPluginBinder();
		ContextPlugin plug;
		try {
			plug = plugBinder.createContextPlugin(DynamixService.getConfig().getPrimaryContextPluginRepo(), pluginfo);
			installPlugin( plug, null);
			Thread.sleep(5000);			
			//DynamixService.OsgiMgr.startPluginBundle(plug);
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
	public static void removeExperiment(){
		try{
			ContextPlugin exp=getInstalledContextPlugin("org.ambientdynamix.contextplugins.ExperimentPlugin");	
			if (exp!=null){
				uninstallPlugin(exp,true);
			}			
		} catch (Exception e) {			
			e.printStackTrace();
		}
		DynamixService.setExperiment(null);
	
	}
	public static Experiment getExperiment(){
		return experiment;
	}
	
 
	static public ReadingStorage getReadingStorage(){
		return contextReadings;
	}
	
	static public PhoneProfiler getPhoneProfiler(){
		return phoneProfiler;
	}
	static public Communication getCommunication(){
		return communication;
	}
	
	
	static public Demon getDemon(){
		return demon;
	}
	
    static public Boolean isEnabled(){
    	if(androidContext==null)return false;
    	return DynamixPreferences.isDynamixEnabled(androidContext);
    }
    
	static public Boolean isDeviceRegistered(){
		if (phoneProfiler.getPhoneId()!=Constants.PHONE_ID_UNITIALIZED){
			return true;
		}else{
			return false;
		}			
	}
	
	static public Boolean isInitialized() {
		if (phoneProfiler.getPhoneId()!=Constants.PHONE_ID_UNITIALIZED && numberOfInstalledPlugins()>0){
			isInitialized=true;
		}else{
			isInitialized=false;
		}
		return isInitialized;
	}

	
	static public int numberOfInstalledPlugins(){
		int counter=0;
		for (ContextPluginInformation plugin :DynamixService.getAllContextPluginInfo()){
			if (plugin.getInstallStatus()==PluginInstallStatus.INSTALLED)
				counter++;
		}
		
		return counter;
	}

	
	static public boolean isExperimentInstalled(String contexttype){
		for (ContextPluginInformation info:DynamixService.getAllContextPluginInfo()){
			if(info.getPluginId().equals(contexttype)){
				return true;
			}
		}
		return false;
	}
 
	/////////////////////////////////
	
	
	static public Handler getUIHandler(){
		return uiHandler;
	}
	
	
	
	// stop bundle 
	public static void stopPlugin(ContextPlugin contextPlugin)
	{
		OsgiMgr.stopPluginBundle(contextPlugin);
	}
	
	/**
	 * Adds a Dynamix Framework listener.
	 */
	public static void addDynamixFrameworkListener(IDynamixFrameworkListener listener) {
		if (listener != null && !frameworkListeners.contains(listener))
			frameworkListeners.add(listener);
	}

	/**
	 * Removes a previously registered Dynamix Framework listener.
	 * 
	 * @param listener
	 */
	public static void removeDynamixFrameworkListener(IDynamixFrameworkListener listener) {
		if (listener != null)
			frameworkListeners.remove(listener);
	}

	/**
	 * Returns the FrameworkConfiguration.
	 */
	public static FrameworkConfiguration getConfig() {
		return config;
	}

	/**
	 * Returns true if Dynamix is running in embedded mode; false otherwise.
	 * 
	 * @return
	 */
	public static boolean isEmbedded() {
	//	return embeddedMode;
		return false;
	}

	/**
	 * Adds the specified application to the set of authorized applications. Note that the incoming app must be present
	 * in the pending applications List. If it is, the application is removed from the pending list and added to the
	 * authorized applications list. This method also clears any notifications generated for the application and
	 * notified the application that it's active. Returns true if the application was authorized; false, otherwise.
	 */
	static boolean authorizeApplication(DynamixApplication app) {
		if (isFrameworkInitialized()) {
			// Authorize the app in the SettingsManager
			if (SettingsManager.authorizePendingApplication(app)) {
				// Update notifications (removes notification icon if all pending applications are handled)
				updateNotifications();
				DynamixSession session = SessionManager.getSession(app);
				if (session != null && session.isSessionOpen()) {
					// Update the session
					if (SessionManager.updateSessionApplication(app)) {
						// Notify that security authorization has been granted
						SessionManager.notifySecurityAuthorizationGranted(app);
						// Notify the application that Dynamix is active
						SessionManager.notifySessionOpened(app, session.getSessionId().toString());
						// Notify the application about active state
						if (isFrameworkStarted())
							SessionManager.notifyAllDynamixFrameworkActive();
						else
							SessionManager.notifyAllDynamixFrameworkInactive();
						return true;
					}
				}
			} else
				Log.w(TAG, "SettingsManager could not authorize: " + app);
		} else
			Log.e(TAG, "Dynamix not initialized during authorizeApplication");
		return false;
	}

	/**
	 * Registers the hosting client's class loader.
	 */
	public static void setEmbeddedHostClassLoader(ClassLoader embeddedHostClassLoader) {
		DynamixService.embeddedHostClassLoader = embeddedHostClassLoader;
	}

	/**
	 * Returns true if there is a registered host class loader; false otherwise.
	 */
	static boolean hasEmbeddedHostClassLoader() {
		return DynamixService.embeddedHostClassLoader != null;
	}

	/**
	 * Returns the hosting client's class loader.
	 */
	static ClassLoader getEmbeddedHostClassLoader() {
		return DynamixService.embeddedHostClassLoader;
	}

	/**
	 * Boots Dynamix in Embedded mode.
	 * 
	 * @param context
	 *            The Android context of the embedding application.
	 * @param embeddedHostClassLoader
	 *            The classloader of the embedding application.
	 * @param config
	 *            The Dynamix Framework configuration to use.
	 * @return True if the boot sequence was started; false otherwise.
	 */
	public static synchronized boolean bootEmbedded(Context context, ClassLoader embeddedHostClassLoader,
			FrameworkConfiguration config) {
		if (bootState == BootState.NOT_BOOTED) {
			embeddedMode = true;
			// In embedded mode, the androidContext is the calling context.
			DynamixService.androidContext = context;
			DynamixService.embeddedHostClassLoader = embeddedHostClassLoader;
			DynamixService.config = config;
			DynamixService.service = new DynamixService();
			DynamixService.service.onCreate();
			boot(context, false, false, true);
			return true;
		} else {
			Log.w(TAG, "Cannot boot Dynamix from state: " + bootState);
			return false;
		}
	}

	/**
	 * Update's the Dynamix FrameworkConfiguration when running in embedded mode.
	 * 
	 * @param config
	 *            The new configuration to use
	 * @return True if the config was updated; false otherwise.
	 */
	public static synchronized boolean updateConfig(FrameworkConfiguration config) {
		if (isEmbedded()) {
			if (config != null) {
				DynamixService.config = config;
				return true;
			} else {
				Log.w(TAG, "Config was null");
				return false;
			}
		} else {
			Log.w(TAG, "Dynamix must be running in embedded mode to update the config");
			return false;
		}
	}

	/**
	 * Boots the Dynamix Framework, which initializes all managers and data structures and prepares Dynamix for use.
	 * This method is asynchronous and returns immediately.
	 */
	static synchronized void boot(Context context, boolean showProgress, boolean bootFromService, boolean embeddedMode) {
		Log.d(TAG, "boot called with Context " + context + " and embedded mode " + embeddedMode + " and boot state "
				+ bootState);
		synchronized (bootState) {
			// Only boot if we're in state NOT_BOOTED
			if (bootState == BootState.NOT_BOOTED) {
				// Set our boot state to booting
				bootState = BootState.BOOTING;
				/*
				 * Start the service (if it's not already running). Note that, if Dynamix crashed, Android will re-start
				 * us using 'onCreate', so the service *will* be running in that case.
				 */
				if (!androidServiceRunning) {
					// Service is not running, so start it
					if (!embeddedMode) {
						// Launch the service using the appContext
						Log.i(TAG, "Starting the Dynamix Service...");
						Context appContext = context.getApplicationContext();
						appContext.startService(new Intent(appContext, DynamixService.class));
					} else {
						// Manually call onCreate, since Android won't because we're running embedded
						service.onCreate();
					}
				}
			} else {
				Log.w(TAG, "boot called when in bootState: " + bootState);
			}
		}
	}

	/**
	 * Starts the WebConnector, which allows web clients to access Dynamix services via its REST interface.
	 */
	public static boolean startWebConnector(int port, int checkPeriodMills, int timeoutMills,
			List<TrustedCert> authorizedCerts) throws IOException {
		if (config.isWebConnectorEnabled()) {
			if (isFrameworkInitialized()) {
				if (!WebConnector.isStarted()) {
					WebConnector.startServer(webFacade, port, checkPeriodMills, timeoutMills, authorizedCerts);
					return true;
				} else {
					Log.d(TAG, "Web connector already started");
					return true;
				}
			} else {
				Log.w(TAG, "Cannot start web connector because Dynamix is not initialized");
				return false;
			}
		} else {
			Log.w(TAG, "Cannot start web connector because it's disabled");
			return false;
		}
	}

	/**
	 * Starts the WebConnector using the values specified in framework configuration.
	 */
	public static boolean startWebConnectorUsingConfigData() {
		for (int port : config.getWebConnectorPorts()) {
			try {
				// loadAuthorizedCertsFromPath(Environment.getExternalStorageDirectory().getAbsolutePath());
				if (startWebConnector(port, config.getWebConnectorTimeoutCheckMills(),
						config.getWebConnectorClientTimeoutMills(), getAuthorizedCertsFromKeyStore()))
					return true;
				else
					return false;
			} catch (IOException e) {
				Log.w(TAG, "Could not start web connector using port: " + port);
			}
		}
		Log.w(TAG, "Failed to start web connector using specified ports: " + config.getWebConnectorPorts());
		return false;
	}

	/*
	 * !!! NOT SECURE !!! Experimental method to load authorized certificates from the inbuilt Dynamix keystore. This is
	 * not yet secure, since plug-ins may be able to access the "trusted_certs" resource and there is no password
	 * protection on the keystore.
	 */
	private synchronized static List<TrustedCert> getAuthorizedCertsFromKeyStore() {
		List<TrustedCert> authorizedCerts = new ArrayList<TrustedCert>();
		try {
			KeyStore trusted = KeyStore.getInstance("BKS");
			InputStream in = new FileInputStream(keyStorePath);
			trusted.load(in, "".toCharArray());
			Enumeration<String> aliases = trusted.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) trusted.getCertificate(alias);
				// Log.i(TAG, "Got Authorized Cert: " + cert);
				authorizedCerts.add(new TrustedCert(alias, cert));
			}
		} catch (KeyStoreException e) {
			Log.w(TAG, e);
		} catch (NotFoundException e) {
			Log.w(TAG, e);
		} catch (NoSuchAlgorithmException e) {
			Log.w(TAG, e);
		} catch (CertificateException e) {
			Log.w(TAG, e);
		} catch (IOException e) {
			Log.w(TAG, e);
		}
		return authorizedCerts;
	}

	/**
	 * Stores the certificate to the Dynamix KeyStore. Updates the WebConnector.
	 * 
	 * @param alias
	 *            The alias to store the cert under.
	 * @param cert
	 *            The certificate to store.
	 * @throws Exception
	 */
	protected synchronized static void storeAuthorizedCert(String alias, X509Certificate cert) throws Exception {
		Log.i(TAG, "Storing authorized cert for " + alias);
		// Load the KeyStore
		KeyStore trusted = KeyStore.getInstance("BKS");
		// trusted.
		// SSLContext context = SSLContext.getInstance("TLS");
		// context.init(km, tm, sr)
		// context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
		InputStream in = new FileInputStream(keyStorePath);
		// http://stackoverflow.com/questions/7245007/runtime-configuration-of-ssl-tls-http-client-on-android-with-client-authenticati
		trusted.load(in, "".toCharArray());
		// Set the cert
		trusted.setCertificateEntry(alias, cert);
		// Store the data
		OutputStream keyStoreStream = new java.io.FileOutputStream(keyStorePath);
		trusted.store(keyStoreStream, "".toCharArray());
		// If the WebConnector is started, update it
		if (WebConnector.isStarted())
			WebConnector.addAuthorizedCert(new TrustedCert(alias, cert));
		/*
		 * NOTE: Store example
		 * https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/mail/store/TrustManagerFactory.java
		 */
	}

	/**
	 * Removes the certificate from the Dynamix KeyStore. Updates the WebConnector.
	 * 
	 * @param alias
	 * @throws Exception
	 */
	protected synchronized static void removeAuthorizedCert(String alias) throws Exception {
		Log.i(TAG, "Removing authorized cert for " + alias);
		// Load the KeyStore
		KeyStore trusted = KeyStore.getInstance("BKS");
		InputStream in = new FileInputStream(keyStorePath);
		trusted.load(in, "".toCharArray());
		// Remember the cert
		X509Certificate cert = (X509Certificate) trusted.getCertificate(alias);
		// Delete the cert
		trusted.deleteEntry(alias);
		// Store the data
		OutputStream keyStoreStream = new java.io.FileOutputStream(keyStorePath);
		trusted.store(keyStoreStream, "".toCharArray());
		// If the WebConnector is started, update it
		if (cert != null && WebConnector.isStarted())
			WebConnector.removeAuthorizedCert(new TrustedCert(alias, cert));
		/*
		 * NOTE: Store example
		 * https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/mail/store/TrustManagerFactory.java
		 */
	}

	/**
	 * Exports the certificate keystore to the root of the device's SD card. This method is used internally for
	 * gathering authorized certificates from browsers.
	 * 
	 * @throws Exception
	 */
	protected static void exportKeyStoreToSDCARD() throws Exception {
		File sourceFile = new File(keyStorePath);
		File destFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
				+ sourceFile.getName());
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	/**
	 * Stops the WebConnector, which prevents web clients from accessing Dynamix services via its REST interface.
	 */
	public static void stopWebConnector() {
		if (isFrameworkInitialized() && WebConnector.isStarted())
			WebConnector.stopServer();
	}

	/**
	 * Sets the time period (in milliseconds) between checks for web client timeouts.
	 */
	protected static void setWebClientTimeoutCheckPeriod(int checkPeriodMills) {
		WebConnector.setWebClientTimeoutCheckPeriod(checkPeriodMills);
	}

	/**
	 * Sets the web client timeout duration (in milliseconds).
	 */
	protected static void setWebClientTimeoutMills(int timeoutMills) {
		WebConnector.setWebClientTimeoutMills(timeoutMills);
	}

	/**
	 * Cancels a previously started Bundle installation for the specified ContextPlugin.
	 * 
	 * @return True if the installation was cancelled; false otherwise.
	 */
	public static boolean cancelInstallation(ContextPlugin plug) {
		return OsgiMgr.cancelInstallation(plug);
	}

	/**
	 * Updates the enabled status of the application, notifying the application of the change.
	 * 
	 * @param app
	 *            The application to enable or disable.
	 * @param enabled
	 *            True to enable a DynamixApplication; false to disable it.
	 */
	static void changeApplicationEnabled(DynamixApplication app, boolean enabled) {
		Log.d(TAG, "changeApplicationEnabled for " + app + " enabled == " + enabled);
		// Set the application to the requested enabled state
		app.setEnabled(enabled);
		// Update the application
		if (updateApplication(app)) {
			// If Dynamix is running, notify the application of the change
			DynamixSession session = SessionManager.getSession(app);
			if (session != null) {
				if (enabled) {
					SessionManager.notifySessionOpened(app, session.getSessionId().toString());
					// Notify the application about active state
					if (isFrameworkStarted())
						SessionManager.notifyAllDynamixFrameworkActive();
					else
						SessionManager.notifyAllDynamixFrameworkInactive();
				} else
					SessionManager.notifySessionClosed(app);
			}
		}
	}

	/**
	 * Returns true if the specified application is connected to Dynamix and not timed out; false otherwise.
	 */
	static boolean checkConnected(DynamixApplication app) {
		if (isFrameworkStarted()) {
			// Check each app in the session
			for (DynamixSession session : SessionManager.getAllSessions()) {
				if (session.isSessionOpen())
					if (session.getApp().equals(app))
						return session.getApp().isConnected();
			}
		}
		return false;
	}

	/**
	 * Clears the specified ContextPlugin's statistics.
	 * 
	 * @param plug
	 *            The Plugin to clear.
	 * @return True if the stats were cleared; false otherwise.
	 */
	public static boolean clearPluginStats(ContextPlugin plug) {
		if (ContextMgr != null) {
			return ContextMgr.clearPluginStats(plug);
		}
		return false;
	}

	/**
	 * Removes the listener using the Facade.
	 */
	static void removeDynamixListener(IDynamixListener listener) {
		try {
			facadeBinder.removeDynamixListener(listener);
		} catch (RemoteException e) {
			Log.w(TAG, "Could not remove listener: " + e.toString());
		}
	}

	/**
	 * Returns the DynamixApplication bound to the specified uid, or null if the application was not found. Note that
	 * this
	 */
	static DynamixApplication getDynamixApplicationByUid(int uid) {
		return SettingsManager.getDynamixApplication(uid);
	}

	/**
	 * Returns the Dynamix Framework VersionInfo
	 */
	public static VersionInfo getFrameworkVersion() {
		return FrameworkConstants.DYNAMIX_VERSION;
	}

	/**
	 * Returns the PluginStats for the incoming plugin id.
	 */
	public static PluginStats getPluginStats(ContextPlugin plug) {
		if (ContextMgr != null)
			return ContextMgr.getPluginStats(plug);
		else
			return null;
	}

	/**
	 * Installs the specified context plug-in update, notifying the listener of the progress (if provided).
	 * 
	 * @param update
	 *            The UpdateResult to install.
	 * @param listener
	 *            The listener to update with progress reports, or null.
	 */
	static void installContextPluginUpdate(PluginDiscoveryResult update, IContextPluginInstallListener listener) {
		if (update.hasUpdateTarget()) {
			Log.i(TAG, "Updating: " + update.getTargetPlugin());
			// Grab the originalPlug
			ContextPlugin originalPlug = update.getTargetPlugin();
			// Grab the originalPlug's existing settings (if there are any)
			ContextPluginSettings originalSettings = SettingsManager.getContextPluginSettings(originalPlug);
			/*
			 * Update the originalPlug's Bundle. This will fire off a threaded BundleInstaller that will call the
			 * DynamixService back using 'handleBundleUpdated' when the install completes.
			 */
			OsgiMgr.updatePluginBundle(originalPlug, originalSettings, update.getDiscoveredPlugin().getContextPlugin(),
					listener);
		} else
			Log.w(TAG, "No target plugin to update!");
	}

	/**
	 * Installs the specified context plug-in updates, notifying the listener of the progress (if provided).
	 * 
	 * @param update
	 *            The Set of UpdateResults to install.
	 * @param listener
	 *            The listener to update with progress reports, or null.
	 */
	static void installContextPluginUpdates(Set<PluginDiscoveryResult> updates, IContextPluginInstallListener listener) {
		for (PluginDiscoveryResult update : updates)
			installContextPluginUpdate(update, listener);
	}

	/**
	 * Installs a new plug-in.
	 * 
	 * @param plug
	 *            The ContextPlugin to install.
	 * @param listener
	 *            The listener to update with progress reports, or null.
	 */
	static void installPlugin(ContextPlugin plug, IContextPluginInstallListener listener) {
		List<ContextPlugin> plugs = new Vector<ContextPlugin>();
		plugs.add(plug);
		installPlugins(plugs, listener);
	}

	/**
	 * Installs a List of new plug-ins.
	 * 
	 * @param contextPlugin
	 *            The ContextPlugins to install.
	 * @param listener
	 *            The listener to update with progress reports, or null.
	 */
	synchronized static void installPlugins(List<ContextPlugin> plugs, IContextPluginInstallListener listener) {
		for (ContextPlugin plug : plugs) {
			Log.d(TAG, "Install plug-in " + plug + " with install state " + plug.getInstallStatus());
			if (!plug.isInstalled()) {
				// Use the OsgiMgr to install the plugin's OSGi Bundle
				if (OsgiMgr.installBundle(plug, listener)) {
					// Add the ContextPlugin to the ContextManager along with its runtime factory
					ContextPluginSettings settings = SettingsManager.getContextPluginSettings(plug);
					ContextMgr.initializeContextPlugin(plug, OsgiMgr.getContextPluginRuntimeFactory(plug), settings,
							null);
				} else {
					/*
					 * The Bundle was not yet available in the OSGi manager, so add the ContextPlugin to the
					 * ContextManager without its runtime factory. The runtime will be added to the context manager once
					 * the bundle is installed - see 'handleBundleInstalled'
					 */
					ContextMgr.addNewContextPlugin(plug);  
				}
			} else
				Log.w(TAG, "installPlugins called for previously installed plug-in: " + plug);
		}
	}

	/**
	 * Reinitializes the plug-in by calling ContextMgr.addContextPluginRuntimeFactory. This method should only be called
	 * for plug-ins that have already been installed.
	 */
	synchronized static void reInitializePlugin(ContextPlugin plug) {
		if (OsgiMgr.isBundleInstalled(plug)) {
			// Add the ContextPlugin to the ContextManager along with its runtime factory
			ContextPluginSettings settings = SettingsManager.getContextPluginSettings(plug);
			ContextMgr.initializeContextPlugin(plug, OsgiMgr.getContextPluginRuntimeFactory(plug), settings, null);
		} else
			Log.e(TAG, "reInitializePlugin called on plug-in that did not have it's Bundle installed: " + plug);
	}

	/**
	 * Returns true if the Dynamix Framework is initialized; false otherwise.
	 */
	public static boolean isFrameworkInitialized() {
		synchronized (bootState) {
			return bootState == BootState.BOOTED;
		}
	}

	/**
	 * Returns true if the Dynamix Framework is initialized; false otherwise.
	 */
	public static boolean isFrameworkStarted() {
		synchronized (startState) {
			return startState == StartState.STARTED;
		}
	}

	/**
	 * Enables the plug-in described by the ContextPluginInformation.
	 * 
	 * @param plugInfo
	 *            The ContextPluginInformation for the plug-in to enable.
	 * @return True if the request was accepted; false otherwise.
	 */
	public static boolean enableContextPlugin(ContextPluginInformation plugInfo) {
		ContextPlugin plug = getInstalledContextPlugin(plugInfo);
		if (plug != null) {
			plug.setEnabled(true);
			updateContextPluginValues(plug, true);
			return true;
		} else {
			Log.w(TAG, "Could not find plug-in for: " + plugInfo);
			return false;
		}
	}

	/**
	 * Disables the plug-in described by the ContextPluginInformation.
	 * 
	 * @param plugInfo
	 *            The ContextPluginInformation for the plug-in to disable.
	 * @return True if the request was accepted; false otherwise.
	 */
	public static boolean disableContextPlugin(ContextPluginInformation plugInfo) {
		ContextPlugin plug = getInstalledContextPlugin(plugInfo);
		if (plug != null) {
			plug.setEnabled(false);
			updateContextPluginValues(plug, true);
			return true;
		} else {
			Log.w(TAG, "Could not find plug-in for: " + plugInfo);
			return false;
		}
	}

	/**
	 * Indicates that the Dynamix plugin status update process has failed.
	 * 
	 * @param message
	 *            A message describing the failure reasons
	 */
	static void onPluginStatusUpdateFailure(String message) {
		Log.w(TAG, "onPluginStatusUpdateFailure: " + message);
		// TODO: Handle event?
	}

	/**
	 * Revokes the application's security authorization, removing all context support, closing its session, and removing
	 * its entry from the SettingsManager.
	 */
	static Result revokeSecurityAuthorization(DynamixApplication app) {
		if (app != null) {
			Log.i(TAG, "revokeSecurityAuthorization for: " + app);
			if (SettingsManager.removeApplication(app)) {
				// Notify the app that their security authorization has been revoked
				SessionManager.notifySecurityAuthorizationRevoked(app);
				ContextMgr.removeAllContextSupport(app);
				return SessionManager.closeSession(app, true);
			} else {
				Log.w(TAG, "revokeSecurityAuthorization could not find app in SettingsManager: " + app);
				return new Result("revokeSecurityAuthorization could not find app in SettingsManager: " + app,
						ErrorCodes.DYNAMIX_FRAMEWORK_ERROR);
			}
		} else {
			Log.w(TAG, "app was null in removeApplication");
			return new Result("app was null in revokeSecurityAuthorization", ErrorCodes.DYNAMIX_FRAMEWORK_ERROR);
		}
	}

	/**
	 * Removes the pending application from the SettingsManager. Returns true if successful; false otherwise.
	 */
	static boolean removePendingApplication(DynamixApplication app) {
		if (SettingsManager.getPendingApplications().contains(app))
			return SettingsManager.removeApplication(app);
		return false;
	}

	/**
	 * Dispatches the incoming NFC Intent to registered plug-ins.
	 * 
	 * @param i
	 *            The NFC Intent.
	 */
	static void dispatchNfcEvent(final Intent i) {
		if (ContextMgr != null) {
			ContextMgr.dispatchNfcEvent(i);
		}
	}

	/**
	 * Sets the new PowerScheme.
	 */
	public static void setNewPowerScheme(PowerScheme scheme) {
		if (bootState == BootState.BOOTED) {
			SettingsManager.setPowerScheme(scheme);
			ContextMgr.setPowerScheme(scheme);
		} else
			Log.w(TAG, "Dynamix has not booted yet... please wait!");
	}

	/**
	 * Starts the Dynamix Framework if it's already initialized and Dynamix is enabled; otherwise, it caches the start
	 * request so that Dynamix will start after the boot process completes. If this method is called before Dynamix is
	 * initialized, the start request is cached and handled after initialization.
	 */
	public static boolean startFramework() {
		// Make sure we're initialized
		if (isFrameworkInitialized()) {
			// Make sure we're stopped
			if (!isFrameworkStarted()) {
				service.doStartFramework();
				return true;
			} else
				Log.w(TAG, "Cannot start framework while in state: " + startState);
		} else {
			// Cache the boot request
			Log.i(TAG, "startFramework called while Dynamix is not yet initialized... start request cached");
			startRequested = true;
			return true;
		}
		return false;
	}

	/**
	 * Stops the Dynamix Framework, keeping the service alive and retaining listeners.
	 */
	public static boolean stopFramework() {
		// Make sure we're initialized
		if (isFrameworkInitialized())
			// Make sure we're started
			if (isFrameworkStarted()) {
				// Stop the framework
				service.doStopFramework(false, false, false);
				return true;
			} else
				Log.w(TAG, "Cannot stop framework while in state: " + startState);
		else
			Log.w(TAG, "DynamixService not initialized!");
		return false;
	}

	/**
	 * Destroys the Dynamix Framework, killing the service (if necessary) and removing all listeners.
	 */
	public static boolean destroyFramework(boolean killProcess, boolean restartProcess) {
		// Make sure we're initialized
		if (isFrameworkInitialized()) {
			// Destroy the framework
			service.doStopFramework(true, killProcess, restartProcess);
			return true;
		} else
			Log.w(TAG, "DynamixService not initialized!");
		return false;
	}

	/**
	 * Uninstalls the specified ContextPlugin, removing its Dynamix settings and underlying OSGi Bundle. If Dynamix is
	 * running, the ContextPlugin is immediately stopped before the uninstall continues. Other ContextPlugins continue
	 * to operate normally during this process.
	 */
	static boolean uninstallPlugin(ContextPlugin plug, boolean notifyListeners) {
		Log.i(TAG, "Uninstalling " + plug);
		/*
		 * Remove the plugin from the Context Manager, which remove its context support registrations and destroys the
		 * plugin.
		 */
		ContextMgr.removeContextPlugin(plug);
		/*
		 * Remove the plugin from our settings. Note: We need to do this BEFORE removing the plugin or its OSGi Bundle
		 * because the database may rely on classes contained within the Bundle's class-loader.
		 */
		SettingsManager.removeContextPlugin(plug);
		// Remove the plug-in's settings, if present
		SettingsManager.removeContextPluginSettings(plug);
		// Remove the plug-in's OSGi Bundle
		OsgiMgr.uninstallBundle(plug);
		// Inform all listeners that a plugin was uninstalled
		if (notifyListeners) {
			SessionManager.notifyAllContextPluginUninstalled(plug);
		}
		// Refresh UI
		if (!embeddedMode) {
			UpdatesActivity.refreshData();
			HomeActivity.refreshData();
		}
		return true;
	}

	/**
	 * Updates the specified ContextPlugin with the incoming ContextPlugin. If handleStateChanges is true, this method
	 * automatically starts or stops the plug-in; if not, this method simply updates the SettingsManager only and will
	 * not start or stop the plug-in. Returns true if the ContextPlugin was updated; false otherwise.
	 */
	static boolean updateContextPluginValues(ContextPlugin plug, boolean handleStateChanges) {
		if (SettingsManager.updateContextPlugin(plug)) {
			// If we're handling state changes, and the plug-in is enabled, start it; otherwise stop it
			if (handleStateChanges) {
				if (plug.isEnabled())
					ContextMgr.startPlugin(plug);
				else
					ContextMgr.stopPlugin(plug, true, false);
			}
			return true;
		} else
			Log.w(TAG, "Could not updateContextPlugin for " + plug);
		return false;
	}

	/**
	 * Refreshes the list of plug-ins known by Dynamix using the configured repositories. Used internally by Dynamix for
	 * automatic plug-in discovery. Can also be called by clients that embed Dynamix to trigger context plug-in
	 * discovery.
	 */
	static public boolean checkForContextPluginUpdates() {
		if (isFrameworkInitialized()) {
			UpdateManager.checkForContextPluginUpdates(getAndroidContext(), UpdateManager.getContextPluginSources(),
					PluginConstants.PLATFORM.ANDROID, Utils.getAndroidVersionInfo(), getFrameworkVersion(),
					new ContextPluginCallbackHandler(null, ContextPluginCallbackHandler.Mode.UPDATE), androidContext
							.getPackageManager().getSystemAvailableFeatures());
			return true;
		} else
			Log.w(TAG, "Can't check for context plug-in updates, since Dynamix has not booted");
		return false;
	}

	/**
	 * Asynchronously checks for context plugin UPDATES using the UpdateManager, notifying the specified
	 * IUpdateStatusListener with results (or errors). Typically used by Dynamix UI for displaying results.
	 * 
	 * @param handler
	 *            The IUpdateStatusListener to notify with results (or errors).
	 */
	static void checkForContextPluginUpdates(IContextPluginUpdateListener callback) {
		if (isFrameworkInitialized()) {
			UpdateManager.checkForContextPluginUpdates(getAndroidContext(), UpdateManager.getContextPluginSources(),
					PluginConstants.PLATFORM.ANDROID, Utils.getAndroidVersionInfo(), getFrameworkVersion(),
					new ContextPluginCallbackHandler(callback, ContextPluginCallbackHandler.Mode.UPDATE),
					androidContext.getPackageManager().getSystemAvailableFeatures());
		}
	}

	/**
	 * Asynchronously checks for NEW context plugins using the UpdateManager, notifying the specified
	 * IUpdateStatusListener with results (or errors). Typically used by Dynamix UI for displaying results.
	 * 
	 * @param handler
	 *            The IUpdateStatusListener to notify with results (or errors)
	 */
	static void checkForNewContextPlugins(IContextPluginUpdateListener callback) {
		if (isFrameworkInitialized()) {
			UpdateManager.checkForContextPluginUpdates(getAndroidContext(), UpdateManager.getContextPluginSources(),
					PluginConstants.PLATFORM.ANDROID, Utils.getAndroidVersionInfo(), getFrameworkVersion(),
					new ContextPluginCallbackHandler(callback, ContextPluginCallbackHandler.Mode.NEW), androidContext
							.getPackageManager().getSystemAvailableFeatures());
		}
	}

	/**
	 * Returns a List of all installed ContextPlugins from the SettingsManager (as as List of ContextPlugin)
	 */
	static List<ContextPlugin> getInstalledContextPlugins() {
		return SettingsManager.getInstalledContextPlugins();
	}

	/**
	 * Returns the ContextPlugin associated with the incoming ContextPluginInformation using the SettingsManager.
	 */
	static ContextPlugin getInstalledContextPlugin(ContextPluginInformation plugInfo) {
		List<ContextPlugin> plugs = getInstalledContextPlugins();
		for (ContextPlugin plug : plugs)
			if (plug.getContextPluginInformation().equals(plugInfo))
				return plug;
		return null;
	}

	/**
	 * Returns a List of all installed ContextPlugins from the SettingsManager (as as List of ContextPluginInformation).
	 */
	static List<ContextPluginInformation> getInstalledContextPluginInfo() {
		List<ContextPluginInformation> plugInfoList = new ArrayList<ContextPluginInformation>();
		List<ContextPlugin> installedPlugs = getInstalledContextPlugins();
		for (ContextPlugin installedPlug : installedPlugs)
			plugInfoList.add(installedPlug.getContextPluginInformation());
		return plugInfoList;
	}

	/**
	 * Returns a List of all pending ContextPlugins from the SettingsManager (as as List of ContextPluginInformation).
	 */
	static List<ContextPluginInformation> getPendingContextPluginInfo() {
		List<ContextPluginInformation> plugInfoList = new ArrayList<ContextPluginInformation>();
		List<DiscoveredContextPlugin> discoveredPendingPlugs = SettingsManager.getPendingContextPlugins();
		for (DiscoveredContextPlugin discovered : discoveredPendingPlugs)
			plugInfoList.add(discovered.getContextPlugin().getContextPluginInformation());
		return plugInfoList;
	}

	/**
	 * Returns a List of both installed and pending ContextPlugins from the SettingsManager (as as List of
	 * ContextPluginInformation).
	 */
	public static List<ContextPluginInformation> getAllContextPluginInfo() {
		List<ContextPluginInformation> plugInfoList = new ArrayList<ContextPluginInformation>();
		plugInfoList.addAll(getInstalledContextPluginInfo());
		plugInfoList.addAll(getPendingContextPluginInfo());
		return plugInfoList;
	}

	/**
	 * Returns the list of persisted DiscoveredPlugins that have been previously discovered and stored in the
	 * SettingsManager.
	 */
	static List<DiscoveredContextPlugin> getPendingContextPlugins() {
		return SettingsManager.getPendingContextPlugins();
	}

	/**
	 * Returns the Dynamix base Android Activity, or null if none is set (e.g., if we started from auto-boot).
	 */
	static Activity getBaseActivity() {
		return baseActivity;
	}

	/**
	 * Returns a FrameworkConfiguration based on a text-based Dynamix Framework configuration file who's path is derived
	 * from the incoming Android Context. The configuration file must adhere to the Dynamix Framework configuration
	 * specification outlined in the Dynamix developer documentation. Note that if the config file does not exist, the
	 * default config file will be copied from Dynamix's res/raw directory.
	 * 
	 * @param context
	 *            The Android Context of the Dynamix Framework
	 * @return A DynamixConfiguration
	 * @throws Exception
	 *             Detailed information about configuration file errors
	 */
	static FrameworkConfiguration createFrameworkConfigurationFromPropsFile(Context context) throws Exception {
		Log.i(TAG, "Creating FrameworkConfiguration...");
		// Get the Dynamix data path
		String dataPath = Utils.getDataDirectoryPath(context);
		// Ensure the 'conf' directory exists in the dataPath
		File dstPath = new File(dataPath + "conf");
		dstPath.mkdirs();
		// Create the configFile using the dstPath
		File configFile = new File(dstPath, "dynamix.conf");
		// Copy the default config file from Dynamix's packaged resources,
		// if the file does not already exist
		if (!configFile.exists()) {
			InputStream in = context.getResources().openRawResource(R.raw.dynamix);
			OutputStream out = new FileOutputStream(configFile);
			// Transfer bytes from in to out
			byte[] buf = new byte[8192];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
		return FrameworkConfiguration.createFromPropsFile(configFile.getCanonicalPath());
	}

	/**
	 * Extracts the Dynamix KeyFile to disk.
	 * 
	 * @param context
	 * @throws Exception
	 */
	static void initKeyStore(Context context) throws Exception {
		Log.i(TAG, "Initializing KeyStore...");
		// Get the Dynamix data path
		String dataPath = Utils.getDataDirectoryPath(context);
		// Ensure the 'conf' directory exists in the dataPath
		File dstPath = new File(dataPath + "conf");
		dstPath.mkdirs();
		// Check if the keystore exists
		// Create the configFile using the dstPath
		File keystoreFile = new File(dstPath, "trusted_webconnector_certs.bks");
		keyStorePath = keystoreFile.getAbsolutePath();
		// Copy the default config file from Dynamix's packaged resources,
		// if the file does not already exist
		if (!keystoreFile.exists()) {
			InputStream in = context.getResources().openRawResource(R.raw.trusted_webconnector_certs);
			OutputStream out = new FileOutputStream(keystoreFile);
			// Transfer bytes from in to out
			byte[] buf = new byte[8192];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
	}

	/**
	 * Returns the ClassLoader for the specified ContextPlugin.
	 */
	static ClassLoader getContextPluginClassLoader(ContextPlugin plug) {
		return OsgiMgr.getContextPluginClassLoader(plug);
	}

	/**
	 * Returns the Dynamix Framework's current session id.
	 */
	static UUID getFrameworkSessionId() {
		return frameworkSessionId;
	}

	/**
	 * Returns the ContextPlugin associated with the incoming pluginId, or null if the ContextPlugin is not found. Uses
	 * the ContextManager, so Dynamix must be running in order to call this method and get results.
	 */
	static ContextPlugin getInstalledContextPlugin(String pluginId) {
		return ContextMgr.getContextPlugin(pluginId);
	}

	/**
	 * Returns a List of the currently installed PrivacyPolicy entities. TODO: Update this. This method currently
	 * Returns a static set of test policies.
	 */
	static List<PrivacyPolicy> getPrivacyPolicies() {
		ArrayList<PrivacyPolicy> list = new ArrayList<PrivacyPolicy>();
		list.add(new HighestTrustPrivacyPolicy());
		list.add(new HighTrustPrivacyPolicy());
		list.add(new MediumTrustPrivacyPolicy());
		list.add(new LowTrustPrivacyPolicy());
		list.add(new BlockedPrivacyPolicy());
		return list;
	}

	/**
	 * Callback for bundle installed events from the OSGIManager. This method handles dynamically installed plugins.
	 * 
	 * @param plug
	 *            The ContextPlugin whoes bundle was installed
	 */
	static synchronized void handleBundleInstalled(final ContextPlugin plug) {
		// Make sure the plugin is valid and installed
		if (plug != null && plug.isInstalled()) {
			// Update our SettingsManager (updates privacy policies automatically)
			if (SettingsManager.addContextPlugin(plug)) {
				// Update the apps with the version from the database (updates in memory privacy policies)
				SessionManager.refreshApps();
				/*
				 * Refresh the UI, but do it first, since an error in the init code below will leave the UI's
				 * plug-ins in the wrong state. We need a long-term way to deal with UI plug-ins.
				 */
				if (!embeddedMode) {
					PluginsActivity.refreshData();
					HomeActivity.refreshData();
				}
				// Try to access settings for the plug-in
				ContextPluginSettings settings = SettingsManager.getContextPluginSettings(plug);
				// Add the plugin's runtime to the Context Manager for management
				ContextMgr.initializeContextPlugin(plug, OsgiMgr.getContextPluginRuntimeFactory(plug), settings,
						new Runnable() {
							@Override
							public void run() {
								// Update all application listeners that a new plugin is available
								SessionManager.notifyAllNewContextPluginInstalled(plug);
								// Also notify dependent any context subscribers that they now have context support
								for (String contextType : plug.getSupportedContextTypes()) {
									for (DynamixSession session : SessionManager.getAllSessions()) {
										for (ContextSupport sub : session.getContextSupport(contextType)) {
											if (sub.getContextPlugin().equals(plug)) {
												SessionManager.notifyContextSupportAdded(sub.getDynamixApplication(),
														sub.getDynamixListener(), sub.getContextSupportInfo());
											}
										}
									}
								}
							}
						});
			} else
				Log.w(TAG, "Could not addContextPlugin for: " + plug);
		} else
			Log.e(TAG, "handleBundleInstalled received null or uninstalled plug-in: " + plug);
		
	}

	/**
	 * Callback for bundle installed failed events from the OSGIManager.
	 * 
	 * @param plug
	 *            The ContextPlugin that failed to be installed.
	 */
	static void handleBundleInstallError(ContextPlugin plug) {
		Log.w(TAG, "handleBundleInstallError for " + plug);
		uninstallPlugin(plug, false);
	}

	/**
	 * Callback that is called when a ContextPlugin's OSGi Bundle has been updated.
	 * 
	 * @param originalPlug
	 *            The original ContextPlugin
	 * @param originalSettings
	 *            The original ContextPlugin's settings
	 * @param newPlug
	 *            The ContextPlugin that is replacing the original.
	 */
	static synchronized void handleBundleUpdated(final ContextPlugin originalPlug,
			final ContextPluginSettings originalSettings, final ContextPlugin newPlug) {
		// Make sure the newPlug is valid and installed
		if (newPlug != null && newPlug.isInstalled()) {
			// Replace the originalPlug with the newPlug using the SettingsManager
			if (SettingsManager.replaceContextPlugin(originalPlug, newPlug)) {
				// Update the apps with the version from the database (updates in memory privacy policies)
				SessionManager.refreshApps();
				/*
				 * Replace the originalPlug with the newPlug using the ContextMgr (this destroys the originalPlug's
				 * runtime)
				 */
				if (ContextMgr.replaceContextPlugin(originalPlug, newPlug)) {
					// Bind the original settings with the new plug-in
					if (originalSettings != null) {
						/*
						 * TODO: For now, we set configured true, since we received previous settings. There may be an
						 * issue where the new plug-in requires additional settings and would not consider itself
						 * configured.
						 */
						newPlug.setConfigured(true);
						SettingsManager.storeContextPluginSettings(newPlug, originalSettings);
					}
					// Update the ContextPlug'ins factory using the newPlug's IContextPluginRuntimeFactory
					ContextMgr.initializeContextPlugin(newPlug, OsgiMgr.getContextPluginRuntimeFactory(newPlug),
							originalSettings, new Runnable() {
								@Override
								public void run() {
									Log.i(TAG, "ContextPlugin: " + originalPlug + " was updated to: " + newPlug);
									// Remove the originalSettings from the original plug-in
									if (originalSettings != null) {
										SettingsManager.removeContextPluginSettings(originalPlug);
									}
								}
							});
				} else {
					Log.w(TAG, "Could not addContextPlugin to the ContextMgr for: " + newPlug);
				}
			} else
				Log.w(TAG, "Could updateContextPlugin using the ContextMgr for: " + newPlug);
		} else {
			Log.e(TAG, "Could not updating existing plugin in SettingsManager: " + originalPlug);
		}
	}

	/**
	 * Callback for OSGi framework errors.
	 */
	static void onOSGiFrameworkError() {
		Log.w(TAG, "onOSGiFrameworkError!!");
		onDynamixInitializingError("OSGi Error");
		// TODO: Handle this case (need to check when these errors are thrown,
		// i.e. is it always fatal?
	}

	/**
	 * Callback for the OSGi started event. This is called once the OSGi Framework has completely initialized and
	 * started. This method completes the Dynamix boot sequence, if necessary.
	 */
	static void onOSGiFrameworkStarted() {
		Log.d(TAG, "onOSGiFrameworkStarted with bootState: " + bootState);
		synchronized (bootState) {
			if (bootState == BootState.BOOTING) {
				completeBoot();
			} else
				throw new RuntimeException("Received onOSGiFrameworkStarted when not BOOTING");
		}
	}

	/**
	 * Callback for the OSGi stopped event.
	 */
	static void onOSGiFrameworkStopped() {
		Log.d(TAG, "onOSGiFrameworkStopped");
	}

	/**
	 * Registers the specified Activity as belonging to the ContextPluginRuntime. Used to programmatically close the
	 * Activity later.
	 * 
	 * @param runtime
	 *            The ContextPluginRuntime.
	 * @param activity
	 *            The associated Activity.
	 */
	static void registerConfigurationActivity(ContextPluginRuntime runtime, Activity activity) {
		if (ContextMgr != null) {
			ContextMgr.registerConfigurationActivity(runtime, activity);
		}
	}

	/**
	 * Registers the specified Activity as belonging to the ContextPluginRuntime. Used to programmatically close the
	 * Activity later.
	 * 
	 * @param runtime
	 *            The ContextPluginRuntime.
	 * @param activity
	 *            The associated Activity.
	 */
	static void registerContextAcquisitionActivity(ContextPluginRuntime runtime, Activity activity) {
		if (ContextMgr != null) {
			ContextMgr.registerContextAcquisitionActivity(runtime, activity);
		}
	}

	/**
	 * Registers a new request UUID for the specified application and listener.
	 * 
	 * @param app
	 *            the application to register
	 */
	static UUID registerRequestUUID(DynamixApplication app, IDynamixListener listener, ContextPlugin plug) {
		return ContextMgr.registerRequestUUID(app, listener, plug);
	}

	/**
	 * Removes all events for a given IDynamixListener and contextType, regardless of expiration time.
	 */
	static void removeCachedContextEvents(IDynamixListener listener, String contextType) {
		if (ContextMgr != null) {
			ContextMgr.removeCachedContextEvents(listener, contextType);
		}
	}

	/**
	 * Returns the ContextPluginRuntimeWrapper for the specified pluginId; or null if the pluginId can't be found.
	 */
	static ContextPluginRuntimeWrapper getContextPluginRuntime(String pluginId) {
		if (ContextMgr != null)
			return ContextMgr.getContextPluginRuntime(pluginId);
		else
			return null;
	}

	/**
	 * Requests a context interaction for a particular listener using a particular ContextPlugin and contextDataType.
	 * Note that this method can only be called for ContextPluginRuntimes of type IReactiveContextPluginRuntime.
	 * 
	 * @param app
	 *            The app requesting the scan.
	 * @param listener
	 *            The listener to send results to.
	 * @param pluginId
	 *            The plugin to handle the scan.
	 * @param contextType
	 *            The type of context to scan for.
	 * @param contextConfig
	 *            An optional configuration Bundle.
	 * @return An IdResult indicating success of failure.
	 */
	static IdResult handleContextRequest(DynamixApplication app, DynamixSession session, IDynamixListener listener,
			String pluginId, String contextType, Bundle contextConfig) {
		// Make sure Dynamix is started
		if (isFrameworkStarted()) {
			// Get the plug-in
			ContextPlugin plug = DynamixService.getInstalledContextPlugin(pluginId);
			if (plug != null) {
				// Make sure the plug-in is enabled
				if (plug.isEnabled()) {
					// Get the plug-in's runtime
					//ContextMgr.getContextPluginRuntime(pluginId).setExecuting(true); //smartsantander change
					
					ContextPluginRuntime runtime = ContextMgr.getContextPluginRuntime(pluginId).getContextPluginRuntime();
					if (runtime != null) {
						// Ensure the app has a context support registration
						if (session.hasContextSupport(listener, contextType)) {
							// Make sure the plugin is configured
							if (!plug.isConfigured()) {
								Log.w(TAG, "Plugin Not Configured: " + pluginId);
								return new IdResult("Plug-in not configured " + plug.getId(),
										ErrorCodes.PLUG_IN_NOT_CONFIGURED);
							} else {
								// Check for AutoReactiveInteractiveContextPluginRuntimes
								if (runtime instanceof AutoReactiveInteractiveContextPluginRuntime) {
									AutoReactiveInteractiveContextPluginRuntime tmp = (AutoReactiveInteractiveContextPluginRuntime) runtime;
									// Check if the runtime has a UI for the contextType
									if (tmp.hasUserInterfaceForContextType(contextType)) {
										return launchUserInterfaceForInteractivePlugin(plug, app, listener, session,contextType);
									} else {
										// No UI for the context type, so handle as ReactiveContextPluginRuntime
										UUID requestId = registerRequestUUID(app, listener, plug);
										if (FrameworkConstants.DEBUG)
											Log.i(TAG, "requestContextScan for application: " + app
													+ " is launching a context scan request");
										/*
										 * Launch the context scan. Note that the 'HandleContextRequest' class
										 * determined whether to use a general or configured context scan based on
										 * whether the scanConfig is null.
										 */
										ContextPluginRuntimeMethodRunners.launchThread(new HandleContextRequest(
												ContextMgr, (ReactiveContextPluginRuntime) runtime, requestId,
												contextType, contextConfig), ContextManager
												.getThreadPriorityForPowerScheme());
										return new IdResult(requestId.toString());
									}
								}
								// Check for InteractiveContextPluginRuntimes
								else if (runtime instanceof InteractiveContextPluginRuntime) {
									return launchUserInterfaceForInteractivePlugin(plug, app, listener, session,
											contextType);
								}
								// Check for ReactiveContextPluginRuntimes
								else if (runtime instanceof ReactiveContextPluginRuntime) {
									UUID requestId = registerRequestUUID(app, listener, plug);
									if (FrameworkConstants.DEBUG)
										Log.i(TAG, "requestContextScan for application: " + app
												+ " is launching a context scan request");
									/*
									 * Launch the context scan. Note that the 'HandleContextRequest' class determined
									 * whether to use a general or configured context scan based on whether the
									 * scanConfig is null.
									 */
									ContextPluginRuntimeMethodRunners.launchThread(new HandleContextRequest(ContextMgr,
											(ReactiveContextPluginRuntime) runtime, requestId, contextType,
											contextConfig), ContextManager.getThreadPriorityForPowerScheme());
									return new IdResult(requestId.toString());
								}
								// Fail on plug-in mismatch
								else {
									return new IdResult(
											"Can't scan using plugin of type" + plug.getContextPluginType(),
											ErrorCodes.PLUG_IN_TYPE_MISMATCH);
								}
							}
						} else
							return new IdResult("No context support for " + contextType, ErrorCodes.NO_CONTEXT_SUPPORT);
					} else {
						return new IdResult("Could not find runtime for " + plug.getId(),
								ErrorCodes.DYNAMIX_FRAMEWORK_ERROR);
					}
				} else
					return new IdResult("Plug-in disabled " + plug.getId(), ErrorCodes.PLUG_IN_DISABLED);
			} else
				return new IdResult("Requested plug-in not found", ErrorCodes.PLUG_IN_NOT_FOUND);
		} else
			return new IdResult("Dynamix Not Started", ErrorCodes.NOT_READY);
	}

	/**
	 * Launches the user interface for the incoming ContextPlugins that have been verified as having an interface.
	 * 
	 * @param plug
	 *            The plug-in with the interface to launch
	 * @param app
	 *            The app requesting the interface
	 * @param listener
	 *            The listener requesting results
	 * @param session
	 *            The app's session
	 * @param contextType
	 *            The context type of the interaction.
	 * @return An IdResult indicating success of failure.
	 */
	private static IdResult launchUserInterfaceForInteractivePlugin(ContextPlugin plug, DynamixApplication app,
			IDynamixListener listener, DynamixSession session, String contextType) {
		// Make sure the interface is launchable
		if (Utils.checkPluginInterfaceLaunchable(app, ContextMgr.getContextPluginRuntime(plug.getId()), false, true)) {
			if (!ContextInteractionHostActivity.isActive()) {
				UUID requestId = registerRequestUUID(app, listener, plug);
				Intent intent = new Intent(androidContext, ContextInteractionHostActivity.class);
				intent.putExtra("pluginId", plug.getId());
				intent.putExtra("contextType", contextType);
				intent.putExtra("sessionId", session.getSessionId().toString());
				intent.putExtra("listenerId", session.getDynamixListenerId(listener));
				intent.putExtra("requestId", requestId.toString());
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				DynamixService.androidContext.startActivity(intent);
				return new IdResult(requestId.toString());
			} else
				return new IdResult("Host activity busy", ErrorCodes.RESOURCE_BUSY);
		} else
			return new IdResult("No permission to launch " + plug.getId(), ErrorCodes.NOT_AUTHORIZED);
	}

	/**
	 * Opens the specified plug-in's configuration view (if it has one) on behalf of a DynamixApplication.
	 * 
	 * @param app
	 *            The app requesting the configuation view.
	 * @param pluginId
	 *            The plug-in to configure.
	 * @return Result indicating success or failure.
	 */
	public static Result openContextPluginConfigurationForApp(DynamixApplication app, String pluginId) {
		return doContextPluginConfigurationView(app, pluginId, false);
	}

	/**
	 * Opens the specified plug-in's configuration view (if it has one) on behalf of the Dynamix Framework.
	 * 
	 * @param pluginId
	 *            The plug-in to configure.
	 * @return Result indicating success or failure.
	 */
	public static Result openContextPluginConfigurationForFramework(String pluginId) {
		return doContextPluginConfigurationView(null, pluginId, true);
	}

	/**
	 * Opens the specified plug-in's configuration view (if it has one). Handles both app and framework calls.
	 * 
	 * @param app
	 *            The app requesting the configuation view (may be null if frameworkCall == true).
	 * @param pluginId
	 *            The plug-in to configure.
	 * 
	 * @param frameworkCall
	 *            True if this call originates from Dynamix; false otherwise.
	 * @return
	 */
	private static Result doContextPluginConfigurationView(DynamixApplication app, String pluginId,
			boolean frameworkCall) {
		if (DynamixService.isFrameworkStarted()) {
			if (frameworkCall || app != null) {
				ContextPluginRuntimeWrapper runtime = ContextMgr.getContextPluginRuntime(pluginId);
				if (runtime != null) {
					// Check if the plug-in's interface is launchable
					if (Utils.checkPluginInterfaceLaunchable(app, runtime, frameworkCall, false)) {
						// Check if the host activity is BUSY
						if (!ContextPluginConfigurationHostActivity.isActive()) {
							Intent intent = new Intent(androidContext, ContextPluginConfigurationHostActivity.class);
							intent.putExtra("pluginId", pluginId);
							intent.putExtra("frameworkCall", frameworkCall);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							DynamixService.androidContext.startActivity(intent);
							return new Result();
						} else
							return new Result("Host activity busy", ErrorCodes.RESOURCE_BUSY);
					} else
						return new Result("Interface is not launchable by " + app, ErrorCodes.NOT_SUPPORTED);
				} else
					return new Result("Plug-in runtime not found " + pluginId, ErrorCodes.PLUG_IN_NOT_FOUND);
			} else
				return new Result("App not found " + app, ErrorCodes.DYNAMIX_FRAMEWORK_ERROR);
		} else
			return new Result("Dynamix Not Started", ErrorCodes.NOT_READY);
	}

	/**
	 * Utility method for setting the BaseActivity.
	 */
	static void setBaseActivity(Activity activity) {
		baseActivity = activity;
		Log.i(TAG, "setBaseActivity with " + activity);
	}

	/**
	 * Sets up the Dynamix Framework database using the incoming Context. If an existing database is already open, this
	 * method closes it first. In addition to DynamixService.boot, this method is also called by the BootUpReceiver,
	 * which is used to start the DynamixFramework in the background when the device firsts starts up.
	 */
	static void setupDatabase(FrameworkConfiguration config, Context context) throws Exception {
		// If a SettingsManager already exists, close it before creating a new one
		if (SettingsManager != null) {
			SettingsManager.closeDatabase();
		}
		// Grab the Dynamix's data path
		String dataPath = Utils.getDataDirectoryPath(context);
		// Create the database path using dbPath and the incoming FrameworkConfiguration
		String dbPath = dataPath + config.getDatabaseFilePath();
		// Create a SettingsManager and open the database
		SettingsManager = new DB4oSettingsManager();
		SettingsManager.openDatabase(dbPath);
	}

	/**
	 * Starts the context plug-in update timer using the update interval set in the Dynamix preferences.
	 */
	static void startContextPluginUpdateTimer() {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (DynamixPreferences.autoContextPluginUpdateCheck(androidContext)) {
					updateContextPluginUpdateTimer(DynamixPreferences.getContextPluginUpdateInterval(androidContext));
				}
			}
		});
	}

	/**
	 * Stops the context plug-in update timer.
	 */
	static void stopContextPluginUpdateTimer() {
		if (contextPlugUpdateTimer != null)
			contextPlugUpdateTimer.cancel();
		contextPlugUpdateTimer = null;
	}

	/**
	 * Unregisteres a previoully registered plug-in configuration activity.
	 * 
	 * @param runtime
	 *            The ContextPluginRuntime wishing to unregister its configuration activity.
	 */
	static void unRegisterConfigurationActivity(ContextPluginRuntime runtime) {
		if (ContextMgr != null) {
			ContextMgr.unRegisterConfigurationActivity(runtime);
		}
	}

	/**
	 * Unregisteres a previoully registered context acquisition activity.
	 * 
	 * @param runtime
	 *            The ContextPluginRuntime wishing to unregister its context acquisition activity.
	 */
	static void unregisterContextAcquisitionActivity(ContextPluginRuntime runtime) {
		if (ContextMgr != null) {
			ContextMgr.unregisterContextAcquisitionActivity(runtime);
		}
	}

	/**
	 * Replaces the authorized application with the incoming application in both the SettingsManager and SessionManager
	 * (if Dynamix is running).
	 */
	static boolean updateApplication(DynamixApplication app) {
		if (SettingsManager.updateApplication(app)) {
			if (SessionManager.updateSessionApplication(app)) {
				SessionManager.refreshApps();
				return true;
			} else {
				Log.w(TAG, "Could not update session for app");
				return false;
			}
		} else {
			Log.w(TAG, "SettingsManager could not update: " + app);
			return false;
		}
	}

	/**
	 * Updates the context plug-in update timer with a new check interval.
	 * 
	 * @param interval
	 *            How often to check for updates (in milliseconds).
	 */
	static void updateContextPluginUpdateTimer(int interval) {
		stopContextPluginUpdateTimer();
		Log.i(TAG, "Setting timer for update check every: " + interval + " milliseconds");
		contextPlugUpdateTimer = new CountDownTimer(interval, interval) {
			@Override
			public void onFinish() {
				Log.i(TAG, "Auto Context Plug-in Update!");
				// Use the update manager to check for updates
				checkForContextPluginUpdates();
				// Restart another update check, if necessary
				if (DynamixPreferences.autoContextPluginUpdateCheck(androidContext))
					updateContextPluginUpdateTimer(DynamixPreferences.getContextPluginUpdateInterval(androidContext));
			}

			@Override
			public void onTick(long millisUntilFinished) {
				// Log.i(TAG, "Time until update: " + millisUntilFinished);
			}
		}.start();
	}

	/**
	 * Refreshes Android notifications, rebuilding notifications and refreshing the tray icons, if needed. Note that
	 * currently, we only show one notification per type.
	 */
	static void updateNotifications() {
		if (!embeddedMode && isFrameworkInitialized()) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					// Log.d(TAG, "updateNotifications running with androidContext: " + androidContext);
					notificationMgr.removeAllNotifications();
					// If we still have pending applications, add a single notification
					if (SettingsManager.getPendingApplications().size() > 0) {
						notificationMgr.addNotification(new AndroidNotification(PENDING_APP_TAB_ID,
								AndroidNotification.Type.PENDING_APP, R.drawable.alert, service.getText(
										R.string.pending_app_notification).toString()));
					}
					List<PluginDiscoveryResult> updates = UpdateManager.getFilteredContextPluginUpdates();
					// If we still have pending applications, add a single notification
					if (updates != null && updates.size() > 0) {
						notificationMgr.addNotification(new AndroidNotification(UPDATES_TAB_ID,
								AndroidNotification.Type.PLUGIN_UPDATE, R.drawable.alert, service.getText(
										R.string.context_plugin_updates_available).toString()));
					}
					notificationMgr.showAllNotifications();
					
					/*
					 * TODO: In the future, we should add a pop-up dialog that asks the user about 
					 * permissions exactly when an app wants to do something. I did a test, and pop-ups
					 * are possible from services using the code below. There is also a project that
					 * does this here: http://code.google.com/p/android-smspopup/
					 */
					// Here's another popup technique: http://www.piwai.info/chatheads-basics/
					
					// Popup code
					//Intent popup = new Intent(DynamixService.getAndroidContext(), PermissionPopupActivity.class);
			        //popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					//DynamixService.getAndroidContext().startActivity(popup);
				}
			});
		}
	}

	/**
	 * Returns the current Android context.
	 */
	public static Context getAndroidContext() {
		return androidContext;
	}

	/**
	 * Completes the Dynamix boot sequence after the OSGi Framework has started and the Dynamix Service has started.
	 */
	private static void completeBoot() {
		// Only completeBoot if we're BOOTING
		synchronized (bootState) {
			if (bootState == BootState.BOOTING) {
				Log.i(TAG, "Completing the Dynamix Service boot sequence...");
				// Set booted state
				bootState = BootState.BOOTED;
				// Update HomeActivity UI
				uiHandle.post(new Runnable() {
					public void run() {
						// HomeActivity.enableToggleButton();
					}
				});
				// Stop our boot progress indicator, if necessary
				if (bootProgress != null) {
					bootProgress.dismiss();
					bootProgress = null;
				}
				/*
				 * Setup our internal Web server to handle web clients. Note that we can only start the WebConnector
				 * after booting, since it needs the webFacade to be properly initialized.
				 */
				DynamixService.startWebConnectorUsingConfigData();
				/*
				 * TODO: Now we just check for updates in the background, which means the PluginActivity may not show
				 * all available plug-ins until it's refreshed. Find a better way!
				 */
				checkForContextPluginUpdates();
				// Send initialized event
				onDynamixInitialized(service);
				/*
				 * TODO: If we did not exit cleanly, deactivate all plug-ins?
				 */
				// Process startFramework, if necessary
				if ((DynamixPreferences.isDynamixEnabled(androidContext))
						&& (DynamixPreferences.autoStartDynamix(androidContext) || startRequested))
					startFramework();
				else {
					if (!embeddedMode) {
						// Update the UI
						uiHandle.post(new Runnable() {
							public void run() {
								BaseActivity.setTitlebarDisabled();
							}
						});
					}
				}
				// Finally, refresh UIs
				if (!embeddedMode) {
					HomeActivity.refreshData();
				}
				Log.i(TAG, "Dynamix has finished booting!");
				
		        
				//SmartSantander
				//Intent i = new Intent();
		        //i.setAction("com.example.androiddistributed.MainService");        
		        //context.sendBroadcast(i);
				
			} else
				Log.w(TAG, "completeBoot called when not booting");
		}
	}

	/**
	 * Default implementation that returns null.
	 */
	public IBinder asBinder() {
		// return facadeBinder;
		return null;
	}

	/**
	 * Returns the Dynamix facade to requesting clients.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind is returning facadeBinder: " + facadeBinder);
		return facadeBinder;
	}

	/**
	 * Sets up initial Dynamix service state.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Initializing Dynamix from state: " + bootState);
		// Notify initializing
		onDynamixInitializing();
		// Ensure the keystore is created
		try {
			initKeyStore(this);
		} catch (Exception e2) {
			Log.w(TAG, "Problem extracting keystore: " + e2);
		}
		// Check boot state
		synchronized (bootState) {
			/*
			 * We will be NOT_BOOTED if Dynamix crashed and was restarted by Android. In this case, boot will not have
			 * been called..
			 */
			if (bootState == BootState.NOT_BOOTED) {
				bootState = BootState.BOOTING;
				Log.w(TAG, "onCreate NOT_BOOTED... we probably crashed!");
				// Call startFramework to cache a start request
				startFramework();
			}
			if (bootState == BootState.BOOTING) {
				/*
				 * !!! IMPORTANT !!! A call to the SessionManager MUST be made by our thread before any others so that
				 * the SessionManager's Handler is bound to our main thread. DO NOT REMOVE THIS CALL FROM THIS LOCATION.
				 */
				SessionManager.getAllSessions();
				/*
				 * Prepare our looper, if needed.
				 */
				if (Looper.myLooper() == null)
					Looper.prepare();
				// Set service is running
				androidServiceRunning = true;
				// Create a framework session id for secure internal communications
				frameworkSessionId = UUID.randomUUID();
				// Setup state for embedded and service modes
				if (!embeddedMode) {
					/*
					 * Remember our service. If we're running in embeddedMode, the service will have already been
					 * created by 'bootEmbedded'.
					 */
					service = this;
					/*
					 * In service mode, the androidContext is the service itself. If we're running in embeddedMode, the
					 * androidContext will have already been set by 'bootEmbedded'.
					 */
					DynamixService.androidContext = this;
					/*
					 * In service mode, the config needs to be created. If we're running in embeddedMode, the config
					 * will have already been set by 'bootEmbedded'.
					 */
					try {
						config = createFrameworkConfigurationFromPropsFile(service);
					} catch (Exception e1) {
						Log.e(TAG, e1.toString());
						Utils.showGlobalAlert(getBaseActivity(), e1.toString(), true);
						return;
					}
					// Setup foreground service handling so that Android tries desperately to keep us alive.
					try {
						foregroundHandler = new AndroidForeground(
								(NotificationManager) getSystemService(NOTIFICATION_SERVICE), getClass().getMethod(
										"startForeground", AndroidForeground.mStartForegroundSignature), getClass()
										.getMethod("stopForeground", AndroidForeground.mStopForegroundSignature));
					} catch (NoSuchMethodException e) {
						// Running on an older platform.
						foregroundHandler = new AndroidForeground(
								(NotificationManager) getSystemService(NOTIFICATION_SERVICE), null, null);
					}
					foregroundHandler.startForegroundCompat(1, new Notification(), this);
				}
				// Setup the database
				try {
					setupDatabase(DynamixService.getConfig(), service);
					Log.i(TAG, "Database is ready");
				} catch (Exception e) {
					Log.e(TAG, e.toString());
					Utils.showGlobalAlert(getBaseActivity(), e.toString(), true);
					return;
				}
				// Setup the ContextManager
				if (ContextMgr != null) {
					// If the ContextManager exists, pause it... it will be started again when Dynamix starts.
					if (ContextMgr.isStarted())
						ContextMgr.pauseContextHandling();
				} else {
					// Create the Context Manager
					ContextMgr = new ContextManager(DynamixService.androidContext, SettingsManager.getPowerScheme(),
							DynamixService.getConfig().getContextCacheMaxEvents(), DynamixService.getConfig()
									.getContextCacheMaxDurationMills(), DynamixService.getConfig()
									.getContextCacheCullIntervalMills());
					Log.i(TAG, "Created the Dynamix ContextManager");
				}
				/*
				 * Create our binders, which allow client's to connect the Dynamix.
				 */
				facadeBinder = new AppFacadeBinder(this, ContextMgr, embeddedMode);
				webFacade = new WebFacadeBinder(this, ContextMgr, embeddedMode);
				// Setup support for self-signed certs, if requested
				if (config.allowSelfSignedCertsDefault())
					Utils.acceptAllSelfSignedSSLcertificates();
				else
					Utils.denyAllSelfSignedSSLcertificates();
				// Create our restart PendingIntent (for use in Dynamix restart requests)
				Intent broadcastReceiverIntent = new Intent(androidContext, BootUpReceiver.class);
				broadcastReceiverIntent.putExtra("restart", true);
				RESTART_INTENT = PendingIntent.getBroadcast(androidContext, 0, broadcastReceiverIntent, 0);
				// Create our notification manager
				if (notificationMgr != null)
					notificationMgr.removeAllNotifications();
				notificationMgr = new DynamixNotificationManager(service);
				// Show boot progress ProgressDialog
				if (getBaseActivity() != null)
					bootProgress = ProgressDialog.show(getBaseActivity(), "Loading Dynamix",
							"Initializing framework and plugins. Please wait...");
				/*
				 * Launch the OSGi framework. Note that osgiMgr.init() runs on its own thread and calls us back using
				 * onOSGiFrameworkStarted, which continues the boot process.
				 */
				if (OsgiMgr != null) {
					// If the OsgiMgr exists, stop it before continuing.
					OsgiMgr.stopFramework();
					Log.w(TAG, "OSGi Manager was not null during onCreate");
				}
				OsgiMgr = new OSGIManager(this);
				OsgiMgr.init();
				Log.i(TAG, "Initializing the Dynamix OSGi Manager...");
			} else {
				Log.e(TAG, "Init was called in state " + bootState);
				throw new RuntimeException("Init was called in state " + bootState);
			}
		}
	}
	
   

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (isFrameworkInitialized()) {
			destroyFramework(true, false);
		}
	}

	@Override
	public void onRebind(Intent intent) {
		// TODO: Handle rebind?
	}

	/**
	 * Handles the Android onStartCommand life-cycle call. This is called after onCreate() when running as a service.
	 * Note that there is a bug in Android 2.3/3.0 where onStartCommand will *not* be called in the case of a crash and
	 * then subsequest restart. https://groups.google.com/forum/?hl=en&fromgroups#!topic/android-developers/2H-zkME9FB0
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand with bootState: " + bootState);
		// Return START_STICKY so that Android tries to keep us alive (or restarts us)
		return Service.START_STICKY;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "App onUnbind for UID: " + Binder.getCallingUid());
		super.onUnbind(intent);
		/*
		 * The Context Manager listens for onCallbackDied events from connected applications, and calls
		 * DynamixService.removeContextListener(listener, true) whether Dynamix is active or not. This means that we
		 * don't need to do any cleanup in this method like before (below). OLD CODE -------- // Try to grab the
		 * application from the SettingsManager (this can return null) DynamixApplication app =
		 * SettingsManager.getAuthorizedApplication(Binder.getCallingUid()); // Remove the context listener for the app
		 * (method works, even if app is null) uncacheApplication(app, false);
		 */
		// Return false so that Android does not call rebind when the client tries to connect again.
		return false;
	}

	/**
	 * Experimental: This method is called as services are registered in the OSGi framework. Security -
	 * ConditionalPermissionAdmin
	 */
	protected synchronized void handleServiceEvent(ServiceEvent event) {
		Log.d(TAG, "handleServiceEvent of type: " + event.getType());
		/*
		 * Event type int values: Type == 1 (Registered?) Type == 4 (Unregistered?)
		 */
		if (event.getType() == ServiceEvent.REGISTERED) {
			// This event is handled by handleBundleInstalled, which is called
			// directly from OSGIManager Installers
		}
	}

	/**
	 * Unregisters a previously registered request UUID
	 * 
	 * @param requestId
	 *            The original request UUID
	 * @return True if the requestId was removed; false otherwise.
	 */
	protected boolean unregisterRequestUUID(UUID requestId) {
		return ContextMgr.unregisterRequestUUID(requestId);
	}

	/**
	 * Utility method for cloding the progress dialog.
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.cancel();
			progressDialog = null;
		}
	}

	/**
	 * Start the Dynamix Framework.
	 */
	private synchronized void doStartFramework() {
		Log.d(TAG, "doStartFramework() with bootState: " + bootState);
		if (isFrameworkInitialized()) {
			if (startState == StartState.STOPPED) {
				synchronized (startState) {
					startState = StartState.STARTING;
				}
				onDynamixStarting();
				// Update UI
				launchProgressDialog("Enabling Experimentation", "Starting Sensors. Please wait...");
				// Set a BroadcastReceiver to startContextDetection on ACTION_SCREEN_ON events
				wakeReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						// Handle device wake!
						Log.v(TAG, "wakeReceiver onReceive called");
						if (isFrameworkStarted()) {
							if (!DynamixPreferences.backgroundModeEnabled(androidContext)) {
								Log.i(TAG, "Resuming Dynamix");
								ContextMgr.startContextManager();
								startAppChecker(DynamixService.getConfig().getAppLivelinessCheckIntervalMills());
								WebConnector.resumeTimeoutChecking();
								// Notify apps that we're active
								SessionManager.notifyAllDynamixFrameworkActive();
							}
						}
					}
				};
				registerReceiver(wakeReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
				// Set a BroadcastReceiver to stopContextDetection on ACTION_SCREEN_OFF events
				sleepReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						// Handle device sleep!
						Log.v(TAG, "sleepReceiver onReceive called");
						if (isFrameworkStarted()) {
							if (!DynamixPreferences.backgroundModeEnabled(androidContext)) {
								Log.i(TAG, "Pausing Dynamix");
								ContextMgr.pauseContextHandling();
								if (appChecker != null)
									appChecker.cancel();
								WebConnector.pauseTimeoutChecking();
								// Notify apps that we're inactive
								SessionManager.notifyAllDynamixFrameworkInactive();
							} else
								Log.i(TAG, "Keeping context scanning alive when screen is off");
						}
					}
				};
				registerReceiver(sleepReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
				
				/*myReceiver = new MyReceiver();

				IntentFilter filter = new IntentFilter();
				filter.addAction("org.ambiendynamix.core.DynamixService");
				registerReceiver(myReceiver, filter);
				Log.i("WTF", "if you see this, you win");*/
				
				/*
				 * Complete start on a thread...
				 */
				new Thread(new Runnable() {
					@Override
					public void run() {
						// State bookkeeping
						startRequested = false;
						// Tell the OSGiManager about Dynamix
						OsgiMgr.setService(DynamixService.this);
						// Start OSGi installer workers
						// OsgiMgr.startInstallerWorkers();
						// Start the ContextManager (re-starts initialized plug-ins)
						ContextMgr.startContextManager();
						// Init our plugins (ignores initializing plug-ins)
						initializePlugins();
						while (!ContextMgr.isStarted()) {
							Log.d(TAG, "Waiting for ContextManager to start...");
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						Log.d(TAG, "ContextManager has started!");
						// We are initialized (set our static reference)
						DynamixService.service = DynamixService.this;
						// Setup a Timer to check periodically for application liveliness...
						startAppChecker(DynamixService.getConfig().getAppLivelinessCheckIntervalMills());
						DynamixPreferences.setDynamixEnabledState(androidContext, true);
						synchronized (startState) {
							startState = StartState.STARTED;
						}
						// Handle notifications
						onDynamixStarted();
						SessionManager.notifyAllDynamixFrameworkActive();
						UpdateManager.checkForDynamixUpdates(getAndroidContext(), DynamixService.getConfig()
								.getPrimaryDynamixServer().getUrl(), new DynamixUpdatesCallbackHandler());
						Log.i(TAG, "Dynamix Service Started!");
	

						//SmartSantander
						if (DynamixService.getPhoneProfiler().getStarted()==false)
							DynamixService.getPhoneProfiler().start();
						else
							DynamixService.getPhoneProfiler().startJob();
						
						if (DynamixService.getDemon().getStarted()==false)
							DynamixService.getDemon().start();
						else
							DynamixService.getDemon().startJob();
						
						

						if (!embeddedMode)
							uiHandle.post(new Runnable() {
								public void run() {
									closeProgressDialog();
									// Set the toggle state to True in the HomeActivity
									HomeActivity.setActiveState(true);
									BaseActivity.setTitlebarEnabled();
									updateNotifications();
								}
							});
					}
				}).start();
			} else
				Log.w(TAG, "Cannot start from state: " + startState);
		} else
			Log.w(TAG, "Cannot start Dynamix because it's not initialized");
	}

	/**
	 * Initialize all installed ContextPlugins.
	 */
	private void initializePlugins() {
		// First, get the list of ContextPlugins from the SettingsManager
		List<ContextPlugin> plugs = SettingsManager.getInstalledContextPlugins();
		// Note that the ContextManager manages *all* plugins, even those that are not installed yet.
		for (ContextPlugin plug : plugs) {
			// If the plug is installed, start its bundle and then add it to the ContextManager for management.
			if (plug.isInstalled()) {
				// Try to start the plugin's OSGi Bundle
				if (OsgiMgr.startPluginBundle(plug)) {
					/*
					 * Add the ContextPlugin to the ContextMgr, getting the plugin's ContextPluginRuntimeFactory from
					 * the OsgiMgr.
					 */
					ContextPluginSettings settings = SettingsManager.getContextPluginSettings(plug);
					ContextMgr.initializeContextPlugin(plug, OsgiMgr.getContextPluginRuntimeFactory(plug), settings,
							null);
				} else {
					Log.w(TAG, "initializePlugins could not start bundle for: " + plug);
				}
			} else {
				/*
				 * The ContextPlugin is not installed... so ask the OsgiMgr to install the bundle for the given plugin.
				 * The installBundle method will return immediately, but its action may be asynchronous if the bundle
				 * was not previously installed (calling 'handleBundleInstalled' when complete).
				 */
				if (OsgiMgr.installBundle(plug, null)) {
					// Add the ContextPlugin to the ContextManager along with its runtime factory
					ContextPluginSettings settings = SettingsManager.getContextPluginSettings(plug);
					ContextMgr.initializeContextPlugin(plug, OsgiMgr.getContextPluginRuntimeFactory(plug), settings,
							null);
				} else {
					/*
					 * Add the ContextPlugin to the ContextMgr without its runtime factory. Note that the runtime will
					 * be added to the ContextMgr once it's installed by OSGi, which is signified by a
					 * 'handleBundleInstalled' event or 'handleBundleInstallError' event.
					 */
					ContextMgr.addNewContextPlugin(plug);
				}
			}
		}
	}

	/**
	 * Stops the Dynamix Framework, releasing its acquired resources and killing the service, if requested.
	 * 
	 * @param removeListeners
	 *            True if context manager listeners should be removed; false if listeners should be retained.
	 * @param destroy
	 *            True if Dynamix should be destroyed; false otherwise.
	 */
	private synchronized void doStopFramework(final boolean destroy, final boolean killProcess,
			final boolean restartProcess) {
		if (startState == StartState.STOPPED)
			doDestroyFramework(killProcess, restartProcess);
		else if (startState == StartState.STOPPING)
			Log.i(TAG, "Already stopping... please wait");
		else {
			// Set stopping state
			synchronized (startState) {
				startState = StartState.STOPPING;
			}
			onDynamixStopping();
			// Show progress
			launchProgressDialog("Disabling Dynamix", "Stopping plug-ins. Please wait...");
			// Clear all notifications
			notificationMgr.removeAllNotifications();
			// Unregister our wakeReceiver
			if (wakeReceiver != null) {
				unregisterReceiver(wakeReceiver);
				wakeReceiver = null;
			}
			// Unregister our sleepReceiver
			if (sleepReceiver != null) {
				unregisterReceiver(sleepReceiver);
				sleepReceiver = null;
			}
			// Stop the app checker timer
			if (appChecker != null) {
				appChecker.cancel();
				appChecker = null;
			}
			// Stop our boot progress indicator
			if (bootProgress != null) {
				bootProgress.dismiss();
				bootProgress = null;
			}
			/*
			 * Complete stop on a thread...
			 */
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					// Stop and plug-in discovery operations
					UpdateManager.cancelContextPluginUpdate();
					if (!destroy) {
						// Pause context detection
						ContextMgr.pauseContextHandling();
						// Wait for the ContextManager to stop
						while (!ContextMgr.isPaused()) {
							try {
								Log.d(TAG, "Waiting for ContextManager to pause...");
								Thread.sleep(500);
							} catch (InterruptedException e) {
							}
						}
						/*
						 * Stop each OSGi bundle associated to ContextPlugins managed by the ContextManager.
						 */
						for (ContextPlugin plug : ContextMgr.getAllContextPlugins()) {
							OsgiMgr.stopPluginBundle(plug);
						}
						// Set all disabled in the preferences
						DynamixPreferences.setDynamixEnabledState(getAndroidContext(), false);
					}
					// Set stopped state
					synchronized (startState) {
						startState = StartState.STOPPED;
					}
					// Update the UI
					if (!embeddedMode)
						uiHandle.post(new Runnable() {
							public void run() {
								closeProgressDialog();
								HomeActivity.setActiveState(false);
								BaseActivity.setTitlebarDisabled();
							}
						});
					// Handle notifications
					onDynamixStopped();
					SessionManager.notifyAllDynamixFrameworkInactive();
					// Finish destroy, if requested
					if (destroy)
						doDestroyFramework(killProcess, restartProcess);
				}
			});
		}
	}

	/**
	 * Completes the framework destroy process
	 * 
	 * @param killProcess
	 * @param restartProcess
	 */
	private synchronized void doDestroyFramework(final boolean killProcess, final boolean restartProcess) {
		// Only destroy if we're initialized and not started
		if (isFrameworkInitialized() && !isFrameworkStarted()) {
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					Log.i(TAG, "Destroying Dynamix Framework...");
					ContextMgr.stopContextHandling();
					// Wait for the ContextManager to stop
					while (!ContextMgr.isStopped()) {
						try {
							Log.d(TAG, "Waiting for ContextManager to stop...");
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
					}
					// Stop the OSGi manager
					OsgiMgr.stopFramework();
					// Close all sessions
					SessionManager.closeAllSessions(true);
					// Close the database
					SettingsManager.closeDatabase();
					if (!embeddedMode) {
						// Remove our foreground handler
						if (foregroundHandler != null)
							foregroundHandler.stopForegroundCompat(1, DynamixService.this);
						/*
						 * TODO: If clients are still bound, stopSelf will *not* stop the service, even though we use
						 * startService to start Dynamix. This is a problem, since we are unable to stop properly if we
						 * still have bound clients. I'm currently using System.exit below to completely kill the
						 * service; however, Android will restart us, which we don't want.
						 */
						service.stopSelf();
					}
					// Stop the WebConnector
					stopWebConnector();
					// Wait a bit so that final events can be sent out to clients
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					// Kill the remote listeners
					SessionManager.killRemoteListeners();
					// Set not booted
					synchronized (bootState) {
						bootState = BootState.NOT_BOOTED;
					}
					Log.i(TAG, "Destroyed Dynamix Framework");
					// Handle kill process request
					if (killProcess) {
						// Setup a restart, if requested
						if (restartProcess) {
							AlarmManager mgr = (AlarmManager) androidContext.getSystemService(Context.ALARM_SERVICE);
							mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, RESTART_INTENT);
						}
						// Exit our process
						System.exit(0);
					}
				}
			});
		} else
			Log.w(TAG, "Cannot destroy framework while in state " + startState);
	}

	/**
	 * Launches a progress dialog that is shown to the user.
	 * 
	 * @param title
	 *            The title of the dialog box.
	 * @param message
	 *            The content of the dialog box.
	 */
	private void launchProgressDialog(String title, String message) {
		if (!embeddedMode) {
			closeProgressDialog();
			if (getBaseActivity() != null) {
				try {
					progressDialog = ProgressDialog.show(getBaseActivity(), title, message);
				} catch (Exception e) {
					progressDialog = null;
				}
			}
		}
	}

	/**
	 * Starts a threaded background timer that periodically calls 'checkAppLiveliness' on the ContextManager to
	 * determine if DynamixApplicaitons are still alive.
	 * 
	 * @param checkPeriodMills
	 *            How often to check for livelyness (in Milleseconds).
	 */
	private void startAppChecker(long checkPeriodMills) {
		if (appChecker != null)
			appChecker.cancel();
		appChecker = new Timer(true);
		appChecker.schedule(new TimerTask() {
			@Override
			public void run() {
				for (DynamixSession session : SessionManager.getAllSessions())
					if (session.isSessionOpen())
						ContextMgr.checkAppLiveliness(session.getApp());
			}
		}, 0, checkPeriodMills);
	}

	/*
	 * Boot state constants
	 */
	private static enum BootState {
		NOT_BOOTED, BOOTING, BOOTED
	}

	/*
	 * Private internal class for handling checkForContextPluginUpdates()
	 */
	private static class DynamixUpdatesCallbackHandler implements IDynamixUpdateListener {
		@Override
		public void onUpdateStarted() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onUpdateCancelled() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onUpdateError(String message) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onUpdateComplete(DynamixUpdates updates) {
			Log.i(TAG, "Received Dynamix Updates....");
			// Check if our trusted certs are up to date
			List<TrustedCert> currentCerts = getAuthorizedCertsFromKeyStore();
			List<TrustedCert> removeCerts = new ArrayList<TrustedCert>();
			List<TrustedCertBinder> addCerts = new ArrayList<TrustedCertBinder>();
			// Create our lists of new certs to add
			for (TrustedCertBinder cert : updates.getTrustedWebConnectorCerts()) {
				boolean found = false;
				for (TrustedCert currentCert : currentCerts) {
					if (cert.getFingerprint().equalsIgnoreCase(currentCert.getFingerprint())) {
						found = true;
						break;
					}
				}
				if (!found) {
					Log.i(TAG, "Found new cert to add: " + cert.getAlias());
					addCerts.add(cert);
				}
			}
			// Create a list of certs to remove
			for (TrustedCert currentCert : currentCerts) {
				boolean found = false;
				for (TrustedCertBinder cert : updates.getTrustedWebConnectorCerts()) {
					if (cert.getFingerprint().equalsIgnoreCase(currentCert.getFingerprint())) {
						found = true;
						break;
					}
				}
				if (!found) {
					Log.i(TAG, "Found cert to remove: " + currentCert.getAlias());
					removeCerts.add(currentCert);
				}
			}
			// Remove certs
			for (TrustedCert remove : removeCerts) {
				try {
					removeAuthorizedCert(remove.getAlias());
				} catch (Exception e) {
					Log.w(TAG, "Could not remove " + remove.getAlias() + ": " + e);
				}
			}
			// Add new certs
			for (TrustedCertBinder add : addCerts) {
				try {
					storeAuthorizedCert(add.getAlias(), Utils.downloadCertificate(add.getUrl()));
				} catch (Exception e) {
					Log.w(TAG, "Could not store certificate: " + e);
				}
			}
		}
	}

	/*
	 * Private internal class for handling checkForContextPluginUpdates()
	 */
	private static class ContextPluginCallbackHandler implements IContextPluginUpdateListener {
		IContextPluginUpdateListener callback;
		Mode mode;

		public ContextPluginCallbackHandler(IContextPluginUpdateListener callback, Mode mode) {
			this.callback = callback;
			this.mode = mode;
		}

		@Override
		public void onUpdateCancelled() {
			if (callback != null)
				callback.onUpdateCancelled();
		}

		@Override
		public void onUpdateComplete(List<PluginDiscoveryResult> incomingUpdates,
				Map<IContextPluginConnector, String> errors) {
			List<DiscoveredContextPlugin> existingUpdates = SettingsManager.getPendingContextPlugins();
			List<DiscoveredContextPlugin> finalUpdates = new Vector<DiscoveredContextPlugin>();
			// Maintain existing updates for PluginSource's with errors
			for (DiscoveredContextPlugin existingUpdate : existingUpdates) {
				for (IContextPluginConnector errorSource : errors.keySet()) {
					if (errorSource.equals(existingUpdate.getContextPlugin().getRepoSource()))
						finalUpdates.add(existingUpdate);
					break;
				}
			}
			List<ContextPluginInformation> discoveredPlugins = new ArrayList<ContextPluginInformation>();
			for (PluginDiscoveryResult result : incomingUpdates) {
				finalUpdates.add(result.getDiscoveredPlugin());
				discoveredPlugins.add(result.getDiscoveredPlugin().getContextPlugin().getContextPluginInformation());
			}
			// Update the SettingsManager
			SettingsManager.setContextPluginUpdates(finalUpdates);
			// Notify clients about the plug-ins
			SessionManager.notifyAllContextPluginDiscoveryFinished(discoveredPlugins);
			Log.d(TAG, "checkForContextPluginUpdates is completed with total updates: " + finalUpdates.size());
			if (callback != null)
				if (mode == Mode.UPDATE)
					callback.onUpdateComplete(UpdateManager.getFilteredContextPluginUpdates(), errors);
				else
					callback.onUpdateComplete(UpdateManager.getNewContextPlugins(), errors);
		}

		@Override
		public void onUpdateStarted() {
			if (callback != null)
				callback.onUpdateStarted();
		}

		enum Mode {
			NEW, UPDATE
		}

		@Override
		public void onUpdateError(String message) {
			Log.w(TAG, "onUpdateError: " + message);
		}
	}

	public static void onDynamixInitializing() {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixInitializing();
				}
			});
	}

	public static void onDynamixInitialized(final DynamixService service) {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixInitialized(service);
				}
			});
	}

	public static void onDynamixInitializingError(final String message) {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixInitializingError(message);
				}
			});
	}

	public static void onDynamixStarting() {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixStarting();
				}
			});
	}

	public static void onDynamixStarted() {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixStarted();
				}
			});
	}

	public static void onDynamixStopping() {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixStopping();
				}
			});
	}

	public static void onDynamixStopped() {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixStopped();
				}
			});
	}

	public static void onDynamixError(final String message) {
		for (final IDynamixFrameworkListener listener : frameworkListeners)
			Utils.dispatch(new Runnable() {
				@Override
				public void run() {
					listener.onDynamixError(message);
				}
			});
	}

	public interface IDynamixFrameworkListener {
		void onDynamixInitializing();

		void onDynamixInitializingError(String message);

		void onDynamixInitialized(DynamixService dynamix);

		void onDynamixStarting();

		void onDynamixStarted();

		void onDynamixStopping();

		void onDynamixStopped();

		void onDynamixError(String message);
	}
	
	public static boolean isNetworkAvailable() {
	        ConnectivityManager connectivityManager = (ConnectivityManager) DynamixService.getAndroidContext().getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	    }

	public static boolean getConnectionStatus() {
		return connectionStatus;
	}
	
	public static void setConnectionStatus(Boolean status) {
		connectionStatus=status;
	}

	public static void setTitleBarRestarting(boolean flag) {

		if (flag==true){
			uiHandle.post(new Runnable() {
				public void run() {
					BaseActivity.setTitlebarRestarting();
				}
			});
		}else{
			uiHandle.post(new Runnable() {
				public void run() {
					BaseActivity.setTitlebarEnabled();
				}
			});
		}
		
	}

}
