package eu.smartsantander.androidExperimentation.operations;

import java.util.List;

import org.ambientdynamix.core.DynamixService;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import eu.smartsantander.androidExperimentation.jsonEntities.Smartphone;

public class Communication extends Thread implements Runnable {

	final String NAMESPACE = "http://androidExperimentation.smartsantander.eu/";
	final String URL = Constants.URL+":8080/services/AndroidExperimentationWS?wsdl";
	
	//private Handler handler;
	private final String TAG = this.getClass().getSimpleName();
	
 
	
	public Communication( )
	{
		 
	}
	
	public void run()
	{
		Log.d(TAG, "running");
	}
	
	public int sendPing(String jsonPing)
	{
		final String METHOD_NAME = "Ping";
		final String SOAP_ACTION = "\""+"http://AndroidExperimentationWS/Ping"+"\"";
		
		int pong = 0;
		
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME); 
		PropertyInfo propInfo=new PropertyInfo();
		propInfo.name="arg0";
		propInfo.type=PropertyInfo.STRING_CLASS;
		propInfo.setValue(jsonPing);
  		request.addProperty(propInfo);  
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
		
		try
		{			
			androidHttpTransport.call(SOAP_ACTION, envelope);
			SoapPrimitive  resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();	
			pong = Integer.parseInt(resultsRequestSOAP.toString()); 			
			Log.i("AndroidExperimentation Ping Result", resultsRequestSOAP.toString());		
		}
		catch (Exception e)
		{		
			Log.i("AndroidExperimentation Ping Exception", e.toString());
		}
	
		return pong;
	}	
	

	
	public int registerSmartphone(int phoneId, String sensorsRules) throws Exception
	{
		int serverPhoneId = 0;
		
		Smartphone smartphone = new Smartphone(phoneId);
		smartphone.setPhoneId(phoneId);
		smartphone.setSensorsRules(sensorsRules);
		String jsonSmartphone = (new Gson()).toJson(smartphone);		
		String serverPhoneId_s ="";		
		try
		{
			serverPhoneId_s = sendRegisterSmartphone(jsonSmartphone);
			serverPhoneId = Integer.parseInt(serverPhoneId_s);
		}
		catch(Exception e)
		{
			serverPhoneId = Constants.PHONE_ID_UNITIALIZED;
			Log.i(TAG, "Device Registration Exception:" + e.getMessage());
		}		
		return serverPhoneId;
	}
	
	private String sendRegisterSmartphone(String jsonSmartphone) throws Exception
	{
		Log.i(TAG, "send register smartphone");
		final String METHOD_NAME = "registerSmartphone";
		final String SOAP_ACTION = "\""+"http://AndroidExperimentationWS/registerSmartphone"+"\"";
		String serverPhoneId = "";	
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME); 
		PropertyInfo propInfo=new PropertyInfo();
		propInfo.name="arg0";
		propInfo.type=PropertyInfo.STRING_CLASS;
		propInfo.setValue(jsonSmartphone);
		request.addProperty(propInfo);  		
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
		envelope.setOutputSoapObject(request);		
		HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);		
		try
		{			
			androidHttpTransport.call(SOAP_ACTION, envelope);			
			SoapPrimitive  resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();			
			serverPhoneId = resultsRequestSOAP.toString(); 
		}
		catch (Exception e)
		{
			throw e;
		}
		return serverPhoneId;
	}
	
	public String getExperiment(int phoneId, String sensorRules) throws Exception
	{
		Smartphone smartphone = new Smartphone(phoneId);
		smartphone.setPhoneId(phoneId);
		smartphone.setSensorsRules(sensorRules);
		Gson gson = new Gson();
		String jsonSmartphone = gson.toJson(smartphone);
		return sendGetExperiment(jsonSmartphone);
	}
	
	private String sendGetExperiment(String jsonSmartphone) throws Exception
	{
		final String METHOD_NAME = "getExperiment";
		final String SOAP_ACTION = "\""+"http://AndroidExperimentationWS/getExperiment"+"\"";
		
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME); 

		PropertyInfo propInfo=new PropertyInfo();
		propInfo.name="arg0";
		propInfo.type=PropertyInfo.STRING_CLASS;
		propInfo.setValue(jsonSmartphone);
  
		request.addProperty(propInfo);  
		
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
		envelope.setOutputSoapObject(request);
		
		HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
		
		String response = "0";
		
		try
		{			
			androidHttpTransport.call(SOAP_ACTION, envelope);	
			SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();			
			response = resultsRequestSOAP.toString(); 
			DynamixService.setConnectionStatus(true);
		}
		catch (Exception e)
		{
			DynamixService.setConnectionStatus(false);
			throw e;
		}
		return response;
	}
	
	public int sendReportResults(String jsonReport) throws Exception
	{
		Log.i("AndroidExperimentation", "Report Call");
		
		final String METHOD_NAME = "reportResults";		
		final String SOAP_ACTION = "\""+"http://AndroidExperimentationWS/reportResults"+"\"";
		
		int ack = 0;
		
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME); 

		PropertyInfo propInfo=new PropertyInfo();
		propInfo.name="arg0";
		propInfo.type=PropertyInfo.STRING_CLASS;
		propInfo.setValue(jsonReport);
  
		request.addProperty(propInfo);  

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
		envelope.setOutputSoapObject(request);
		
		HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
		
		try
		{			
			androidHttpTransport.call(SOAP_ACTION, envelope);
			SoapPrimitive  resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
			
			ack = Integer.parseInt(resultsRequestSOAP.toString()); 
			DynamixService.setConnectionStatus(true);
		}
		catch (Exception e)
		{
			DynamixService.setConnectionStatus(false);
			throw e;
		}			
		return ack;
	}
	
	
	public List<Plugin> sendGetPluginList() throws Exception
	{		
		final String METHOD_NAME = "getPluginList";
		final String SOAP_ACTION = "\""+"http://AndroidExperimentationWS/getPluginList"+"\"";
		
      	String jsonPluginList="0";
		
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME); 
		
		PropertyInfo propInfo=new PropertyInfo();
		propInfo.name="arg0";
		propInfo.type=PropertyInfo.STRING_CLASS;
		propInfo.setValue("");
  
		request.addProperty(propInfo);  
				
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
		envelope.setOutputSoapObject(request);
		
		HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
		
		try
		{			
			androidHttpTransport.call(SOAP_ACTION, envelope);
			
			SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
			
			jsonPluginList = resultsRequestSOAP.toString(); 
			DynamixService.setConnectionStatus(true);
		}
		catch (Exception e)
		{
			DynamixService.setConnectionStatus(false);
			throw e;
		}
			
		if(jsonPluginList.equals("0"))
		{
			Log.i(TAG, "no plugin list for us");
			throw new Exception("No available Plugins");
		}
		else
		{
			Gson gson = new Gson();
        	PluginList pluginList = gson.fromJson(jsonPluginList, PluginList.class);        	       	        	
        	List<Plugin> plugList = pluginList.getPluginList();
        	return plugList;
        }

	}
	  
}
