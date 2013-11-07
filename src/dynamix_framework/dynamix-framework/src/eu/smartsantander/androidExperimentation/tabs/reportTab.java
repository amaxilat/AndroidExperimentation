package eu.smartsantander.androidExperimentation.tabs;

import org.ambientdynamix.core.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;


/**
 * This tab displays info about the SmartSantander project using a WebView, mainly because of styling, i.e., couldn't
 * find a simpler way to display fully aligned text :)
 *
 * Charset is set to utf-8 due to using greek and spanish translation for the app
 * 
 * TODO: rename to info, report name is misleading
 * 
 */


public class reportTab extends Activity
{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reporter);
        String text = "<html><body style=\"text-align:justify\" bgcolor=\"black\" text=\"white\"> %s </body></Html>";
        
        WebView webView = (WebView) findViewById(R.id.webview1);
        
        //must add both "text/html; AND charset=utf-8" to the call to handle Greek and Spanish correctly in the WebView or
        //else it displays gibberish
        
        webView.loadData(String.format(text, getString(R.string.about1)), "text/html; charset=utf-8", "utf-8");
                  
    } 
}
