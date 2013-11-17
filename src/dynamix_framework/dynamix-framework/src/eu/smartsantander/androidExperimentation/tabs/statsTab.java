package eu.smartsantander.androidExperimentation.tabs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import eu.smartsantander.androidExperimentation.operations.PhoneProfiler;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
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
	PhoneProfiler pProfil;
	SharedPreferences prefs;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.statistics);
		
		
		pProfil = DynamixService.getPhoneProfiler();
		
		
		
		
		prefs = getSharedPreferences("phoneId", Context.MODE_PRIVATE);
		
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
		
		new DownloadWebStatsTask().execute("http://localhost:8080/mobileStatsChart.jsp?tstamp=00000000&devId=146");
		
		fillStatsFields();	
						
	}
	

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		new DownloadWebStatsTask().execute("http://blanco.cti.gr:8080/chart.jsp?id=4&devId=54");

	}
	
	public void fillStatsFields() {
		
		TextView statsTextView1 = (TextView) findViewById(R.id.stats_time_title);
		TextView statsTextView2 = (TextView) findViewById(R.id.stats_time_value);
		TextView statsTextView3 = (TextView) findViewById(R.id.stats_number_exp_title);
		TextView statsTextView4 = (TextView) findViewById(R.id.stats_number_exp_value);
		TextView statsTextView5 = (TextView) findViewById(R.id.stats_number_readings_title);
		TextView statsTextView6 = (TextView) findViewById(R.id.stats_number_readings_value);
		TextView statsTextView7 = (TextView) findViewById(R.id.stats_comparison_title);
		TextView statsTextView8 = (TextView) findViewById(R.id.stats_comparison_value);
		TextView statsTextView9 = (TextView) findViewById(R.id.stats_graph_title);
		
		//String totalMinutes = pProfil.getLastOnlineLogin().toString();
		
		//String totalMinutes = new Date().toString();
		
		statsTextView1.setText("Total time online");
		statsTextView2.setText("Not available");
		statsTextView3.setText("Number of experiments run");
		statsTextView4.setText("Not available");
		statsTextView5.setText("Number of readings produced");
		statsTextView6.setText("Not available");
		statsTextView7.setText("Comparison with other users");
		statsTextView8.setText("Not available");
		statsTextView9.setText("Statistics for previous 7 days");
		
		
		
	}
	
	private class DownloadWebStatsTask extends AsyncTask<String, Void, String> {
	    
		@Override
	    protected String doInBackground(String... urls) {
	      String response = "";
	      for (String url : urls) {
	        DefaultHttpClient client = new DefaultHttpClient();
	        HttpGet httpGet = new HttpGet(url);
	        try {
	          HttpResponse execute = client.execute(httpGet);
	          InputStream content = execute.getEntity().getContent();

	          BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
	          String s = "";
	          while ((s = buffer.readLine()) != null) {
	            response += s;
	          }

	        } catch (Exception e) {
	          e.printStackTrace();
	        }
	      }
	      return response;
	    }

	
	    @Override
	    protected void onPostExecute(String result) {
	      
	    	fillStatsFields();
	    	WebView myWebView = (WebView) findViewById(R.id.statswebview);
			
			//WebSettings webSettings = myWebView.getSettings();
			//webSettings.setJavaScriptEnabled(true);
			
			myWebView.loadData(result, "text/html", "utf-8");
	    	
	    }
	  }

}
