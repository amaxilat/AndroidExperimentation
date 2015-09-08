package eu.smartsantander.androidExperimentation.operations;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.core.DynamixService;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PhoneProfiler extends Thread implements Runnable {
    private SharedPreferences pref;
    private Editor editor;
    private Boolean started = false;
    private int PHONE_ID = Constants.PHONE_ID_UNITIALIZED;
    private List<Experiment> experiments = new ArrayList<Experiment>();
    private long totalReadingsProducedCounter;

    private final String TAG = this.getClass().getSimpleName();

    private boolean isInitialised = false;

    public PhoneProfiler() {
        this.PHONE_ID = Constants.PHONE_ID_UNITIALIZED;
    }


    public Boolean getStarted() {
        return started;
    }

    public void run() {
        startJob();
        started = true;
    }

    public void startJob() {
        try {
            Log.d(TAG, "running");
            Thread.sleep(2000);
            pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations", 0);
            editor = pref.edit();
            if ((pref.contains("phoneId"))) {
                this.PHONE_ID = pref.getInt("phoneId", 0);
                Log.d(TAG, "PhoneId:" + this.PHONE_ID);
                if (this.PHONE_ID < 1) {
                    setPhoneId(Constants.PHONE_ID_UNITIALIZED);
                }
            } else {
                setPhoneId(Constants.PHONE_ID_UNITIALIZED);
            }

            if (pref.contains("totalReadingsProducedCounter")) {
                this.totalReadingsProducedCounter = pref.getLong("totalReadingsProducedCounter", 0);
                if (this.totalReadingsProducedCounter < 0)
                    this.totalReadingsProducedCounter = 0;
            } else {
                this.totalReadingsProducedCounter = 0;
            }
            editor.commit();
            isInitialised = true;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void register() {

        if (!isInitialised) {
            startJob();
        }

        if (DynamixService.isDeviceRegistered())
            return;
        int phoneId = Constants.PHONE_ID_UNITIALIZED;
        int serverPhoneId;

        try {
            Log.d(TAG, String.valueOf(phoneId));
            final TelephonyManager tm = (TelephonyManager) DynamixService.getAndroidContext().getSystemService(Context.TELEPHONY_SERVICE);

            final String tmDevice, tmSerial, androidId;
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();
            androidId = "" + android.provider.Settings.Secure.getString(DynamixService.getAndroidContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

            UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
            String deviceId = deviceUuid.toString();

            serverPhoneId = DynamixService.getCommunication().registerSmartphone(deviceId.hashCode(), getSensorRules());
            if (serverPhoneId <= 0)
                serverPhoneId = Constants.PHONE_ID_UNITIALIZED;
            else {
                DynamixService.getPhoneProfiler().setPhoneId(serverPhoneId);
                setLastOnlineLogin();
            }


        } catch (Exception e) {
            DynamixService.getPhoneProfiler().setPhoneId(Constants.PHONE_ID_UNITIALIZED);
        }
    }

    public int getPhoneId() {
        return this.PHONE_ID;
    }

    public void setPhoneId(int PHONE_ID) {
        this.PHONE_ID = PHONE_ID;
        if (editor == null || pref == null) {
            pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations", 0);
            editor = pref.edit();
        }
        editor.putInt("phoneId", this.PHONE_ID);
        editor.putString("experiment", "");
        editor.putLong("lastOnlineLoginDate", (new Date()).getTime());
        editor.putLong("totalReadingsProducedCounter", 0);
        editor.commit();
    }

    public void savePrefs() {
        if (editor == null || pref == null) {
            pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations", 0);
            editor = pref.edit();
        }
        //editor.putInt("phoneId", this.PHONE_ID);
        String experimentsJson = (new Gson()).toJson(experiments);
        editor.putString("experiments", experimentsJson);
        editor.putLong("lastOnlineLoginDate", (new Date()).getTime());
        editor.putLong("totalReadingsProducedCounter", totalReadingsProducedCounter);
        editor.commit();
    }


    public String getSensorRules() {
        String sensorRules = "";
        for (ContextPluginInformation plugin : DynamixService.getAllContextPluginInfo()) {
            if (plugin.getInstallStatus() == PluginInstallStatus.INSTALLED) {
                sensorRules = sensorRules + plugin.getPluginId() + ",";
            }

        }
        return sensorRules;
    }


    public void experimentPush(Experiment exp) {
        if (editor == null) {
            pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations", 0);
            editor = pref.edit();
        }
        String experimentsJson = pref.getString("experiments", "");
        if (experimentsJson != null && experimentsJson.length() > 0) {
            Type listType = new TypeToken<ArrayList<Experiment>>() {
            }.getType();
            experiments = (new Gson()).fromJson(experimentsJson, listType);
        }
        if (experiments == null) {
            experiments = new ArrayList<Experiment>();
        }
        experiments.add(exp);
        experimentsJson = (new Gson()).toJson(experiments);
        editor.putString("experiments", experimentsJson);
        editor.commit();
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    // keep stats for total time connected to the service

    public void setLastOnlineLogin() {

        Date dat = new Date();

        if (editor == null) {
            pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations", 0);
            editor = pref.edit();
        }

        editor.putLong("lastOnlineLoginDate", dat.getTime());
        editor.commit();

    }

    public Date getLastOnlineLogin() {

        Date lastLoginDate;

        if (editor == null) {
            pref = DynamixService.getAndroidContext().getApplicationContext().getSharedPreferences("SmartSantanderConfigurations", 0);
        }

        lastLoginDate = new Date(pref.getLong("lastOnlineLoginDate", 0));

        return lastLoginDate;
    }

    public void incMsgCounter() {
        this.totalReadingsProducedCounter++;
    }

    public Long getTotalReadingsProduced() {
        return this.totalReadingsProducedCounter;
    }
}
