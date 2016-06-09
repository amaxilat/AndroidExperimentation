package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.core.DynamixService;

import android.os.Handler;
import android.util.Log;

import eu.smartsantander.androidExperimentation.util.Constants;

public class Demon extends Thread implements Runnable {
    private AsyncExpTask pingExp = new AsyncExpTask();
    private int counter = 0;
    private boolean started;
    final Handler handler;
    private final String TAG = this.getClass().getSimpleName();

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (DynamixService.isEnabled()) {
                if (!DynamixService.isDeviceRegistered()) {
                    DynamixService.getPhoneProfiler().register();
                } else if (DynamixService.isInitialized()) {
                    if (!pingExp.isStateActive() || counter > 30) {
                        counter = 0;
                        pingExp.cancel(true);
                        pingExp = new AsyncExpTask();
                        pingExp.execute();
                    }
                    counter++;
                }

            }
            handler.postDelayed(this, Constants.EXPERIMENT_POLL_INTERVAL);
        }
    };


    public Demon() {
        handler = DynamixService.getUIHandler();
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
            handler.postDelayed(runnable, Constants.EXPERIMENT_POLL_INTERVAL + 5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static boolean match(String[] smartphoneDependencies, String[] experimentDependencies) {
        for (String expDependency : experimentDependencies) {
            boolean found = false;
            for (String smartphoneDependency : smartphoneDependencies) {
                if (smartphoneDependency.equals(expDependency)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }


}