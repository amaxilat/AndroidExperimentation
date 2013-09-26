package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.core.DynamixService;

import android.os.Handler;
import android.util.Log;
import eu.smartsantander.androidExperimentation.Constants;

public class Demon extends Thread implements Runnable {
	private AsyncExperimentTask pingExp = new AsyncExperimentTask();


	Handler handler;
	private final String TAG = this.getClass().getSimpleName();

	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (DynamixService.isInitialized()== true){
				pingExp.cancel(true);
				pingExp = new AsyncExperimentTask();
				pingExp.execute();				
			}						
			handler.postDelayed(this, Constants.EXPERIMENT_POLL_INTERVAL);
		}
	};
	
	public Demon( ) {
		handler=DynamixService.getUIHandler();	
	}

	
	public void run() {
		try {
			Log.d(TAG, "AndroidExperimentation Running");
			if (DynamixService.isDeviceRegistered() == false) {
				Log.d(TAG, "AndroidExperimentation Running Unregistered Device");
				return;
			}
			handler.postDelayed(runnable, Constants.EXPERIMENT_POLL_INTERVAL);
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "AndroidExperimentation:" + e.getMessage());
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
            if (found == false) {
                return false;
            }
        }
        return true;
    }

}
