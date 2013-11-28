package eu.smartsantander.androidExperimentation.operations;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import eu.smartsantander.androidExperimentation.tabs.DebugMsg;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/* Singleton class for providing centralized notification mechanism to all
 * the activities of the application. It uses two different queues depending 
 * on the type of the information posted, i.e., network or experiment-centric.
 * 
 */



public class NotificationHQManager {

	private static NotificationHQManager manager;
	private ArrayList<DebugMsg> notifications = new ArrayList<DebugMsg>();
	private ArrayList<DebugMsg> tempList = new ArrayList<DebugMsg>();
	private final String TAG = "NOTIFICATION MANAGER";
	private static int listSize = 16;
	
	private NotificationHQManager() {
		
		// get last notifications stored in Shared Preferences file....
		
	}
	
	public static synchronized NotificationHQManager getInstance() {
		
		if (manager == null) {
			manager = new NotificationHQManager();
		}
		
		return manager;
	}
	
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	public void postNotification(String note){
		// add new note to the end of the list
		//GregorianCalendar d = new GregorianCalendar();
		//String curTime = String.valueOf(d.get(Calendar.HOUR)) + ":" + String.valueOf(d.get(Calendar.MINUTE));		
		
		//notifications.add(curTime + " " + note);
		//Log.w(TAG, "Size of notifications is: " + notifications.size());
		Date d = new Date();
		
		DebugMsg msg = new DebugMsg(note, d);
		
		notifications.add(msg);
		
		int lsize = notifications.size();
		
		// 40 is a reasonably large number, just for the sake of not trimming the list so often
		if (lsize> 40)
			notifications.subList(0, lsize - listSize - 1).clear();
	}
	
	public DebugMsg getLatestNotification() {
		int lsize = notifications.size();
		return notifications.get(lsize -1 );
	}
	
	public ArrayList<DebugMsg> getNotifications() {
		
		int lsize = notifications.size();
		
		// trim size of the list down to listSize (i.e., 7)
		
		if (listSize < lsize)
			notifications.subList(0, lsize - listSize - 1).clear();
			
		Log.w(TAG, "Contents of trimmed notifications list: " + notifications.toString());
		
		//return notifications.toArray(new String[0]);
		tempList = new ArrayList<DebugMsg>(notifications);
		return tempList;

	}
	
}


