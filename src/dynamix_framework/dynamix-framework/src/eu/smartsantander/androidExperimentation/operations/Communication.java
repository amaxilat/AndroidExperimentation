package eu.smartsantander.androidExperimentation.operations;

import android.util.Log;
import com.google.gson.Gson;
import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import eu.smartsantander.androidExperimentation.jsonEntities.Smartphone;
import org.ambientdynamix.core.DynamixService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Communication extends Thread implements Runnable {

    final String NAMESPACE = "http://androidExperimentation.smartsantander.eu/";
    final String URL = Constants.URL + ":8080/services/AndroidExperimentationWS?wsdl";

    //private Handler handler;
    private final String TAG = this.getClass().getSimpleName();


    public Communication() {

    }

    public void run() {
        Log.d(TAG, "running");
    }

    public int sendPing(String jsonPing) {
        int pong = 0;

        try {
            String response = get("/ping");
            pong = 1;
        } catch (IOException e) {
            Log.i("Ping Exception", e.toString());
        }
        return pong;


    }

    private String get(final String path) throws IOException {

        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(new HttpGet(Constants.URL + "/api/v1" + path));
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            final String responseString = out.toString();
            out.close();
            //..more logic
            return responseString;

        } else {
            //Closes the connection.
            response.getEntity().getContent().close();
            throw new IOException(statusLine.getReasonPhrase());
        }
    }

    private String post(final String path, final String entity) throws IOException {


        String url = Constants.URL + "/api/v1" + path;


        // Create a new RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Add the String message converter
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        // Make the HTTP GET request, marshaling the response to a String
        String result = restTemplate.postForObject(url, entity, String.class, new ArrayList<String>());
        return result;
    }


    public int registerSmartphone(int phoneId, String sensorsRules) throws Exception {
        int serverPhoneId = 0;

        Smartphone smartphone = new Smartphone(phoneId);
        smartphone.setPhoneId(phoneId);
        smartphone.setSensorsRules(sensorsRules);
        String jsonSmartphone = (new Gson()).toJson(smartphone);
        String serverPhoneId_s;
        try {
            serverPhoneId_s = sendRegisterSmartphone(jsonSmartphone);
            serverPhoneId = Integer.parseInt(serverPhoneId_s);
        } catch (Exception e) {
            serverPhoneId = Constants.PHONE_ID_UNITIALIZED;
            Log.i(TAG, "Device Registration Exception:" + e.getMessage());
        }
        return serverPhoneId;
    }

    private String sendRegisterSmartphone(String jsonSmartphone) throws Exception {
        Log.i(TAG, "send register smartphone: " + jsonSmartphone);
        String response = post("/smartphone", jsonSmartphone);
        return response;

//		final String METHOD_NAME = "registerSmartphone";
//		final String SOAP_ACTION = "\""+"http://AndroidExperimentationWS/registerSmartphone"+"\"";
//		String serverPhoneId = "";
//		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
//		PropertyInfo propInfo=new PropertyInfo();
//		propInfo.name="arg0";
//		propInfo.type=PropertyInfo.STRING_CLASS;
//		propInfo.setValue(jsonSmartphone);
//		request.addProperty(propInfo);
//		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
//		envelope.setOutputSoapObject(request);
//		HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
//		try
//		{
//			androidHttpTransport.call(SOAP_ACTION, envelope);
//			SoapPrimitive  resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
//			serverPhoneId = resultsRequestSOAP.toString();
//		}
//		catch (Exception e)
//		{
//			throw e;
//		}
//		return serverPhoneId;
    }

    public String getExperiment(int phoneId, String sensorRules) throws Exception {
        Smartphone smartphone = new Smartphone(phoneId);
        smartphone.setPhoneId(phoneId);
        smartphone.setSensorsRules(sensorRules);
        Gson gson = new Gson();
        String jsonSmartphone = gson.toJson(smartphone);
        return sendGetExperiment(jsonSmartphone);
    }

    private String sendGetExperiment(String jsonSmartphone) throws Exception {
        final String METHOD_NAME = "getExperiment";
        final String SOAP_ACTION = "\"" + "http://AndroidExperimentationWS/getExperiment" + "\"";

        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);

        PropertyInfo propInfo = new PropertyInfo();
        propInfo.name = "arg0";
        propInfo.type = PropertyInfo.STRING_CLASS;
        propInfo.setValue(jsonSmartphone);

        request.addProperty(propInfo);

        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);

        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);

        String response = "0";

        try {
            androidHttpTransport.call(SOAP_ACTION, envelope);
            SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
            response = resultsRequestSOAP.toString();
            DynamixService.setConnectionStatus(true);
        } catch (Exception e) {
            DynamixService.setConnectionStatus(false);
            throw e;
        }
        return response;
    }

    public int sendReportResults(String jsonReport) throws Exception {
        DynamixService.logToFile(jsonReport);
        Log.i("AndroidExperimentation", "Report Call");

        //jsonReport=jsonReport.replace("\\\\\\", "\\");
        final String METHOD_NAME = "reportResults";
        final String SOAP_ACTION = "\"" + "http://AndroidExperimentationWS/reportResults" + "\"";

        int ack = 0;

        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);

        PropertyInfo propInfo = new PropertyInfo();
        propInfo.name = "arg0";
        propInfo.type = PropertyInfo.STRING_CLASS;
        propInfo.setValue(jsonReport);

        request.addProperty(propInfo);

        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);

        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);

        try {
            androidHttpTransport.call(SOAP_ACTION, envelope);
            SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();

            ack = Integer.parseInt(resultsRequestSOAP.toString());
            DynamixService.setConnectionStatus(true);
        } catch (Exception e) {

            DynamixService.setConnectionStatus(false);
            throw e;
        }
        return ack;
    }


    public List<Plugin> sendGetPluginList() throws Exception {
        final String METHOD_NAME = "getPluginList";
        final String SOAP_ACTION = "\"" + "http://AndroidExperimentationWS/getPluginList" + "\"";

        String jsonPluginList = "0";

        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);

        PropertyInfo propInfo = new PropertyInfo();
        propInfo.name = "arg0";
        propInfo.type = PropertyInfo.STRING_CLASS;
        propInfo.setValue("");

        request.addProperty(propInfo);

        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);

        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);

        try {
            androidHttpTransport.call(SOAP_ACTION, envelope);

            SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();

            jsonPluginList = resultsRequestSOAP.toString();
            DynamixService.setConnectionStatus(true);
        } catch (Exception e) {
            DynamixService.setConnectionStatus(false);
            throw e;
        }

        if (jsonPluginList.equals("0")) {
            Log.i(TAG, "no plugin list for us");
            throw new Exception("No available Plugins");
        } else {
            Gson gson = new Gson();
            PluginList pluginList = gson.fromJson(jsonPluginList, PluginList.class);
            List<Plugin> plugList = pluginList.getPluginList();
            return plugList;
        }

    }

}
