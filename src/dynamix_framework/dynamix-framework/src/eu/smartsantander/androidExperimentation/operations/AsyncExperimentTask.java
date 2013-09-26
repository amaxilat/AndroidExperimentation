package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.api.application.IdResult;
import org.ambientdynamix.core.DynamixService;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

public class AsyncExperimentTask extends AsyncTask<String, Void, String> {
	private final String TAG = this.getClass().getSimpleName();
	
	public AsyncExperimentTask(){}
	 
	    
	@Override
	protected String doInBackground(String... params) {
		Log.i("AsyncExperimentTask", "Experiment Connecting...");
		if (DynamixService.sessionStarted==false){
			DynamixServiceListenerUtility.start();
	    }else{
			try {
				IdResult r=DynamixService.dynamix.contextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.GpsPlugin", "org.ambientdynamix.contextplugins.GpsPlugin");
				Log.i("contextRequest", r.getMessage());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
		return "AndroidExperimentation Async Experiment Task Executed";
	}

	@Override
	protected void onPostExecute(String result) {
		Log.i("AndroidExperimentation",
				"AndroidExperimentation Async Experiment Task Post Execute:"
						+ result);
 
	}

	@Override
	protected void onPreExecute() {
		Log.i("AndroidExperimentation",
				"AndroidExperimentation Async Experiment Task pre execute");
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		Log.i("AndroidExperimentation",
				"AndroidExperimentation Async Experiment Task  update progress");
	}

	@Override
	protected void onCancelled() {
		Log.i("AndroidExperimentation",
				"AndroidExperimentation Async Experiment Task cancelled");
	}
}



/*
if (runningJob.equals("-1")) {
	
	// if registered ask for experiment
	if (phoneProfiler.getPhoneId() != Constants.PHONE_ID_UNITIALIZED) {
		String jsonExperiment = "0";
		try {
			jsonExperiment = communication.getExperiment(
					phoneProfiler.getPhoneId(),
					sensorProfiler.getSensorRules());
		} catch (Exception e) {
			// TODO handle this
			e.printStackTrace();
			return "No experiment Fetched";
		}

		Log.i(TAG, jsonExperiment);
		if (jsonExperiment.equals("0")) {
			Log.i(TAG, "No experiment Fetched");
			return "No experiment Fetched";
		} else {
			try {
				Gson gson = new Gson();

				Experiment experiment = (Experiment) gson.fromJson(jsonExperiment, Experiment.class);
				String[] smarDeps = sensorProfiler.getSensorRules().split(",");
				String[] expDeps = experiment.getSensorDependencies().split(",");


				if (match(smarDeps,expDeps)==true) {
					String contextType = experiment.getContextType();
					String url = experiment.getUrl();

					Downloader downloader = new Downloader();
					try {
						downloader.DownloadFromUrl(url, experiment.getFilename());
					} catch (Exception e) {
						e.printStackTrace();
						return "Failed to Download Experiment";
					}

					editor.putString("runningJob", contextType);
					editor.putString("runningExperimentUrl",experiment.getUrl());
					editor.commit();
					sendThreadMessage("job_name:"+ experiment.getName());

					// tell to dynamix Framework to update its
					// repository
					updateDynamixRepository();
					scheduler.commitJob(contextType);
					return "Experiment Commited";
				} else {
					Log.i(TAG, "Experiment violates Sensor Rules");
					return "Experiment violates Sensor Rules";
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "Exception in consuming experiment" +e.getMessage());
				return "Exception in consuming experiment";
			}
		}
	} else {
		Log.i("AndroidExperimentation", "Ping failed");
		return "Ping failed";
	}
} else {
	if (scheduler.currentJob.jobState == null) {
		String runningExperimentUrl = pref.getString(
				"runningExperimentUrl", "-1");
		try {
			Constants.checkExperiment(runningJob, runningExperimentUrl);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendThreadMessage("job_name:" + runningJob);
		scheduler.commitJob(runningJob);
		return "Experiment Commited";
	}

}*/
