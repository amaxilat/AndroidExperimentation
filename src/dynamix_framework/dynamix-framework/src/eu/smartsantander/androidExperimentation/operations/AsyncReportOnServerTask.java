package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.core.DynamixService;
import org.springframework.web.client.HttpClientErrorException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

public class AsyncReportOnServerTask extends AsyncTask<String, Void, String> {
	private final String TAG = this.getClass().getSimpleName();
	private boolean finished = false;
	private int counter=0;

	public AsyncReportOnServerTask() {
		finished = false;
	}

	@Override
	protected String doInBackground(String... params) {
		finished = false;
		while (DynamixService.getDataStorageSize() > 0){
			Pair<Long, String> value = DynamixService.getOldestExperimentalMessage();
			try {
				if (value.first != 0 && value.second != null&& value.second.length() > 0) {
					DynamixService.getCommunication().sendReportResults(value.second);//
					DynamixService.deleteExperimentalMessage(value.first);
					DynamixService.logToFile("SQLITE OFFLOAT:"+value.second);
					counter=0;
				}
			} catch (HttpClientErrorException e) {
				//ignore
				DynamixService.deleteExperimentalMessage(value.first);
				counter=0;
			} catch (Exception e) {
				// no communication do nothing
				if (counter>=2){
					break;
				}else{
					counter++;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {						 
						e1.printStackTrace();
					}
				}
			}
		}
		finished = true;
		return "AsyncReportOnServerTask Executed";
	}

	@Override
	protected void onPostExecute(String result) {
		finished = true;
	}

	@Override
	protected void onPreExecute() {
		finished = false;
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		finished = false;
	}

	@Override
	protected void onCancelled() {
		finished = true;
	}

	public boolean isFinished() {
		return this.finished;
	}

}
