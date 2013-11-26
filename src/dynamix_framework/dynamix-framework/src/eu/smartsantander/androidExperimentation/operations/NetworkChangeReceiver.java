package eu.smartsantander.androidExperimentation.operations;

import com.bugsense.trace.BugSenseHandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {
	private static final String TAG = "NetworkChangeReceiver";
	private static AsyncReportOnServerTask reportT = null;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.i(TAG, "NetworkChangeReceiver Started");
		process(context);
	}

	public static synchronized void process(Context context) {
		try {
			final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (wifi.isAvailable()) {
				Log.i(TAG, "NetworkChangeReceiver: WIFI available");
				if (reportT == null || reportT.isFinished()) {
					if (reportT!=null) {
						reportT.cancel(true);
					}
					reportT = new AsyncReportOnServerTask();
					reportT.execute();
				}
			} else {
				Log.i(TAG, "NetworkChangeReceiver: WIFI NOT available");
			}
		} catch (Exception e) {
			e.printStackTrace();
			BugSenseHandler.sendException(e);
			Log.i(TAG, "NetworkChangeReceiver" + e.getMessage());
		}
	}

}
