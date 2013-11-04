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
	private boolean stateActive=false;
	
	public AsyncExperimentTask(){}
	 
	public boolean isStateActive(){
		return stateActive;
	}
	    
	@Override
	protected String doInBackground(String... params) {
		this.stateActive=true;
		Log.i("AsyncExperimentTask", "Experiment Connecting...");
		if (DynamixService.sessionStarted==false){
			DynamixServiceListenerUtility.start();
	    }else{
			try {

				
				
				IdResult r;
				
				//do it for all plugins....
				r=DynamixService.dynamix.contextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.GpsPlugin", "org.ambientdynamix.contextplugins.GpsPlugin");
				r=DynamixService.dynamix.contextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.WifiScanPlugin", "org.ambientdynamix.contextplugins.WifiScanPlugin");
				r=DynamixService.dynamix.contextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.NoiseLevelPlugin", "org.ambientdynamix.contextplugins.NoiseLevelPlugin");
				
				if(DynamixService.getExperiment()!=null){
					boolean flag=DynamixService.isExperimentInstalled("org.ambientdynamix.contextplugins.ExperimentPlugin");
					if(flag==false)
						DynamixService.startExperiment();
					
				}
				//ping experiment....
				r=DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.ExperimentPlugin", "org.ambientdynamix.contextplugins.ExperimentPlugin",DynamixService.getReadingStorage().getBundle() );
				Log.i("contextRequest", r.getMessage());
			
				try{
					manageExperiment();
				}catch(Exception e){
					this.stateActive=false;
					return e.getMessage();
				}
				
				this.stateActive=false;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
		this.stateActive=false;
		return "AndroidExperimentation Async Experiment Task Executed";
	}

	@Override
	protected void onPostExecute(String result) {
		Log.i("AndroidExperimentation","AndroidExperimentation Async Experiment Task Post Execute:"	+ result);
		this.stateActive=false;
	}

	@Override
	protected void onPreExecute() {
		Log.i("AndroidExperimentation",	"AndroidExperimentation Async Experiment Task pre execute");
		this.stateActive=true;
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		Log.i("AndroidExperimentation",	"AndroidExperimentation Async Experiment Task  update progress");
		this.stateActive=true;
	}

	@Override
	protected void onCancelled() {
		Log.i("AndroidExperimentation",				"AndroidExperimentation Async Experiment Task cancelled");
		this.stateActive=false;
	}
	
	public String manageExperiment() throws Exception{
		if (DynamixService.getExperiment()!=null){
			if(DynamixService.getExperiment().getToTime()!=null && DynamixService.getExperiment().getToTime()<System.currentTimeMillis()){
				//DynamixService.removeExperiment();
			}
		}
		String jsonExperiment = "0";
		try {
			jsonExperiment = DynamixService.getCommunication().getExperiment(
					DynamixService.getPhoneProfiler().getPhoneId(),
					DynamixService.getPhoneProfiler().getSensorRules());
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Failed to fetch Experiment Info");
		}
		Log.i(TAG, jsonExperiment);
		if (jsonExperiment.equals("0")) {
			Log.i(TAG, "No experiment Fetched");
			DynamixService.removeExperiment();
			throw new Exception( "No experiment Fetched");	
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
						throw new Exception("Experiment still the same");						
					}
					String url = experiment.getUrl();
					Downloader downloader = new Downloader();
					try {
						downloader.DownloadFromUrl(url,experiment.getFilename());
						DynamixService.removeExperiment();
						IdResult r = DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback,"org.ambientdynamix.contextplugins.ExperimentPlugin", "org.ambientdynamix.contextplugins.ExperimentPlugin",DynamixService.getReadingStorage().getBundle() );
						DynamixService.setExperiment(experiment);
						DynamixService.startExperiment();
						DynamixService.stopFramework();
						Thread.sleep(5000);
						DynamixService.startFramework();
						Thread.sleep(10000);
						DynamixServiceListenerUtility.start();
					} catch (Exception e) {
						e.printStackTrace();
						if (DynamixService.isNetworkAvailable()==false){
							Toast.makeText(DynamixService.getAndroidContext(), "Please Check Internet Connecton!",	10000).show();
						}else{
							Toast.makeText(DynamixService.getAndroidContext(), "Please Check Internet Connecton!",	10000).show();
						}
						throw new Exception("Failed to Download Experiment");
					}
					
					Toast.makeText(DynamixService.getAndroidContext(), "Experiment Pushed",	8000).show();
					return "Experiment Commited";
				} else {
					Log.i(TAG, "Experiment violates Sensor Rules");
					throw new Exception("Experiment violates Sensor Rules");
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "Exception in consuming experiment" + e.getMessage());
				throw new Exception( "Exception in consuming experiment");
			}
		}

	}
}

 
