package eu.smartsantander.androidExperimentation.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.gson.Gson;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.AppConstants.PluginInstallStatus;
import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import eu.smartsantander.androidExperimentation.operations.NotificationHQManager;

/**
 * This tab displays various kinds of messages about the sensing plugins installed
 * and events triggered by the Ambient Dynamix framework. Kind of replaces the previous
 * way of posting notifications using the Toast mechanism provided by Android.
 *
 */

public class securityTab extends Activity {

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
		setContentView(R.layout.security);
		
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
