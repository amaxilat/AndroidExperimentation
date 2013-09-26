package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;

public class profileTab extends Activity{
	private TextView phoneIdTv;
	private ImageView internetStatusImgv;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);        
        phoneIdTv = (TextView) this.findViewById(R.id.textView1);
        internetStatusImgv = (ImageView) findViewById(R.id.imageView2);      
    }
    
    @Override
    public void onResume(){
    	super.onResume();
  		phoneIdTv.setText(String.valueOf(DynamixService.getPhoneProfiler().getPhoneId()));

    }
    
    public void setInternetStatus(String internet_status)
    {    	
    	if( internet_status.equals("internet_ok") )
    	{
    		internetStatusImgv.setImageResource(R.drawable.database_connected);
    	}
    	else if( internet_status.equals("no_internet") )
    	{
    		internetStatusImgv.setImageResource(R.drawable.database_disconnected);
    	}
    }
}
