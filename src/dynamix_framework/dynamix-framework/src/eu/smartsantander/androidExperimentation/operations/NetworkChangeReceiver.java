package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.core.DynamixService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.util.Log;
import eu.smartsantander.androidExperimentation.Constants;

public class NetworkChangeReceiver extends BroadcastReceiver {
	private final String TAG = this.getClass().getSimpleName();
	  @Override
	  public void onReceive(final Context context, final Intent intent) {
		try{
			final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

			final android.net.NetworkInfo wifi =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    
		    if (wifi.isAvailable()) {
		    	AsyncReportOnServerTask reportT = new AsyncReportOnServerTask();
		    	reportT.execute();
		    }
		}catch(Exception e){
			e.printStackTrace();
			Log.i(TAG, "NetworkChangeReceiver" + e.getMessage());			
		}

	}

 
} 
