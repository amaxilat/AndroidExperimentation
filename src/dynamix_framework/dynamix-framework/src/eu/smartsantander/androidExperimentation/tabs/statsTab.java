package eu.smartsantander.androidExperimentation.tabs;

import java.util.Date;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;

import eu.smartsantander.androidExperimentation.operations.PhoneProfiler;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;


/**
 * This tab displays overall stats about the activity of this specific device 
 *
 */



public class statsTab extends Activity implements OnSharedPreferenceChangeListener {
	
	
	OnSharedPreferenceChangeListener listener;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.statistics);
		
		
		PhoneProfiler pProfil = DynamixService.getPhoneProfiler();
		
		TextView statsTextView = (TextView) findViewById(R.id.statstextView1);
		
		//String totalMinutes = pProfil.getLastOnlineLogin().toString();
		
		String totalMinutes = new Date().toString();
		
		
		SharedPreferences prefs = getSharedPreferences("phoneId", Context.MODE_PRIVATE);
		
		// update the field dynamically when changed
		
		listener = new OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				// TODO Auto-generated method stub
				
				System.out.println("A CHANGE HAS COME!!!!!! " + key );
				
			}
		};
		
		prefs.registerOnSharedPreferenceChangeListener(listener);
		
		
		String statistics = String.format("Total time online: %s minutes", totalMinutes);
				
		statsTextView.setText(statistics);
		
		WebView myWebView = (WebView) findViewById(R.id.statswebview);
		
		//WebSettings webSettings = myWebView.getSettings();
		//webSettings.setJavaScriptEnabled(true);
		
		myWebView.loadUrl("http://www.google.gr");
						
	}
	

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		
	}
	
	
	

}
