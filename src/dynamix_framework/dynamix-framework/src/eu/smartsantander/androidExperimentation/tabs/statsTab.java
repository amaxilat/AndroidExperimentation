package eu.smartsantander.androidExperimentation.tabs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import eu.smartsantander.androidExperimentation.operations.PhoneProfiler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import eu.smartsantander.androidExperimentation.Constants;

/**
 * This tab displays overall stats about the activity of this specific device
 *
 * Uses several textviews to display info regarding statistics of the current user, if any,
 * and a webview to display a graph retrieved as an image from the experimentation server.
 * The image is created at the server using jfreechart and some obscure JSP...
 *
 *  TODO: CHANGE THE WAY WE HANDLE THE SERVER AND JSP URLs
 *
 *
 */

public class statsTab extends Activity implements
		OnSharedPreferenceChangeListener {

	private OnSharedPreferenceChangeListener listener;
	private PhoneProfiler pProfil;
	private SharedPreferences prefs;
	private WebView myWebView;
	private WebSettings webSettings;
	private String html;
	private loadJPGstatsTask jpgTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.statistics);

		pProfil = DynamixService.getPhoneProfiler();

		myWebView = (WebView) findViewById(R.id.statswebview);
		myWebView.setWebViewClient(new myWebClient());
		myWebView.getSettings().setJavaScriptEnabled(true);
		myWebView.requestFocus(View.FOCUS_DOWN);

		prefs = getSharedPreferences("SmartSantanderConfigurations",
				Context.MODE_PRIVATE);

		// update the field dynamically when changed

		listener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				// TODO Auto-generated method stub

				System.out.println("A CHANGE HAS COME!!!!!! " + key);

			}
		};

		prefs.registerOnSharedPreferenceChangeListener(listener);

		// fill textViews with device data

		fillStatsFields();

		// call the AsyncTask to get the stats picture

		jpgTask = new loadJPGstatsTask();
		jpgTask.execute();


		/*String theJPGData;

		if (checkNetworkIsAvailable()) {

			theJPGData = loadTheStatsJPG();
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("pictureStatsData", theJPGData);
			editor.commit();

		} else {
			theJPGData = prefs.getString("pictureStatsData", "Not available");
		}

		myWebView.loadDataWithBaseURL(null, theJPGData, "text/html", null, null);*/


	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub

	}



	// handle the downloading of the statistics JPG from the server as an asyncTask
	// otherwise, the UI blocks in case of network connectivity issues

	private class loadJPGstatsTask extends AsyncTask<String, Integer, String> {

		String theJPGData;


		@Override
		protected void onPreExecute(){
			System.out.println("ENTERING ASYNC TASK");
		}


		@Override
		protected void onPostExecute(String j){
			myWebView.loadDataWithBaseURL(null, j, "text/html", null, null);
		}


		@Override
		protected String doInBackground(String... params) {

			if (checkNetworkIsAvailable()) {

				theJPGData = loadTheStatsJPG();
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("pictureStatsData", theJPGData);
				editor.commit();

			} else {
				theJPGData = prefs.getString("pictureStatsData", "Not available");
			}

			return theJPGData;
		}


	}




	public class myWebClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

		}

	}

	@Override
	public void onResume() {

		fillStatsFields();

		// String thePNGData = loadTheStatsJPG();

		/*String theJPGData = prefs.getString("pictureStatsData", "Not available");

		if (checkNetworkIsAvailable()) {

			theJPGData = loadTheStatsJPG();
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("pictureStatsData", theJPGData);
			editor.commit();

		} else {
			theJPGData = prefs.getString("pictureStatsData", "Not available");
		}

		myWebView
				.loadDataWithBaseURL(null, theJPGData, "text/html", null, null);*/
		jpgTask.execute();
		super.onResume();

	}

	public void fillStatsFields() {

		TextView statsTextView1 = (TextView) findViewById(R.id.stats_time_title);
		TextView statsTextView2 = (TextView) findViewById(R.id.stats_time_value);
		TextView statsTextView3 = (TextView) findViewById(R.id.stats_number_exp_title);
		TextView statsTextView4 = (TextView) findViewById(R.id.stats_number_exp_value);
		TextView statsTextView5 = (TextView) findViewById(R.id.stats_number_readings_title);
		TextView statsTextView6 = (TextView) findViewById(R.id.stats_number_readings_value);
		TextView statsTextView9 = (TextView) findViewById(R.id.stats_graph_title);

		// String totalMinutes = pProfil.getLastOnlineLogin().toString();

		// String totalMinutes = new Date().toString();

		statsTextView1.setText("Total time online (Hours)");

		statsTextView2.setText(String.format("%.3g%n",
				(float) TimeUnit.MILLISECONDS.toSeconds(DynamixService
						.getTotalTimeConnectedOnline()) / 3600));
		int experiments = DynamixService.getPhoneProfiler().getExperiments()
				.size();
		statsTextView3.setText("Number of experiments run");
		statsTextView4.setText(String.valueOf(experiments));
		statsTextView5.setText("Number of readings produced");
		long totalMsg = DynamixService.getPhoneProfiler()
				.getTotalReadingsProduced();
		statsTextView6.setText(Long.toString(totalMsg));
		statsTextView9.setText("Statistics for previous 7 days");

	}

	private String loadTheStatsJPG() {
		byte[] imageRaw = null;
		try {
			// set time request parameter as today's 00:00 hours
			Date d = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(d.getTime());
			cal.set(Calendar.HOUR_OF_DAY, 0); // set hours to zero
			cal.set(Calendar.MINUTE, 0); // set minutes to zero
			cal.set(Calendar.SECOND, 0); // set seconds to zero

			long time = cal.getTimeInMillis();

			String statsURL = Constants.WEB_STATS_URL + "?tstamp=" + time
					+ "&devId="
					+ DynamixService.getPhoneProfiler().getPhoneId();
			URL url = new URL(statsURL);
			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			InputStream in = new BufferedInputStream(
					urlConnection.getInputStream());
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			int c;
			while ((c = in.read()) != -1) {
				out.write(c);
			}
			out.flush();

			imageRaw = out.toByteArray();

			urlConnection.disconnect();
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String image64 = "";
		if (imageRaw != null && imageRaw.length > 0)
			image64 = Base64.encodeToString(imageRaw, Base64.DEFAULT);

		String pageData = "<meta name=\"viewport\" content=\"width=device-width, user-scalable=no\" /><body bgcolor=black><img src=\"data:image/jpeg;base64,"
				+ image64 + "\" width=\"100%\" /></body>";

		return pageData;
	}

	public boolean checkNetworkIsAvailable() {
		ConnectivityManager cm = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null
				&& activeNetwork.isConnectedOrConnecting();
		return isConnected;
	}

}
