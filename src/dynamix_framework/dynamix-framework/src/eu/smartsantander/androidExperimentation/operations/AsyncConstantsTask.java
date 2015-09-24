package eu.smartsantander.androidExperimentation.operations;

import android.os.AsyncTask;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.ambientdynamix.util.Log;

import eu.smartsantander.androidExperimentation.Constants;

public class AsyncConstantsTask extends AsyncTask<String, Void, Integer> {
    private static final String CONSTANTS_CLASS_NAME = "Constants";
    private static final String URL_KEY = "url";
    private final String TAG = this.getClass().getSimpleName();
    private boolean stateActive = false;

    @Override
    protected Integer doInBackground(String... params) {
        try {
            final ParseQuery q = new ParseQuery(CONSTANTS_CLASS_NAME);
            final ParseObject constantsObject = q.getFirst();
            final String url = constantsObject.getString(URL_KEY);
            Log.i(TAG, "url:" + url);
            Constants.URL = url;
            return 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 1;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
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
