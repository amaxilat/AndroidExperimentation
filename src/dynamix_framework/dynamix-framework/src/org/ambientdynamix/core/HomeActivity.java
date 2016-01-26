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
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.ambientdynamix.data.DynamixPreferences;
import org.lucasr.twowayview.TwoWayView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import eu.smartsantander.androidExperimentation.fragment.SensorMeasurement;
import eu.smartsantander.androidExperimentation.fragment.SensorMeasurementAdapter;
import eu.smartsantander.androidExperimentation.operations.AsyncReportOnServerTask;
import eu.smartsantander.androidExperimentation.operations.AsyncStatusRefreshTask;
import eu.smartsantander.androidExperimentation.service.RegistrationIntentService;

/**
 * Home user interface, which shows the current authorized Dynamix applications along with their status. This UI also
 * provides a toggle button for activating/deactivating the Dynamix Framework.
 *
 * @author Darren Carlson
 */
public class HomeActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    // Private data
    private final static String TAG = "HomeActivity";
    private static final int ENABLE_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;
    private static final int ACTIVITY_EDIT = 1;
    private static HomeActivity activity;
    private static boolean experimentationStatus = true;
    private static boolean registered = false;
    public static Location location = null;
    public final Handler uiHandler = new Handler();
    private boolean startedGcm = false;

    //SmartSantander
    public TextView phoneIdTv;
    public TextView expDescriptionTv;


    private Button pendingSendButton;
    public MapFragment mMap;

    private GoogleApiClient mGoogleApiClient;

    public ArrayList<SensorMeasurement> sensorMeasurements;
    public SensorMeasurementAdapter sensorMeasurementAdapter;

    final LocationRequest mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(60 * 1000)        // 10 seconds, in milliseconds
            .setFastestInterval(30 * 1000); // 1 second, in milliseconds


    // Refreshes the UI
    public static void refreshData() {
        if (activity != null) {
            activity.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.refresh();
                }
            });
        }
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
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Activity State: onCreate()");
        super.onCreate(savedInstanceState);
        // Set our static reference
        activity = this;
        setContentView(R.layout.home_tab);

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_main));

        // Check if we were successful in obtaining the map.
        if (mMap == null) {
            // check if google play service in the device is not available or out-dated.
            GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            // nothing anymore, cuz android will take care of the rest (to remind user to update google play service).
        }

        // Construct the data source
        sensorMeasurements = new ArrayList<>();
        // Create the adapter to convert the array to views
        sensorMeasurementAdapter = new SensorMeasurementAdapter(this, sensorMeasurements);
        // Attach the adapter to a ListView
        final TwoWayView listView = (TwoWayView) findViewById(R.id.lvItems);
        listView.setOrientation(TwoWayView.Orientation.VERTICAL);
        listView.setPadding(0, 0, 0, 0);
        listView.setItemMargin(0);
        listView.setAdapter(sensorMeasurementAdapter);

        //Disable for now
        //final Intent activityRecognitionIntent = new Intent(this, ActivityRecognitionService.class);
        //pIntent = PendingIntent.getService(getApplicationContext(), 0, activityRecognitionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.i(TAG, "Connecting Google APIS...");
        mGoogleApiClient.connect();

        pendingSendButton = (Button) findViewById(R.id.send_pending_now);
        pendingSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncReportOnServerTask().execute();
            }
        });

        // Setup an state refresh timer, which periodically updates application
        // state in the appList
        final Timer refresher = new Timer(true);
        final TimerTask t = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "refresher");
                refreshData();
            }
        };
        refresher.scheduleAtFixedRate(t, 0, 5000);

        phoneIdTv = (TextView) this.findViewById(R.id.deviceId_label);

        expDescriptionTv = (TextView) this.findViewById(R.id.experiment_description);

        if (mMap.getMap() != null) {
            mMap.getMap().setMyLocationEnabled(true);
            mMap.getMap().getUiSettings().setAllGesturesEnabled(false);
            mMap.getMap().getUiSettings().setMyLocationButtonEnabled(false);
        }
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
            final Bundle extras = intent.getExtras();
            switch (requestCode) {
                case ACTIVITY_EDIT:
                    // Access the serialized app coming in from the Intent's Bundle
                    // extra
                    final DynamixApplication app = (DynamixApplication) extras.getSerializable("app");
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
    }

    private void refresh() {

        if (DynamixService.isFrameworkInitialized()) {
            // Setup toggle button with proper state
            boolean dynamixEnabled = DynamixPreferences.isDynamixEnabled(this);
            // If Dynamix is enabled, but the DynamixService is not running, then call startFramework
            if (dynamixEnabled && !DynamixService.isFrameworkStarted()) {
                DynamixService.startFramework();
            }

            final Long storageSize = DynamixService.getDataStorageSize();
            if (storageSize > 0) {
                pendingSendButton.setVisibility(View.VISIBLE);

                pendingSendButton.setText(String.format(getString(R.string.pending_messages_template), storageSize));
            } else {
                pendingSendButton.setVisibility(View.GONE);
            }


            if (experimentationStatus && !registered) {
                Log.i(TAG, "Add Location Listener");
                if (!mGoogleApiClient.isConnected()) {
                    //TODO : use this in a blocking connect mode - needed to do the below stuff
                    mGoogleApiClient.connect();
                } else {
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

                    // Getting Current Location
                    final Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location != null) {
                        registered = true;
                        HomeActivity.location = location;
                        updateMapLocation(location);
                        //Load organicity points
                        //new AsyncGetOrganicityMarkersTask().execute(this);
                    }
                }
            } else if (!experimentationStatus && registered) {
                Log.i(TAG, "Remove Location Listener");
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                registered = false;
            }

            if (!startedGcm && dynamixEnabled) {
                connect2Gcm();
            }
        }
        final AsyncStatusRefreshTask task = new AsyncStatusRefreshTask(this);
        task.execute();
    }

    private void connect2Gcm() {
        if (DynamixService.isFrameworkInitialized()
                && DynamixService.isFrameworkStarted()
                && DynamixService.getPhoneProfiler().getPhoneId() != -1
                && DynamixService.getExperiment() != null) {

            if (checkPlayServices()) {
                startedGcm = true;
                final Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            } else {
                Log.w(TAG, "PlayServices not available!");
            }
        }
    }


    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return resultCode == ConnectionResult.SUCCESS;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Google activity recognition services connected");
        //Disable for now
        //ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 10000, pIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google activity recognition services connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Google activity recognition services disconnected");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged:" + location.toString());
        HomeActivity.location = location;
    }

    private void updateMapLocation(final Location location) {
        updateMapLocation(location.getLatitude(), location.getLongitude());
    }

    public void updateMapLocation(final String latitude, final String longitude) {
        updateMapLocation(Double.parseDouble(latitude), Double.parseDouble(longitude));
    }

    private void updateMapLocation(final double latitude, final double longitude) {
        Log.i(TAG, "updateMapLocation:" + longitude + "/" + latitude);
        // Creating a LatLng object for the current location
        final LatLng latLng = new LatLng(latitude, longitude);

        try {
            // Showing the current location in Google Map
            mMap.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
        } catch (NullPointerException ignore) {
        }
    }

    public static void changeStatus(boolean status) {
        experimentationStatus = status;
    }


}