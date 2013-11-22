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
	private static AsyncReportOnServerTask reportT = null;
	  @Override
	  public void onReceive(final Context context, final Intent intent) {
		  Log.i(TAG, "NetworkChangeReceiver Started");
		  try{
			final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			final android.net.NetworkInfo wifi =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    
		    if (wifi.isAvailable()) {
		    	Log.i(TAG, "NetworkChangeReceiver: WIFI available");
		    	if (reportT==null || reportT.isFinished()){		    		
		    		reportT = new AsyncReportOnServerTask();
		    		reportT.execute();
		    	}
		    }else{
		    	Log.i(TAG, "NetworkChangeReceiver: WIFI NOT available");
		    }
		}catch(Exception e){
			e.printStackTrace();
			Log.i(TAG, "NetworkChangeReceiver" + e.getMessage());			
		}

	}

 
} 
