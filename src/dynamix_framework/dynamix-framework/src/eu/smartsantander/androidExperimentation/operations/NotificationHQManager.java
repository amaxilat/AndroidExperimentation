package eu.smartsantander.androidExperimentation.operations;

import java.util.ArrayList;

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
	private ArrayList<String> notifications = new ArrayList<String>();
	
	private static int listSize = 8;
	
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
		notifications.add(note);
	}
	
	public String getLatestNotification() {
		int lsize = notifications.size();
		return (String)notifications.get(lsize - 1);
	}
	
	public String[] getNotifications() {
		
		int lsize = notifications.size();
		
		// trim size of the list down to listSize (i.e., 7)
		
		if (listSize < lsize)
			notifications.subList(0, lsize - listSize - 1).clear();
		
		return notifications.toArray(new String[0]);
	}
	
}


