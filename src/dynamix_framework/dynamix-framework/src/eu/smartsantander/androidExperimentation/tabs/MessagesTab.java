package eu.smartsantander.androidExperimentation.tabs;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import org.ambientdynamix.core.R;

import eu.smartsantander.androidExperimentation.operations.NotificationHQManager;
import eu.smartsantander.androidExperimentation.util.DebugMsg;
import eu.smartsantander.androidExperimentation.util.DebugMsgArrayAdapter;

/**
 * This tab displays various kinds of messages about the sensing plugins installed
 * and events triggered by the Ambient Dynamix framework. Kind of replaces the previous
 * way of posting notifications using the Toast mechanism provided by Android.
 *
 */

public class MessagesTab extends Activity {

	//List<HashMap<String,String>> sensorOptionsL=new ArrayList<HashMap<String,String>>();	
	//HashMap<String, String> sensorOptions = new HashMap<String, String>();
	//SimpleAdapter simpleAdptl;
	
	NotificationHQManager notesManager;
	//String[] notes = new String[0];
	//ArrayList<String> notes;
	ArrayList<DebugMsg> notes;
	
	private final String TAG = "DEBUG TAB";
	
	//ArrayAdapter<String> noteAdapter;
	DebugMsgArrayAdapter noteAdapter;
	ListView list1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messages_tab);
		
		notesManager = NotificationHQManager.getInstance();
		
		notes = notesManager.getNotifications();
		
		
		
		//simpleAdptl = new SimpleAdapter(this, sensorOptionsL, android.R.layout.simple_list_item_1, new String[] {"sensor"}, new int[] {android.R.id.text1});
		
		//noteAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, notes);
		noteAdapter = new DebugMsgArrayAdapter(this, android.R.layout.simple_list_item_1, notes);
		
		// find listview in the tab for debug messages
		list1 = (ListView) findViewById(R.id.notification_messages_list);
		
		list1.setAdapter(noteAdapter);
		
	}

	@Override
	public void onResume() {
		super.onResume();

		//notes = notesManager.getNotifications();
		
//		for (DebugMsg m : notes)
//			Log.w(TAG, "Timestamp: " + m.getDate());
		
		//Log.w(TAG, "size of notes in debug is " + notes.length);
		
		//for (int i=0; i<notes.length; i++)
		//	Log.w(TAG, "notes[" +i + "]="+ notes[i]);
		notes = notesManager.getNotifications();
		noteAdapter = new DebugMsgArrayAdapter(this, android.R.layout.simple_list_item_1, notes);
		list1.setAdapter(noteAdapter);
		
       	for (DebugMsg m : notes)
                		Log.w(TAG, "Timestamp: " + m.getDate());
                	//noteAdapter.notifyDataSetChanged();
		
		
 	}
	
	
	
}
