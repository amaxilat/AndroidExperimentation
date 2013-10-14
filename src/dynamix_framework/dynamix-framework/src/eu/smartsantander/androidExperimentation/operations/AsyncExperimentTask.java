package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.api.application.IdResult;
import org.ambientdynamix.core.DynamixService;
 

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

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
				manageExperiment();
				
				
				IdResult r;
				
				//do it for all plugins....
				r=DynamixService.dynamix.contextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.GpsPlugin", "org.ambientdynamix.contextplugins.GpsPlugin");
				
				
				//ping experiment....
				r=DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.ExperimentPlugin", "org.ambientdynamix.contextplugins.ExperimentPlugin",DynamixService.getReadingStorage().getBundle() );
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
	
	public String manageExperiment(){
		String jsonExperiment = "0";
		try {
			jsonExperiment = DynamixService.getCommunication().getExperiment(
					DynamixService.getPhoneProfiler().getPhoneId(),
					DynamixService.getPhoneProfiler().getSensorRules());
		} catch (Exception e) {
			e.printStackTrace();
			return "No experiment Fetched";
		}
		Log.i(TAG, jsonExperiment);
		if (jsonExperiment.equals("0")) {
			Log.i(TAG, "No experiment Fetched");
			DynamixService.removeExperiment();
			return "No experiment Fetched";
		} else {
			try {
				Gson gson = new Gson();
				Experiment experiment = (Experiment) gson.fromJson(jsonExperiment, Experiment.class);
				String[] smarDeps = DynamixService.getPhoneProfiler().getSensorRules().split(",");
				String[] expDeps = experiment.getSensorDependencies().split(",");			
				if (Constants.match(smarDeps, expDeps) == true) {
					int oldExpId=-1;	
					if (DynamixService.getExperiment()!=null){
						oldExpId=DynamixService.getExperiment().getId();
					}
					boolean flag=DynamixService.isExperimentInstalled(experiment.getContextType());
					if (experiment.getId()==oldExpId && flag==true){						
						Log.i(TAG, "Experiment still the same");
						return "Experiment still the same";						
					}
					String contextType = experiment.getContextType();
					String url = experiment.getUrl();
					Downloader downloader = new Downloader();
					try {
						downloader.DownloadFromUrl(url,experiment.getFilename()); 
						DynamixService.setExperiment(experiment);
					} catch (Exception e) {
						e.printStackTrace();
						if (DynamixService.isNetworkAvailable()==false){
							Toast.makeText(DynamixService.getAndroidContext(), "Please Check Internet Connecton!",	10000).show();
						}else{
							Toast.makeText(DynamixService.getAndroidContext(), "Please Check Internet Connecton!",	10000).show();
						}
						return "Failed to Download Experiment";
					}
					return "Experiment Commited";
				} else {
					Log.i(TAG, "Experiment violates Sensor Rules");
					return "Experiment violates Sensor Rules";
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "Exception in consuming experiment" + e.getMessage());
				return "Exception in consuming experiment";
			}
		}

	}
}

 
