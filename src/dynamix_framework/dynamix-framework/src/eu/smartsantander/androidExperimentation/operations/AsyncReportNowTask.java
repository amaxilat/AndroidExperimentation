package eu.smartsantander.androidExperimentation.operations;

import android.os.AsyncTask;
import android.util.Log;

import org.ambientdynamix.core.DynamixService;
import org.springframework.web.client.HttpClientErrorException;

public class AsyncReportNowTask extends AsyncTask<String, Void, String> {
    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected String doInBackground(String... params) {
        String message = params[0];
        try { //try to send to server, on fail save it in SQLite
            Log.i(TAG, "Offloading results...");
            Log.d(TAG, message);
            DynamixService.getCommunication().setLastMessage(message);
            DynamixService.getCommunication().sendReportResults(message);//
        } catch (HttpClientErrorException e) {
            //ignore
        } catch (Exception e) {
            DynamixService.addExperimentalMessage(message);
            Log.d(TAG, "Stored Message Count " + DynamixService.getDataStorageSize());
        }
        return "AndroidExperimentation Async Experiment Task Executed";
    }

    @Override
    protected void onPostExecute(String result) {
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }

    @Override
    protected void onCancelled() {
    }

}
