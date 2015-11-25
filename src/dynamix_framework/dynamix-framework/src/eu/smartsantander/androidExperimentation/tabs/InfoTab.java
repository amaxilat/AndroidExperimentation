package eu.smartsantander.androidExperimentation.tabs;

import org.ambientdynamix.core.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

 


public class InfoTab extends Activity
{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.infoTab);
        String text = "<html><body style=\"text-align:justify\"  text=\"white\"> %s </body></Html>";
        
        WebView webView = (WebView) findViewById(R.id.webview1);
        
        //must add both "text/html; AND charset=utf-8" to the call to handle Greek and Spanish correctly in the WebView or
        //else it displays gibberish
        webView.setBackgroundColor(0);
        webView.loadData(String.format(text, getString(R.string.about1)), "text/html; charset=utf-8", "utf-8");
        webView.setBackgroundResource(R.drawable.organicity_background);

    } 
}
