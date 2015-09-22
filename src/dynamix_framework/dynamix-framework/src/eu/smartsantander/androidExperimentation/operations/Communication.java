package eu.smartsantander.androidExperimentation.operations;

import android.util.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.Smartphone;
import org.ambientdynamix.core.DynamixService;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Communication extends Thread implements Runnable {

    final String NAMESPACE = "http://androidExperimentation.smartsantander.eu/";
    final String URL = Constants.URL + ":8080/services/AndroidExperimentationWS?wsdl";

    //private Handler handler;
    private final String TAG = this.getClass().getSimpleName();

    final RestTemplate restTemplate;

    public Communication() {
        // Create a new RestTemplate instance
        restTemplate = new RestTemplate();

        // Add the String message converter
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
    }

    public void run() {
        Log.d(TAG, "running");
    }

    public int sendPing(String jsonPing) {
        int pong = 0;

        try {
            get("/ping");
            pong = 1;
        } catch (IOException e) {
            Log.i("Ping Exception", e.toString());
        }
        return pong;


    }

    private String post(final String path, final String entity) throws IOException {

        final String url = Constants.URL + "/api/v1" + path;

        // Make the HTTP POST request, marshaling the response to a String
        return restTemplate.postForObject(url, entity, String.class, new ArrayList<String>());
    }

    private String get(final String path) throws IOException {

        final String url = Constants.URL + "/api/v1" + path;

        // Make the HTTP GET request, marshaling the response to a String
        return restTemplate.getForObject(url, String.class, new ArrayList<String>());
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
        return post("/smartphone", jsonSmartphone);
    }

    public List<Experiment> getExperiments() throws Exception {
        String experimentsString = get("/experiment");
        return new ObjectMapper().readValue(experimentsString, new TypeReference<List<Experiment>>() {
        });
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
        Log.i(TAG, "Report Call");
        try {
            Log.i(TAG, jsonReport);
            post("/experiment", jsonReport);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }


    public List<Plugin> sendGetPluginList() throws Exception {
        final String pluginListStr = get("/plugin");
        return new ObjectMapper().readValue(pluginListStr, new TypeReference<List<Plugin>>() {
        });
    }
}
