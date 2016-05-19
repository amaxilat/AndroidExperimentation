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

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import org.ambientdynamix.data.DynamixPreferences;
import org.ambientdynamix.util.AndroidNotification;

import java.security.SecureRandom;

import eu.smartsantander.androidExperimentation.tabs.InfoTab;
import eu.smartsantander.androidExperimentation.tabs.MessagesTab;
import eu.smartsantander.androidExperimentation.tabs.NewExperimentTab;
import eu.smartsantander.androidExperimentation.tabs.StatisticsTab;
import eu.smartsantander.androidExperimentation.util.Constants;
import eu.smartsantander.androidExperimentation.util.GenericDialogListener;
import eu.smartsantander.androidExperimentation.util.OrganicityOAuthDialog;

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
    private static final Handler uiHander = new Handler();
    private static Context context;
    public static final int PLUGINS_TAB_ID = 3;

    private TabHost tabHost = null;
    private static boolean activityVisible;

    public static Resources myRes;
    public static String access_token;
    private long nonce;


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
        final Resources res = getResources(); // Resource object to get Drawables
        tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        Intent intent; // Reusable Intent for each tab
        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, HomeActivity.class);
        final Bundle b = new Bundle();
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

        final TabHost tabHost = getTabHost();

        // profile tab
        // Intent intentProfile = new Intent().setClass(this, profileTab.class);
        // TabSpec tabSpecProfile =
        // tabHost.newTabSpec("profile").setIndicator("",
        // ressources.getDrawable(R.drawable.ic_tab_profile)).setContent(intentProfile);

        //tabHost.addTab(buildTab(ExperimentTab.class, R.drawable.ic_tab_jobs, "jobs"));
        tabHost.addTab(buildTab(NewExperimentTab.class, R.drawable.ic_tab_jobs, "jobs"));
        tabHost.addTab(buildTab(StatisticsTab.class, R.drawable.ic_tab_stats, "stats"));
        tabHost.addTab(buildTab(InfoTab.class, R.drawable.ic_tab_reports, "reports"));
//        tabHost.addTab(buildTab(DefaultSensingActivity.class, R.drawable.ic_tab_city, "defaults"));
        tabHost.addTab(buildTab(MessagesTab.class, R.drawable.ic_tab_security, "security"));
        //
        DynamixService.ConfigureLog4J();
    }

    private TabSpec buildTab(Class tabClass, int drawableRes, String name) {
        final Intent intent = new Intent().setClass(this, tabClass);
        final Drawable icon = getResources().getDrawable(drawableRes);
        return tabHost.newTabSpec(name).setIndicator("", icon).setContent(intent);
    }

    /**
     * Create the options menu for adjusting Dynamix settings.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();

        MenuItem itemPow = menu.add(0, Menu.FIRST, Menu.NONE, "Power");

        itemPow.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        boolean dynamixEnabled = DynamixPreferences.isDynamixEnabled(this);

        if (dynamixEnabled) {
            itemPow.setIcon(R.drawable.power_icon);
        } else {
            itemPow.setIcon(R.drawable.power_icon_off);
        }

        Log.i(TAG, "looking here....");
        itemPow.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (!askDynamixIsEnabled()) {
                    item.setIcon(R.drawable.power_icon);
                    DynamixService.startFramework();
                    HomeActivity.changeStatus(true);
                } else {
                    item.setIcon(R.drawable.power_icon_off);
                    DynamixService.stopFramework();
                    HomeActivity.changeStatus(false);
                }
                return true;
            }
        });


        // Setup Change Settings
        MenuItem item1 = menu.add(1, Menu.FIRST + 1, Menu.NONE, "Change Settings");
        final MenuItem item3 = menu.add(1, Menu.FIRST + 1, Menu.NONE, "Disconnect Organicity Account");
        final MenuItem item2 = menu.add(1, Menu.FIRST + 1, Menu.NONE, "Connect Organicity Account");

        item1.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(BaseActivity.this,
                        DynamixPreferenceActivity.class));
                return true;
            }
        });

        item2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                //NotifierHelper.displayToast(mContext, "onClick_fsqLogin", NotifierHelper.SHORT_TOAST);
                baseActivity.nonce = new SecureRandom().nextLong();
                String authRequestRedirect = Constants.ORGANICITY_APP_OAUTH_BASEURL + Constants.ORGANICITY_APP_OAUTH_URL
                        + "?client_id=" + Constants.ORGANICITY_APP_KEY
                        + "&redirect_uri=" + Constants.ORGANICITY_APP_CALLBACK_OAUTHCALLBACK
//                        + "&scope=user"
                        + "&response_type=id_token token"
                        + "&state=" + System.currentTimeMillis()
                        + "&nonce=" + nonce
//                        + "&display=touch"
                        ;
                Log.d(TAG, "authRequestRedirect->" + authRequestRedirect);

                CookieSyncManager.createInstance(context);
                new OrganicityOAuthDialog(context, authRequestRedirect
                        , new GenericDialogListener() {
                    public void onComplete(final Bundle values) {
                        Log.d(TAG, "onComplete->" + values);

                        try {
                            final SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("aaa", Context.MODE_PRIVATE);
                            final SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("access_token", BaseActivity.access_token);
                            editor.apply();
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                item3.setVisible(true);
                                item2.setVisible(false);
                            }
                        });

                    }

                    public void onError(String e) {
                        Log.d(TAG, "onError->" + e);
                    }

                    public void onCancel() {
                        Log.d(TAG, "onCancel()");
                    }
                }).show();


                return true;
            }
        });

        item3.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    final SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("aaa", Context.MODE_PRIVATE);
                    final SharedPreferences.Editor editor = sharedPref.edit();
                    editor.remove("access_token");
                    editor.apply();
                    item2.setVisible(true);
                    item3.setVisible(false);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                return false;
            }
        });

        final String accessToken = getApplicationContext().getSharedPreferences("aaa", MODE_PRIVATE).getString("access_token", null);
        if (accessToken == null) {
            item3.setVisible(false);
            item2.setVisible(true);
        } else {
            item3.setVisible(true);
            item2.setVisible(false);
        }


        //Setup Help Settings
//        MenuItem item2 = menu.add(2, Menu.FIRST + 2, Menu.NONE, "Help");
//        item2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem item) {
//                startActivity(new Intent(BaseActivity.this,
//                        HelpActivity.class));
//                return true;
//            }
//        });

        // Setup Default Settings
//        MenuItem item3 = menu.add(3, Menu.FIRST + 3, Menu.NONE, "Shut Down");
//        item3.setOnMenuItemClickListener(new OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem item) {
//                // Present "Are You Sure" dialog box
//                AlertDialog.Builder builder = new AlertDialog.Builder(
//                        BaseActivity.this);
//                builder.setMessage("Shut Down Dynamix?")
//                        .setCancelable(false)
//                        .setPositiveButton("Yes",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog,
//                                                        int id) {
//                                        DynamixService.destroyFramework(true,
//                                                false);
//                                    }
//                                })
//                        .setNegativeButton("No",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog,
//                                                        int id) {
//                                        dialog.cancel();
//                                    }
//                                });
//                builder.create().show();
//                return true;
//            }
//        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //DynamixService.setBaseActivity(null);
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

    private boolean askDynamixIsEnabled() {

        boolean dynamixEnabled = DynamixPreferences.isDynamixEnabled(this);

        return dynamixEnabled;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
