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
        String text = "<html><body style=\"text-align:justify\" bgcolor=\"black\" text=\"white\"> %s </body></Html>";
        
        String data ="Android Experimentation is related to the <a href='http://smartsantander.eu'>SmartSantander</a> research project, providing an Android smartphone application to extend the functionality of an existing Future Internet infrastructure.<p></p><p>The Android software component downloads executable code on volunteers' smartphones, to gather sensors' readings and perform calculations, that are uploaded to a central server. The server is responsible for distributing the code and collecting the results. </p><p> For more information about SmartSantander you can visit <a href='http://smartsantander.eu'>http://smartsantander.eu</a> <br> </p><p> For more information regarding the Ambient Dynamix framework you can visit: <br> <a href='http://ambientdynamix.org'>http://ambientdynamix.org</a></p> ";
       
        WebView webView = (WebView) findViewById(R.id.webview1);
        webView.loadData(String.format(text, data), "text/html", "utf-8");
                  
    } 
}
