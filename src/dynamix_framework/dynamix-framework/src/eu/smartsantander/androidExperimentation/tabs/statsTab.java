package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;


import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.operations.Communication;
import eu.smartsantander.androidExperimentation.operations.PhoneProfiler;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.ambientdynamix.util.Log;
import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * This tab displays overall stats about the activity of this specific device
 * <p/>
 * Uses several textviews to display info regarding statistics of the current user, if any,
 * and a webview to display a graph retrieved as an image from the experimentation server.
 * The image is created at the server using jfreechart and some obscure JSP...
 * <p/>
 * TODO: CHANGE THE WAY WE HANDLE THE SERVER AND JSP URLs
 */

public class statsTab extends Activity implements
        OnSharedPreferenceChangeListener {

    private static final String TAG = "StatsTab";
    private OnSharedPreferenceChangeListener listener;
    private PhoneProfiler pProfil;
    private SharedPreferences prefs;
    //	private WebView myWebView;
    private WebSettings webSettings;
    private String html;
    private loadJPGstatsTask jpgTask;

    private Communication communication;
    private BarChart mBarChart;
    private statsTab thisActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics);

        pProfil = DynamixService.getPhoneProfiler();

//		myWebView = (WebView) findViewById(R.id.statswebview);
//		myWebView.setWebViewClient(new myWebClient());
//		myWebView.getSettings().setJavaScriptEnabled(true);
//		myWebView.requestFocus(View.FOCUS_DOWN);

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

        thisActivity = this;

        communication = new Communication();

        prefs.registerOnSharedPreferenceChangeListener(listener);

        // fill textViews with device data

        fillStatsFields();

        // call the AsyncTask to get the stats picture
        mBarChart = (BarChart) findViewById(R.id.barchart);
        jpgTask = new loadJPGstatsTask();
        jpgTask.execute();


//        for (Integer integer : sortedMap.keySet()) {
//            mBarChart.addBar(new BarModel(sortedMap.get(integer), 0xFF123456));
//        }
//        mBarChart.startAnimation();

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
        protected void onPreExecute() {
            System.out.println("ENTERING ASYNC TASK");
        }


        @Override
        protected void onPostExecute(String j) {
//			myWebView.loadDataWithBaseURL(null, j, "text/html", null, null);

        }


        @Override
        protected String doInBackground(String... params) {


            final SortedMap<Integer, Double> sortedMap = communication.getLastStatistics(
                    DynamixService.getPhoneProfiler().getPhoneId()
            );

            thisActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBarChart.clearChart();
                    for (Integer integer : sortedMap.keySet()) {
                        mBarChart.addBar(new BarModel(integer.toString(), sortedMap.get(integer).floatValue(), 0xFF1FF4AC));
                    }
                    mBarChart.startAnimation();
                }
            });

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
        try {
            jpgTask.execute();
        } catch (Exception e) {
            Log.e(TAG, "Stats:" + e.getMessage());
        }
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
