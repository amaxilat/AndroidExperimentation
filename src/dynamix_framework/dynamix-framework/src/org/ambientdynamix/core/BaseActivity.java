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

import org.ambientdynamix.util.AndroidNotification;

import org.ambientdynamix.core.R;

import eu.smartsantander.androidExperimentation.AndroidExperimentationService;
import eu.smartsantander.androidExperimentation.tabs.dynamixTab;
import eu.smartsantander.androidExperimentation.tabs.jobsTab;
import eu.smartsantander.androidExperimentation.tabs.profileTab;
import eu.smartsantander.androidExperimentation.tabs.reportTab;
import eu.smartsantander.androidExperimentation.tabs.securityTab;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewParent;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

/**
 * Base Activity for the Dynamix Framework UI. Responsible for hosting Tabs and
 * booting Dynamix, which launches the Dynamix background service.
 * <p>
 * Note: This Activity is registered as the 'application' in the
 * AndroidManifest.xml, so it's started first.
 * 
 * @see DynamixService
 * @author Darren Carlson
 */
public class BaseActivity extends TabActivity {
	private final static String TAG = BaseActivity.class.getSimpleName();
	/*
	 * Useful Links: Fancy ListViews:
	 * http://www.androidguys.com/tag/android-listview/ Custom ListView
	 * Adapters:
	 * http://www.softwarepassion.com/android-series-custom-listview-items
	 * -and-adapters/ Common layout objects:
	 * http://developer.android.com/guide/topics/ui/layout-objects.html Layout
	 * tricks!
	 * http://www.curious-creature.org/2009/02/22/android-layout-tricks-1/ Table
	 * Layout (Good):
	 * http://www.vbsteven.be/blog/using-the-simpleadapter-with-a-
	 * listview-in-android/ MultiColumn ListView:
	 * http://www.heikkitoivonen.net/blog
	 * /2009/02/15/multicolumn-listview-in-android/
	 */
	/**
	 * Allows external callers to activate the specified Tab.
	 * 
	 * @param tabID
	 */
	private static BaseActivity baseActivity;
	private static Handler uiHander = new Handler();
	private static Context context;
	public static int HOME_TAB_ID = 0;
	public static int PENDING_TAB_ID = 1;
	public static int PRIVACY_TAB_ID = 2;
	public static int PLUGINS_TAB_ID = 3;
	public static int UPDATES_TAB_ID = 4;
	private TabHost tabHost = null;
	private final Handler uiHandler = new Handler();
	private static boolean activityVisible;

	// Android Experimentation Members
	private Boolean tabIntentListenerIsRegistered = false;
	private tabIntentListener tabIntentlistener = null;
	private Boolean serviceIntentListenerIsRegistered = false;
	private ServiceIntentListener serviceIntentlistener = null;
	Messenger mService = null;
	private dynamixTab dTab;
	private profileTab pTab;
	private jobsTab jTab;
	private reportTab rTab;
	private securityTab sTab;
	boolean mBound;

	public static void close() {
		if (baseActivity != null)
			baseActivity.finish();
	}

	public static void toast(final String message, final int duration) {
		uiHander.post(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "TOASTING: " + message);
				Toast.makeText(context, message, duration).show();
			}
		});
	}

	protected static void activateTab(final int tabID) {
		uiHander.post(new Runnable() {
			@Override
			public void run() {
				baseActivity.getTabHost().setCurrentTab(tabID);
			}
		});
	}

	protected static Context getActivityContext() {
		return context;
	}

	protected static void setTitlebarDisabled() {
		if (baseActivity != null)
			baseActivity.changeTitlebarState(Color.RED, "Dynamix "
					+ DynamixService.getFrameworkVersion() + " is disabled");
	}

	protected static void setTitlebarEnabled() {
		if (baseActivity != null)
			baseActivity.changeTitlebarState(Color.rgb(0, 225, 50), "Dynamix "
					+ DynamixService.getFrameworkVersion() + " is enabled");
	}

	public static boolean isActivityVisible() {
		return activityVisible;
	}

	public static void activityResumed() {
		activityVisible = true;
	}

	public static void activityPaused() {
		activityVisible = false;
	}

	// AndroidExperimentation Classes
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the object we can use to
			// interact with the service. We are communicating with the
			// service using a Messenger, so here we get a client-side
			// representation of that from the raw IBinder object.
			mService = new Messenger(service);
			mBound = true;
			Log.i(TAG, "main service connected ok");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mBound = false;
		}
	};

	// listener to receive intents from service
	protected class ServiceIntentListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("dynamix_state")) {
				String state = intent.getExtras().getString("value");

				if (state.equals("connected")) {
					dTab.setDynamixConnected();
				} else if (state.equals("disconnected")) {
					dTab.setDynamixDisconnected();
				}
			} else if (intent.getAction().equals("job_state")) {
				String state = intent.getExtras().getString("value");
				jTab.setJobState(state);
			} else if (intent.getAction().equals("phone_id")) {
				String phoneId = intent.getExtras().getString("value");
				pTab.setPhoneId(phoneId);
			} else if (intent.getAction().equals("jobDependencies")) {
				String dependencies = intent.getExtras().getString("value");
				jTab.loaJobdDependencies(dependencies);
			} else if (intent.getAction().equals("internet_status")) {
				String internet_status = intent.getExtras().getString("value");
				rTab.setInternetStatus(internet_status);
				pTab.setInternetStatus(internet_status);
			} else if (intent.getAction().equals("job_report")) {
				String jobName = intent.getExtras().getString("value");
				rTab.jobToReport(jobName);
			} else if (intent.getAction().equals("job_name")) {
				String jobName = intent.getExtras().getString("value");
				jTab.commitJob(jobName);
			}
		}
	}

	// listener to receive intents from child tabs
	protected class tabIntentListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("connect_dynamix")) {
				sendMessage("connect_to_dynamix");
			} else if (intent.getAction().equals("disconnect_dynamix")) {
				sendMessage("disconnect_dynamix");
			} else if (intent.getAction().equals("stop_job")) {
				sendMessage("stop_job");
			} else if (intent.getAction().equals("start_job")) {
				sendMessage("start_job");
			} else if (intent.getAction().equals("sensors_permissions_changed")) {
				sendMessage("sensorsPermissionsChanged");
			}
		}
	}

	// send message to main service
	public void sendMessage(String message) {
		if (!mBound)
			return;

		Message msg = null;

		// Create and send a message to the service, using a supported 'what'
		// value
		if (message.equals("connect_to_dynamix")) {
			msg = Message.obtain(null,
					AndroidExperimentationService.MSG_CONNECT_TO_DYNAMIX, 0, 0);
		} else if (message.equals("disconnect_dynamix")) {
			msg = Message.obtain(null,
					AndroidExperimentationService.MSG_DISCONNECT_DYNAMIX, 0, 0);
		} else if (message.equals("stop_job")) {
			msg = Message.obtain(null,
					AndroidExperimentationService.MSG_STOP_JOB, 0, 0);
		} else if (message.equals("start_job")) {
			msg = Message.obtain(null,
					AndroidExperimentationService.MSG_START_JOB, 0, 0);
		} else if (message.equals("sensorsPermissionsChanged")) {
			msg = Message
					.obtain(null,
							AndroidExperimentationService.MSG_SENSORS_PERMISSIONS_CHANGED,
							0, 0);
		}

		try {
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	// ----------------------------------------------

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "Activity State: onCreate()");
		super.onCreate(savedInstanceState);
		context = this;
		// Set the Dynamix base activity so it can use our context
		DynamixService.setBaseActivity(this);
		// Request for the progress bar to be shown in the title
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);
		// setProgressBarVisibility(true);
		// Set our static reference
		baseActivity = this;
		// setContentView(R.layout.main);
		/*
		 * Setup the Tab UI Reference:
		 * http://developer.android.com/resources/tutorials
		 * /views/hello-tabwidget.html
		 */
		Resources res = getResources(); // Resource object to get Drawables
		tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab
		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, HomeActivity.class);
		Bundle b = new Bundle();
		b.putBoolean("fromTab", true);
		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("home")
				.setIndicator("Home", res.getDrawable(R.drawable.tab_home))
				.setContent(intent);
		tabHost.addTab(spec);
		intent = new Intent().setClass(this, PendingApplicationActivity.class);
		intent.putExtras(b);
		spec = tabHost
				.newTabSpec("pending")
				.setIndicator("Pending",
						res.getDrawable(R.drawable.tab_pending))
				.setContent(intent);
		tabHost.addTab(spec);
		intent = new Intent().setClass(this, PrivacyActivity.class);
		intent.putExtras(b);
		spec = tabHost
				.newTabSpec("privacy")
				.setIndicator("Privacy",
						res.getDrawable(R.drawable.tab_profiles))
				.setContent(intent);
		tabHost.addTab(spec);
		intent = new Intent().setClass(this, PluginsActivity.class);
		intent.putExtras(b);
		spec = tabHost
				.newTabSpec("plugins")
				.setIndicator("Plugins",
						res.getDrawable(R.drawable.tab_plugins))
				.setContent(intent);
		tabHost.addTab(spec);
		intent = new Intent().setClass(this, UpdatesActivity.class);
		intent.putExtras(b);
		spec = tabHost
				.newTabSpec("updates")
				.setIndicator("Updates",
						res.getDrawable(R.drawable.tab_updates))
				.setContent(intent);
		tabHost.addTab(spec);
		// Boot Dynamix
		DynamixService.boot(this, true, false, false);

		Resources ressources = getResources();
		TabHost tabHost = getTabHost();

		// profile tab
		Intent intentProfile = new Intent().setClass(this, profileTab.class);
		TabSpec tabSpecProfile = tabHost
				.newTabSpec("profile")
				.setIndicator("",
						ressources.getDrawable(R.drawable.ic_tab_profile))
				.setContent(intentProfile);

		// security tab
		Intent intentSecurity = new Intent().setClass(this, securityTab.class);
		TabSpec tabSpecSecurity = tabHost
				.newTabSpec("security")
				.setIndicator("",
						ressources.getDrawable(R.drawable.ic_tab_security))
				.setContent(intentSecurity);

		// dynamix tab
		Intent intentDynamix = new Intent().setClass(this, dynamixTab.class);
		TabSpec tabSpecDynamix = tabHost
				.newTabSpec("dynamix")
				.setIndicator("",
						ressources.getDrawable(R.drawable.ic_tab_dynamix))
				.setContent(intentDynamix);

		// jobs tab
		Intent intentJobs = new Intent().setClass(this, jobsTab.class);
		TabSpec tabSpecJobs = tabHost
				.newTabSpec("jobs")
				.setIndicator("",
						ressources.getDrawable(R.drawable.ic_tab_jobs))
				.setContent(intentJobs);

		// report tab
		Intent intentReports = new Intent().setClass(this, reportTab.class);
		TabSpec tabSpecReports = tabHost
				.newTabSpec("reports")
				.setIndicator("",
						ressources.getDrawable(R.drawable.ic_tab_reports))
				.setContent(intentReports);

		// add all tabs
		tabHost.addTab(tabSpecProfile);		
		tabHost.addTab(tabSpecSecurity);	
		tabHost.addTab(tabSpecDynamix);
		tabHost.addTab(tabSpecJobs);
		tabHost.addTab(tabSpecReports);
			
		
        tabHost.setCurrentTabByTag("profile");
  
        tabHost.setCurrentTabByTag("dynamix");
        tabHost.setCurrentTabByTag("jobs");
        tabHost.setCurrentTabByTag("reports");
        
 
		pTab = (profileTab) this.getLocalActivityManager().getActivity("profile");
		dTab = (dynamixTab) this.getLocalActivityManager().getActivity("dynamix");
		jTab = (jobsTab) this.getLocalActivityManager().getActivity("jobs");
		rTab = (reportTab) this.getLocalActivityManager().getActivity("reports");
		sTab = (securityTab) this.getLocalActivityManager().getActivity("security");
		tabIntentlistener = new tabIntentListener();
		serviceIntentlistener = new ServiceIntentListener();
		bindService(new Intent(this.context,
				AndroidExperimentationService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	/**
	 * Create the options menu for adjusting Dynamix settings.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.clear();
		// Setup Change Settings
		MenuItem item1 = menu.add(1, Menu.FIRST, Menu.NONE, "Change Settings");
		item1.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				startActivity(new Intent(BaseActivity.this,
						DynamixPreferenceActivity.class));
				return true;
			}
		});
		// Setup Default Settings
		MenuItem item3 = menu.add(3, Menu.FIRST + 1, Menu.NONE, "Shut Down");
		item3.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				// Present "Are You Sure" dialog box
				AlertDialog.Builder builder = new AlertDialog.Builder(
						BaseActivity.this);
				builder.setMessage("Shut Down Dynamix?")
						.setCancelable(false)
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										DynamixService.destroyFramework(true,
												false);
									}
								})
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				builder.create().show();
				return true;
			}
		});
		return true;
	}

	protected void changeTitlebarState(int color, String message) {
		View titleView = getWindow().findViewById(android.R.id.title);
		if (titleView != null) {
			ViewParent parent = titleView.getParent();
			if (parent != null && (parent instanceof View)) {
				View parentView = (View) parent;
				parentView.setBackgroundColor(color);
				setTitle(message);
			}
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DynamixService.setBaseActivity(null);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Check for a notification
		Intent i = intent;
		Bundle extras = i.getExtras();
		AndroidNotification notification = null;
		if (extras != null)
			notification = (AndroidNotification) extras
					.getSerializable("notification");
		// If we have a notification, set the current tab to the notification's
		// tab id, otherwise set it to 0 (Home)
		if (notification != null) {
			Log.i(TAG, "Opening tab: " + notification.getTabID());
			tabHost.setCurrentTab(notification.getTabID());
		} else
			tabHost.setCurrentTab(0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Update visibility
		activityResumed();
		/*
		 * Reset the titlebar state onResume, since our Activity's state will be
		 * lost if the app is paused.
		 */
		if (DynamixService.isFrameworkStarted())
			setTitlebarEnabled();
		else
			setTitlebarDisabled();

		// register intent listener for tabs
		if (!tabIntentListenerIsRegistered) {
			registerReceiver(tabIntentlistener, new IntentFilter("disconnect_dynamix"));
			registerReceiver(tabIntentlistener, new IntentFilter("connect_dynamix"));
			registerReceiver(tabIntentlistener, new IntentFilter("stop_job"));
			registerReceiver(tabIntentlistener, new IntentFilter("start_job"));
			registerReceiver(tabIntentlistener, new IntentFilter("WTF"));
			registerReceiver(tabIntentlistener, new IntentFilter("sensors_permissions_changed"));

			tabIntentListenerIsRegistered = true;
		}

		// register intent listener for MainService
		if (!serviceIntentListenerIsRegistered) {
			registerReceiver(serviceIntentlistener, new IntentFilter("hi"));
			registerReceiver(serviceIntentlistener, new IntentFilter("dynamix_state"));
			registerReceiver(serviceIntentlistener, new IntentFilter("job_state"));
			registerReceiver(serviceIntentlistener,	new IntentFilter("phone_id"));
			registerReceiver(serviceIntentlistener, new IntentFilter("jobDependencies"));
			registerReceiver(serviceIntentlistener, new IntentFilter("internet_status"));
			registerReceiver(serviceIntentlistener, new IntentFilter("job_report"));
			registerReceiver(serviceIntentlistener,	new IntentFilter("job_name"));
			serviceIntentListenerIsRegistered = true;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Update visibility
		activityPaused();
		// this.finish();
		// unregister intent listener
		if (tabIntentListenerIsRegistered)
		{
			unregisterReceiver(tabIntentlistener);
			tabIntentListenerIsRegistered = false;
		}
		
		// unregister intent listener
		if (serviceIntentListenerIsRegistered)
		{
			unregisterReceiver(serviceIntentlistener);
			serviceIntentListenerIsRegistered = false;
		}
		
	}

}
