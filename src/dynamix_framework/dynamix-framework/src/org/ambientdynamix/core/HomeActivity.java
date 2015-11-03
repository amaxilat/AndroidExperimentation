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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import eu.smartsantander.androidExperimentation.ActivityRecognitionService;
import eu.smartsantander.androidExperimentation.operations.AsyncReportOnServerTask;
import eu.smartsantander.androidExperimentation.operations.AsyncStatusRefreshTask;
import eu.smartsantander.androidExperimentation.service.RegistrationIntentService;
import eu.smartsantander.androidExperimentation.util.Constants;
import us.feras.mdv.MarkdownView;

import org.ambientdynamix.data.DynamixPreferences;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Home user interface, which shows the current authorized Dynamix applications along with their status. This UI also
 * provides a toggle button for activating/deactivating the Dynamix Framework.
 *
 * @author Darren Carlson
 */
public class HomeActivity extends ListActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    // Private data
    private final static String TAG = "HomeActivity";
    private static final int ENABLE_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;
    private static final int ACTIVITY_EDIT = 1;
    private static HomeActivity activity;
    private static boolean experimentationStatus = true;
    private static boolean registered = false;
    //private static HomeActivity me;
    public DynamixApplicationAdapter adapter;
    public ListView appList = null;
    private Timer refresher;
    public final Handler uiHandler = new Handler();
    private ToggleButton togglebutton = null;
    private boolean startedGcm = false;

    //SmartSantander
    public TextView phoneIdTv;
    public TextView expDescriptionTv;
    public TextView connectionStatus;
    public MarkdownView markdownView;

    private Button pendingSendButton;
    private MapFragment mMap;

    private GoogleApiClient mGoogleApiClient;
    private PendingIntent pIntent;

    private TextView pendingTextView;
    private TextView activityStatusTextView;

    // Create runnable for updating the UI
    final Runnable updateList = new Runnable() {
        public void run() {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    };


    // Refreshes the UI
    public static void refreshData() {
        if (activity != null)
            activity.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.refresh();
                }
            });
    }

    /**
     * Static method that allows callers to change the enabled state of the Dynamix Enable/Disable button. Note that
     * this method is only cosmetic, meaning that it does not actually call methods on the DynamixService.
     *
     * @param active
     */
    public static void setActiveState(boolean active) {
        if (activity != null) {
            // if (activity.togglebutton != null)
            //     activity.togglebutton.setChecked(active);
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        AlertDialog.Builder builder = null;
        final DynamixApplication app = (DynamixApplication) appList.getItemAtPosition(info.position);
        switch (item.getItemId()) {
            case ENABLE_ID:
                // Present "Are You Sure" dialog box
                builder = new AlertDialog.Builder(this);
                builder.setMessage(app.isEnabled() ? "Block " + app.getName() + "?" : "Unblock " + app.getName() + "?")
                        .setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        app.setEnabled(!app.isEnabled());
                        adapter.notifyDataSetChanged();
                        DynamixService.changeApplicationEnabled(app, app.isEnabled());
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
                return true;
            case DELETE_ID:
                // Present "Are You Sure" dialog box
                builder = new AlertDialog.Builder(this);
                builder.setMessage("Remove " + app.getName() + "?").setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                adapter.remove(app);
                                DynamixService.revokeSecurityAuthorization(app);
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Activity State: onCreate()");
        super.onCreate(savedInstanceState);
        // Set our static reference
        activity = this;
        setContentView(R.layout.home_tab);
        appList = getListView();
        appList.setClickable(true);

        final Intent activityRecognitionIntent = new Intent(this, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(getApplicationContext(), 0, activityRecognitionIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        markdownView = (MarkdownView) findViewById(R.id.markdownView);
        markdownView.setVisibility(View.INVISIBLE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        org.ambientdynamix.util.Log.i(TAG, "Connecting Google APIS...");
        mGoogleApiClient.connect();

        pendingSendButton = (Button) findViewById(R.id.send_pending_now);

        pendingSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncReportOnServerTask().execute();
                refresh();
            }
        });

        // Set an OnItemClickListener on the appList to support editing the
        // applications
        appList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                editApplication((DynamixApplication) appList.getItemAtPosition(position));
            }
        });

        // Setup the Dynamix Enable/Disable button
        pendingTextView = (TextView) findViewById(R.id.pending);
        activityStatusTextView = (TextView) findViewById(R.id.activity_status);
//        togglebutton = (ToggleButton) findViewById(R.id.DynamixActiveToggle);
//        togglebutton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (togglebutton.isChecked()) {
//                    DynamixService.startFramework();
//                } else {
//                    DynamixService.stopFramework();
//                }
//            }
//        });
        // Setup an state refresh timer, which periodically updates application
        // state in the appList
        refresher = new Timer(true);
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                uiHandler.post(updateList);
                ((HomeActivity) activity).refreshData();
            }
        };
        refresher.scheduleAtFixedRate(t, 0, 5000);
        registerForContextMenu(appList);

        //SmartSantander
        phoneIdTv = (TextView) this.findViewById(R.id.deviceId_label);
        expDescriptionTv = (TextView) this.findViewById(R.id.experiment_description);
        connectionStatus = (TextView) this.findViewById(R.id.connection_status);
        connectionStatus.setVisibility(View.INVISIBLE);
        appList.setVisibility(View.GONE);


        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_main));
        mMap.getMap().setMyLocationEnabled(true);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.app_list_contextmenu_title);
        menu.add(0, ENABLE_ID, 0, R.string.app_contextmenu_block);
        menu.add(0, DELETE_ID, 0, R.string.app_contextmenu_remove);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            Bundle extras = intent.getExtras();
            switch (requestCode) {
                case ACTIVITY_EDIT:
                    // Access the serialized app coming in from the Intent's Bundle
                    // extra
                    DynamixApplication app = (DynamixApplication) extras.getSerializable("app");
                    // Update the DynamixService with the updated application
                    DynamixService.updateApplication(app);
                    refresh();
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        try {
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Edit the application by creating an intent to launch the ApplicationSettingsActivity, making sure to send along
     * the application as a Bundle extra.
     *
     * @param app
     */
    private void editApplication(DynamixApplication app) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("pending", false);
        bundle.putSerializable("app", app);
        Intent i = new Intent(this, ContextFirewallActivity.class);
        i.putExtras(bundle);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    private void refresh() {


        if (DynamixService.isFrameworkInitialized()) {
            // Setup toggle button with proper state
            boolean dynamixEnabled = DynamixPreferences.isDynamixEnabled(this);
            //togglebutton.setChecked(dynamixEnabled);
            // If Dynamix is enabled, but the DynamixService is not running, then call startFramework
            if (dynamixEnabled && !DynamixService.isFrameworkStarted()) {
                DynamixService.startFramework();
            }
            // Load the registered application List box
            adapter = new DynamixApplicationAdapter(this, R.layout.icon_row, new ArrayList<DynamixApplication>(
                    DynamixService.SettingsManager.getAuthorizedApplications()), false);
            adapter.setNotifyOnChange(true);
            appList.setAdapter(adapter);

            Long storageSize = DynamixService.getDataStorageSize();
            if (storageSize > 0) {
                pendingSendButton.setVisibility(View.VISIBLE);
                pendingTextView.setVisibility(View.VISIBLE);
                pendingTextView.setText(String.format(getString(R.string.pending_messages_template), storageSize));
            } else {
                pendingSendButton.setVisibility(View.INVISIBLE);
                pendingTextView.setVisibility(View.INVISIBLE);
                pendingTextView.setText("");
            }

            activityStatusTextView.setText(Constants.activityStatus);

            if (!startedGcm
                    && DynamixService.isFrameworkInitialized()
                    && dynamixEnabled
                    && DynamixService.isFrameworkStarted()
                    && DynamixService.getPhoneProfiler().getPhoneId() != -1
                    && DynamixService.getExperiment() != null) {

                if (checkPlayServices()) {
                    startedGcm = true;
                    Intent intent = new Intent(this, RegistrationIntentService.class);
                    startService(intent);
                } else {
                    Log.w(TAG, "PlayServices not available!");
                }
            }

            if (experimentationStatus && !registered) {
                Log.i(TAG, "Add Location Listener");
                // Getting Current Location
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (location != null) {
                    LocationRequest mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                            .setFastestInterval(2 * 1000); // 1 second, in milliseconds
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


                    registered = true;
                    // Getting latitude of the current location
                    double latitude = location.getLatitude();

                    // Getting longitude of the current location
                    double longitude = location.getLongitude();

                    // Creating a LatLng object for the current location
                    LatLng latLng = new LatLng(latitude, longitude);

                    // Showing the current location in Google Map
                    mMap.getMap().moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.getMap().getUiSettings().setAllGesturesEnabled(false);
                    mMap.getMap().getUiSettings().setMyLocationButtonEnabled(false);
                }
            } else if (!experimentationStatus && registered) {
                Log.i(TAG, "Remove Location Listener");
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                registered = false;
            }
        }
        appList.setVisibility(View.GONE);//smartsantander
        AsyncStatusRefreshTask task = new AsyncStatusRefreshTask();
        task.execute(this);
    }


    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
//            if (apiAvailability.isUserResolvableError(resultCode)) {
////                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
////                        .show();
//            } else {
//                Log.i(TAG, "This device is not supported.");
//                finish();
//            }
            return false;
        }
        return true;
    }


    @Override
    public void onConnected(Bundle bundle) {
        org.ambientdynamix.util.Log.d(TAG, "Google activity recognition services connected");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 10000, pIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {
        org.ambientdynamix.util.Log.d(TAG, "Google activity recognition services connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        org.ambientdynamix.util.Log.d(TAG, "Google activity recognition services disconnected");
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i(TAG, "onLocationChanged:" + location.toString());
        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        // Creating a LatLng object for the current location
        LatLng latLng = new LatLng(latitude, longitude);

        // Showing the current location in Google Map
        mMap.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        mMap.getMap().getUiSettings().setAllGesturesEnabled(false);
        mMap.getMap().getUiSettings().setMyLocationButtonEnabled(false);
    }

    public static void changeStatus(boolean status) {
        experimentationStatus = status;
    }
}