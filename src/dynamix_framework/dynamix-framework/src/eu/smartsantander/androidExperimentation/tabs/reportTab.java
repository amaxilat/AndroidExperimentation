package eu.smartsantander.androidExperimentation.tabs;

import org.ambientdynamix.core.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;


public class reportTab extends Activity
{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reporter);
        String text = "<html><body style=\"text-align:justify\"> %s </body></Html>";
        
        String data ="Android Experimentation is related to the <a href='http://smartsantander.eu'>SmartSantander</a> research project, aiming to provide an Android smartphone application augmenting the functionality of an existing Future Internet infrastructure. <p></p><p>The Android software component distributes executable code on smartphone devices of a network of volunteers. The code is deployed as Ambient Dynamix plugins, performs calculations and produces sensor results, that are uploaded to a central server. There is a central software component responsible for distributing the plugins and collecting the results. </p><p> For more information regarding SmartSantander you can visit: <br> <a href='http://smartsantander.eu'>http://smartsantander.eu</a> <br> </p><p> For more information regarding the Ambient Dynamix framework you can visit: <br> <a href='http://ambientdynamix.org'>http://ambientdynamix.org</a></p> ";
       
        WebView webView = (WebView) findViewById(R.id.webview1);
        webView.loadData(String.format(text, data), "text/html", "utf-8");
                  
    } 
}
