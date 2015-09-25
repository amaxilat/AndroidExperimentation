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

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.bugsense.trace.ExceptionCallback;
import com.newrelic.agent.android.NewRelic;
import com.parse.Parse;

import eu.smartsantander.androidExperimentation.operations.AsyncConstantsTask;
import eu.smartsantander.androidExperimentation.operations.NotificationHQManager;
import eu.smartsantander.androidExperimentation.tabs.jobsTab;
import eu.smartsantander.androidExperimentation.tabs.reportTab;
import eu.smartsantander.androidExperimentation.tabs.securityTab;
import eu.smartsantander.androidExperimentation.tabs.statsTab;

import org.ambientdynamix.util.AndroidNotification;


/**
 * Base Activity for the Dynamix Framework UI. Responsible for hosting Tabs and
 * booting Dynamix, which launches the Dynamix background service.
 * <p/>
 * Note: This Activity is registered as the 'application' in the
 * AndroidManifest.xml, so it's started first.
 *
 * @author Darren Carlson
 * @see DynamixService
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
    private Boolean serviceIntentListenerIsRegistered = false;

    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in
    // Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in
    // Milliseconds
    protected LocationManager locationManager;
    NotificationHQManager noteManager = NotificationHQManager.getInstance();

    public static Resources myRes;

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
        //if (baseActivity != null)
        //    baseActivity.changeTitlebarState(Color.RED,
        //            myRes.getString(R.string.dynamix_enable_toggle_off));// "Experimentation is disabled"
        // );//"Experimentation"
        // + DynamixService.getFrameworkVersion()
        // + " is disabled");
    }

    protected static void setTitlebarEnabled() {
        //if (baseActivity != null)
        //    baseActivity.changeTitlebarState(Color.rgb(0, 225, 50),
         //           myRes.getString(R.string.dynamix_enable_toggle_on));// "Experimentation"
        // + DynamixService.getFrameworkVersion()
        // + " is enabled");
    }

    protected static void setTitlebarRestarting() {// smartsantander
        //if (baseActivity != null)
        //    baseActivity.changeTitlebarState(Color.rgb(100, 153, 0),
        //            myRes.getString(R.string.dynamix_restarting));// "Experimentation"
        // + DynamixService.getFrameworkVersion()
        // + " is enabled");
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

    // ----------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Activity State: onCreate()");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        myRes = getResources();
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
                .setIndicator("", res.getDrawable(R.drawable.tab_home))
                .setContent(intent);
        tabHost.addTab(spec);
        intent = new Intent().setClass(this, PendingApplicationActivity.class);
        intent.putExtras(b);
//        spec = tabHost
//                .newTabSpec("pending")
//                .setIndicator("Pending",
//                        res.getDrawable(R.drawable.tab_pending))
//                .setContent(intent);
//        // tabHost.addTab(spec);
//        intent = new Intent().setClass(this, PrivacyActivity.class);
//        intent.putExtras(b);
//
//        spec = tabHost
//                .newTabSpec("privacy")
//                .setIndicator("Privacy",
//                        res.getDrawable(R.drawable.tab_profiles))
//                .setContent(intent);

        // tabHost.addTab(spec);

        intent = new Intent().setClass(this, PluginsActivity.class);
        intent.putExtras(b);
        spec = tabHost.newTabSpec("plugins")
                .setIndicator("", res.getDrawable(R.drawable.tab_plugins))
                .setContent(intent);
        tabHost.addTab(spec);
//        intent = new Intent().setClass(this, UpdatesActivity.class);
//        intent.putExtras(b);
//        spec = tabHost
//                .newTabSpec("updates")
//                .setIndicator("Updates",
//                        res.getDrawable(R.drawable.tab_updates))
//                .setContent(intent);
        // tabHost.addTab(spec);
        // Boot Dynamix
        DynamixService.boot(this, true, false, false);

        Resources ressources = getResources();
        TabHost tabHost = getTabHost();

        // profile tab
        // Intent intentProfile = new Intent().setClass(this, profileTab.class);
        // TabSpec tabSpecProfile =
        // tabHost.newTabSpec("profile").setIndicator("",
        // ressources.getDrawable(R.drawable.ic_tab_profile)).setContent(intentProfile);

        // security tab
        Intent intentSecurity = new Intent().setClass(this, securityTab.class);
        TabSpec tabSpecSecurity = tabHost
                .newTabSpec("security")
                .setIndicator("",
                        ressources.getDrawable(R.drawable.ic_tab_security))
                .setContent(intentSecurity);

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

        // stats tab
        // TODO: create new content for statistics
        Intent intentStats = new Intent().setClass(this, statsTab.class);

        TabSpec tabSpecStats = tabHost
                .newTabSpec("stats")
                .setIndicator("",
                        ressources.getDrawable(R.drawable.ic_tab_stats))
                .setContent(intentStats);


        tabHost.addTab(tabSpecJobs);
        tabHost.addTab(tabSpecStats);
        tabHost.addTab(tabSpecReports);
        tabHost.addTab(tabSpecSecurity);
        //
        DynamixService.ConfigureLog4J();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MINIMUM_TIME_BETWEEN_UPDATES,
                MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, new MyLocationListener());

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

        // Setup Help Settings
        MenuItem item2 = menu.add(2, Menu.FIRST, Menu.NONE, "Help");
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
    }

    ;

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
        if (DynamixService.isFrameworkStarted()) {
            setTitlebarEnabled();

        } else {
            if (DynamixService.getRestarting() == true)
                setTitlebarRestarting();
            else
                setTitlebarDisabled();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Update visibility
        activityPaused();
        // this.finish();

    }


    private class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            String message = String.format(
                    "Lon %1$s Lat: %2$s",
                    location.getLongitude(), location.getLatitude());
            noteManager.postNotification(message);
        }

        public void onStatusChanged(String s, int i, Bundle b) {

        }

        public void onProviderDisabled(String s) {

        }

        public void onProviderEnabled(String s) {

        }

    }

}
