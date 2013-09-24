package eu.smartsantander.androidExperimentation.operations;

import java.util.Map;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.Constants;

import android.os.Handler;
import android.util.Log;

public class Registration extends Thread implements Runnable {

	private final String TAG = this.getClass().getSimpleName();

	private Handler handler;
	private PhoneProfiler phoneProfiler;
	private SensorProfiler sensorProfiler;
	private Communication communication;
	Map<String, Boolean> sensorsPermissions;
	private String sensorsRules;

	public Registration(Handler handler, Communication communication,
			PhoneProfiler phoneProfiler, SensorProfiler sensorProfiler) {
		this.handler = handler;
		this.communication = communication;
		this.phoneProfiler = phoneProfiler;
		this.sensorProfiler = sensorProfiler;
		sensorsRules = sensorProfiler.getSensorRules();
		Log.i("Sensor Rules", sensorsRules);
	}

	public void run() {
		try {
			Log.d(TAG, "Registration Process Running");
			Thread.sleep(1000);
			register();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void register() {
		if (phoneProfiler != null
				&& phoneProfiler.getPhoneId() != Constants.PHONE_ID_UNITIALIZED)
			return;
		int phoneId = phoneProfiler.getPhoneId();
		int serverPhoneId;
		
		try {
			serverPhoneId = communication.registerSmartphone(phoneId,sensorsRules);
			phoneProfiler.setPhoneId(serverPhoneId);
		} catch (Exception e) {
			phoneProfiler.setPhoneId(Constants.PHONE_ID_UNITIALIZED);
		}
	}

}
