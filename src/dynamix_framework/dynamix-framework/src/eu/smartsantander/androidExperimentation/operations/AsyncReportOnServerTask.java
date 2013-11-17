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
import android.util.Pair;
import android.widget.Toast;

public class AsyncReportOnServerTask extends AsyncTask<String, Void, String> {
	private final String TAG = this.getClass().getSimpleName();
	
	public AsyncReportOnServerTask(){}
	 
	 
	    
	@Override
	protected String doInBackground(String... params) {	
		Log.i("AsyncReportOnServerTask", "Experiment Connecting...");
		 		
	  while(DynamixService.getDataStorageSize()>0)
		try{
			Pair<Long,String> value=DynamixService.getOldestExperimentalMessage();
			if (value.first!=0 && value.second!=null && value.second.length()>0){
				DynamixService.getCommunication().sendReportResults(value.second);//
				DynamixService.deleteExperimentalMessage(value.first);
			}			
		}catch(Exception e){
			//no communication do nothing
			Log.i("AsyncReportOnServerTask", "Experiment Reporting Exception:"+e.getMessage());
			
		}		
		return "AsyncReportOnServerTask Executed";
	}

	@Override
	protected void onPostExecute(String result) {
		Log.i("AsyncReportOnServerTask","AsyncReportOnServerTask   Task Post Execute:"	+ result);
	}

	@Override
	protected void onPreExecute() {
		Log.i("AsyncReportOnServerTask",	"AsyncReportOnServerTask Task pre execute");
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		Log.i("AndroidExperimentation",	"AsyncReportOnServerTask Task  update progress");
	}

	@Override
	protected void onCancelled() {
		Log.i("AsyncReportOnServerTask", "AsyncReportOnServerTask Task cancelled");
	}
		
}

 
