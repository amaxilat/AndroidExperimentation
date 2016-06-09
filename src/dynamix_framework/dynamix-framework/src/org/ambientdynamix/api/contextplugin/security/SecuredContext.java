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
package org.ambientdynamix.api.contextplugin.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;

/**
 * Secured version of an Android Context, which is provided to ContextPlugins during runtime. A SecuredContext is
 * configured with a set of Permissions that are used to guard access to critical resources, such as Android system
 * services. <br>
 *
 * @author Darren Carlson
 */
public class SecuredContext extends Context {
    // Private data
    private final String TAG = this.getClass().getSimpleName();
    private Context c;
    private volatile Set<Permission> permissions;
    private volatile boolean permissionCheckingEnabled = true;
    private ClassLoader classLoader;
    private Looper plugLooper;
    private LocationManager locationManager;
    private SecuredSensorManager ssm;
    private Handler mainThreadHandler;
    private final List<BroadcastReceiver> receivers = new ArrayList<>();
    private BluetoothManager sbm;

    /**
     * Creates a SecuredContext that allows all permissions. Caution: This allows access to all Android services!
     *
     * @param c           The Context to secure.
     * @param plugLooper  The Looper for the plug-in
     * @param classLoader The plug-in's class-loader
     * @param allowAll    True if the SecuredContext should allow all permissions; false otherwise.
     */
    public SecuredContext(Context c, Handler mainThreadHandler, Looper plugLooper, ClassLoader classLoader,
                          boolean permissionCheckingEnabled) {
        this.c = c;
        this.plugLooper = plugLooper;
        this.mainThreadHandler = mainThreadHandler;
        this.classLoader = classLoader;
        this.permissionCheckingEnabled = permissionCheckingEnabled;
    }

    /**
     * Creates a SecuredContext with a specified set of permissions.
     *
     * @param c           The Context to secure.
     * @param plugLooper  The Looper for the plug-in
     * @param classLoader The plug-in's class-loader
     * @param permissions The permissions to add initially.
     */
    public SecuredContext(Context c, Handler mainThreadHandler, Looper plugLooper, ClassLoader classLoader,
                          Set<Permission> permissions) {
        if (c == null || mainThreadHandler == null || plugLooper == null)
            throw new RuntimeException("Missing Parameters");
        this.c = c;
        this.plugLooper = plugLooper;
        this.mainThreadHandler = mainThreadHandler;
        this.classLoader = classLoader;
        this.permissions = permissions;
        permissionCheckingEnabled = true;
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        // Not allowed in SecuredContext
        return false;
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        // Not allowed in SecuredContext
        return PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        // Not allowed in SecuredContext
        return 0;
    }

    @Override
    public int checkCallingPermission(String permission) {
        // Not allowed in SecuredContext
        return PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        // Not allowed in SecuredContext
        return 0;
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        // Not allowed in SecuredContext
        return PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        // Not allowed in SecuredContext
        return 0;
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid,
                                  int modeFlags) {
        // Not allowed in SecuredContext
        return 0;
    }

    /**
     * Clears all permissions.
     */
    public void clearAllPermissions() {
        this.permissions = null;
    }

    @Override
    public void clearWallpaper() throws IOException {
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws NameNotFoundException {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return null;
    }

    @Override
    public Context createDisplayContext(Display display) {
        return null;
    }

    @Override
    public String[] databaseList() {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public boolean deleteDatabase(String name) {
        // Not allowed in SecuredContext
        return false;
    }

    @Override
    public boolean deleteFile(String name) {
        // Not allowed in SecuredContext
        return false;
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
        // Not allowed in SecuredContext
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        // Not allowed in SecuredContext
    }

    @Override
    public void enforceCallingPermission(String permission, String message) {
        // Not allowed in SecuredContext
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        // Not allowed in SecuredContext
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
        // Not allowed in SecuredContext
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        // Not allowed in SecuredContext
    }

    @Override
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid,
                                     int modeFlags, String message) {
        // Not allowed in SecuredContext
    }

    @Override
    public String[] fileList() {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public Context getApplicationContext() {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        // Not allowed in SecuredContext
        return c.getApplicationInfo();
    }

    @Override
    public AssetManager getAssets() {
        // Not allowed in SecuredContext
        return c.getAssets();
    }

    @Override
    public File getCacheDir() {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public ContentResolver getContentResolver() {
        return c.getContentResolver();
    }

    @Override
    public File getDatabasePath(String name) {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public File getDir(String name, int mode) {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public File getFilesDir() {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public File getFileStreamPath(String name) {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public Looper getMainLooper() {
        // return c.getMainLooper();
        return null;
    }

    @Override
    public PackageManager getPackageManager() {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public String getPackageName() {
        // return c.getPackageName();
        return null;
    }

    @Override
    public Resources getResources() {
        // Return a new SecuredResources object
        return new SecuredResources(c.getAssets(), c.getResources().getDisplayMetrics(), c.getResources()
                .getConfiguration());
        // return c.getResources();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return null;
    }

    /**
     * Returns an Android system service for the specified serviceName, or null if the service is not found. System
     * services are only available to plug-ins that hold proper permissions. Note that plug-ins are NEVER allowed to
     * access the SensorManager directly. Instead, plug-ins are provided a SecuredSensorManager instead (if allowed).
     *
     * @see SecuredSensorManager
     */
    private Object speechRecognizerLock = new Object();

    @Override
    public synchronized Object getSystemService(String serviceName) {
        Log.v(TAG, "getSystemService for: " + serviceName);
        if (checkPermission(serviceName)) {
            // Object service = null;
            if (serviceName.equalsIgnoreCase(Service.SENSOR_SERVICE)) {
                // Only create a SecuredSensorManager once!
                if (ssm == null) {
                    ssm = new SecuredSensorManager((SensorManager) c.getSystemService(serviceName), plugLooper);
                }
                return ssm;
            } else if (serviceName.equalsIgnoreCase(Service.LOCATION_SERVICE)) {
                // Only create a SecuredSensorManager once!
                if (locationManager == null) {
                    locationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
                }
                return locationManager;
            } else if (serviceName.equalsIgnoreCase(Service.BLUETOOTH_SERVICE)) {
                // Only create a SecuredSensorManager once!
                if (sbm == null) {
                    sbm = (BluetoothManager) c.getSystemService(Service.BLUETOOTH_SERVICE);
                }
                return sbm;
            } else {
                return c.getSystemService(serviceName);
            }
        }
        return null;
    }

    @Override
    public Theme getTheme() {
        // needed
        return c.getTheme();
    }

    @Override
    public Drawable getWallpaper() {
        return null;
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return c.getWallpaperDesiredMinimumHeight();
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return c.getWallpaperDesiredMinimumWidth();
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        // Not allowed in SecuredContext
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return null;
    }

    @Override
    public Drawable peekWallpaper() {
        // return c.peekWallpaper();
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        if (checkPermission(Permissions.MANAGE_BROADCAST_RECEIVERS)) {
            synchronized (receivers) {
                if (!receivers.contains(receiver))
                    receivers.add(receiver);
            }
            return c.registerReceiver(receiver, filter);
        }
        // Return null if BroadcastReceiver was not registered
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission,
                                   Handler scheduler) {
        if (checkPermission(Permissions.MANAGE_BROADCAST_RECEIVERS)) {
            synchronized (receivers) {
                if (!receivers.contains(receiver))
                    receivers.add(receiver);
            }
            return c.registerReceiver(receiver, filter, broadcastPermission, scheduler);
        }
        // Return null if BroadcastReceiver was not registered
        return null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        /*
         * We need to always allow unregisterReceiver, since some plug-ins may need to unregister receivers in order to
		 * clean up state when shutting down. If a user remove permission for this method, plug-ins would never be able
		 * to shut down properly. In terms of security, the caller needs a reference to a valid BroadcastReceiver, so
		 * it's probably not dangerous to allow.
		 */
        try {
            receivers.remove(receiver);
            c.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.w(TAG, "Exception during unregisterReceiver: " + e);
        }
    }

    /**
     * Handles securely removing listeners and broadcast receivers that may have been registered with various managers.
     * We need this method because each manager may not be accessible due to privacy settings.
     */
    public synchronized void removeAllListeners() {
        if (ssm != null) {
            ssm.removeAllListeners();
        }
        synchronized (receivers) {
            for (BroadcastReceiver receiver : receivers)
                unregisterReceiver(receiver);
        }
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        // Not allowed in SecuredContext
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {

    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {

    }

    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {

    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        // Not allowed in SecuredContext
    }

    @Override
    public void sendBroadcast(Intent intent) {
        // Not allowed in SecuredContext
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        // Not allowed in SecuredContext
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        // Not allowed in SecuredContext
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
                                     Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        // Not allowed in SecuredContext
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {

    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {

    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {

    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        // Not allowed in SecuredContext
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler,
                                           int initialCode, String initialData, Bundle initialExtras) {
        // Not allowed in SecuredContext
    }

    @Override
    public void setTheme(int resid) {
        // c.setTheme(resid);
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws IOException {
        // c.setWallpaper(bitmap);
    }

    @Override
    public void setWallpaper(InputStream data) throws IOException {
        // c.setWallpaper(BitmapFactory.decodeStream(data));
    }

    @Override
    public void startActivity(Intent intent) {
        // Not allowed in SecuredContext
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {

    }

    @Override
    public void startActivities(Intent[] intents) {

    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {

    }

    @Override
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        // Not allowed in SecuredContext
        return false;
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues,
                                  int extraFlags) throws SendIntentException {
        // Not allowed in SecuredContext
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws SendIntentException {

    }

    @Override
    public ComponentName startService(Intent service) {
        // Not allowed in SecuredContext
        return null;
    }

    @Override
    public boolean stopService(Intent service) {
        // Not allowed in SecuredContext
        return false;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        // Not allowed in SecuredContext
    }

    /**
     * Replaces the set of permissions with the incoming set. Note that old permissions are not maintained.
     *
     * @param permissions The new set of permissions to apply.
     */
    public void updatePermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    private boolean checkPermission(String permString) {
        if (false && permissionCheckingEnabled) {
            for (Permission p : permissions) {
                if (p.getPermissionString().equalsIgnoreCase(permString) && p.isPermissionGranted()) {
                    return true;
                }
            }
        } else {
            return true;
        }
        Log.w(TAG, "Permission NOT granted: " + permString);
        return false;
    }

    @Override
    public File getExternalCacheDir() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getExternalFilesDir(String type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getObbDir() {
        return null;
    }

    @Override
    public String getPackageCodePath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPackageResourcePath() {
        // TODO Auto-generated method stub
        return null;
    }


}
