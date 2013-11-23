package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.core.DynamixService;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

public class AsyncReportOnServerTask extends AsyncTask<String, Void, String> {
	private final String TAG = this.getClass().getSimpleName();
	private boolean finished = false;

	public AsyncReportOnServerTask() {
		finished = false;
	}

	@Override
	protected String doInBackground(String... params) {
		finished = false;
		Log.i("AsyncReportOnServerTask", "Offloading Data Started...");
		while (DynamixService.getDataStorageSize() > 0){
			try {
				Pair<Long, String> value = DynamixService.getOldestExperimentalMessage();
				if (value.first != 0 && value.second != null&& value.second.length() > 0) {
					DynamixService.getCommunication().sendReportResults(value.second);//
					DynamixService.deleteExperimentalMessage(value.first);
				}
			} catch (Exception e) {
				// no communication do nothing
				Log.i("AsyncReportOnServerTask","Experiment Reporting Exception:" + e.getMessage());

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
