package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.core.DynamixService;

import android.os.AsyncTask.Status;
import android.os.Handler;
import android.util.Log;
import eu.smartsantander.androidExperimentation.Constants;

public class Demon extends Thread implements Runnable {
	private AsyncExperimentTask pingExp = new AsyncExperimentTask();

	private Boolean started=false;
	Handler handler;
	private final String TAG = this.getClass().getSimpleName();

	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (DynamixService.isEnabled()==true){
				if (DynamixService.isDeviceRegistered() == false) {
					Log.d(TAG, "AndroidExperimentation Running Unregistered Device");
					DynamixService.getPhoneProfiler().register();
				}else if (DynamixService.isInitialized()== true){
				if (pingExp.getStatus().equals(Status.FINISHED)==true ||pingExp.getStatus().equals(Status.PENDING)==true){
							pingExp.cancel(true);
							pingExp = new AsyncExperimentTask();
							pingExp.execute();	
					}
				}				
			}
			handler.postDelayed(this, Constants.EXPERIMENT_POLL_INTERVAL);
		}
	};
	
	public Demon( ) {
		handler=DynamixService.getUIHandler();	
	}

	public Boolean getStarted(){
		return started;
	}
	public void run() {		
		startJob();
		started=true;		
	}
	
	
	public void startJob() {
		try {
			Log.d(TAG, "AndroidExperimentation Running");
			handler.postDelayed(runnable, Constants.EXPERIMENT_POLL_INTERVAL+5000);
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
