package eu.smartsantander.androidExperimentation.operations;

import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.ambientdynamix.core.DynamixService;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.util.Constants;
import gr.cti.android.experimentation.model.Plugin;
import gr.cti.android.experimentation.model.Smartphone;
import gr.cti.android.experimentation.model.SmartphoneStatistics;

public class Communication extends Thread implements Runnable {
    //private Handler handler;
    private final String TAG = this.getClass().getSimpleName();

    final RestTemplate restTemplate;

    private int lastHash;
    private String message;

    public Communication() {
        // Create a new RestTemplate instance
        restTemplate = new RestTemplate();

        // Add the String message converter
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        lastHash = 0;
    }

    public void run() {
        Log.d(TAG, "running");
    }

    /**
     * Register a smartphone to the server.
     *
     * @param phoneId      the unique id of the smartphone.
     * @param sensorsRules a list of sensors available on the phone.
     * @return the server id of the smartphone.
     * @throws Exception
     */
    public int registerSmartphone(final int phoneId, final String sensorsRules) throws Exception {
        int serverPhoneId = 0;

        final Smartphone smartphone = new Smartphone();
        smartphone.setPhoneId((long) phoneId);
        smartphone.setSensorsRules(sensorsRules);
        final String jsonSmartphone = (new Gson()).toJson(smartphone);
        final String serverPhoneId_s;
        try {
            serverPhoneId_s = sendRegisterSmartphone(jsonSmartphone);
            serverPhoneId = Integer.parseInt(serverPhoneId_s);
        } catch (Exception e) {
            serverPhoneId = Constants.PHONE_ID_UNITIALIZED;
            Log.e(TAG, "Device Registration Exception:" + e.getMessage(), e);
        }
        return serverPhoneId;
    }


    public SmartphoneStatistics getSmartphoneStatistics(Integer id) {
        try {
            final String stats = get("/smartphone/" + id + "/statistics");
            return new ObjectMapper().readValue(stats, SmartphoneStatistics.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SmartphoneStatistics getSmartphoneStatistics(int id, int experimentId) {
        try {
            final String stats = get("/smartphone/" + id + "/statistics/" + experimentId);
            return new ObjectMapper().readValue(stats, SmartphoneStatistics.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the last points of measurements by the user.
     * TODO : move it to the backend for statistics
     * @param phoneId the if of the user's phone.
     * @return
     */
    public JSONArray getLastPoints(final int phoneId) {
        final String path = "/data?deviceId=" + phoneId + "&after=today";
        try {
            final String stats = get(path);
            try {
                return new JSONArray(stats);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieve a list of all the available experiments.
     *
     * @return a list of experiments the user can install on his smartphone.
     * @throws Exception
     */
    public List<Experiment> getExperiments() throws Exception {
        String experimentsString = get("/experiment");
        return new ObjectMapper().readValue(experimentsString, new TypeReference<List<Experiment>>() {
        });
    }


    /**
     * Retrieve a list of all the available experiments for the given phoneId.
     *
     * @param phoneId the phoneId that queries for experiments.
     * @return a list of experiments the user can install on his smartphone.
     * @throws Exception
     */
    public List<Experiment> getExperimentsById(final String phoneId) throws Exception {

        URI targetUrl = UriComponentsBuilder.fromUriString(Constants.URL)
                .path("/api/v1/experiment")
                .queryParam("phoneId", phoneId)
                .build()
                .toUri();

        String experimentsString = get(targetUrl);
        return new ObjectMapper().readValue(experimentsString, new TypeReference<List<Experiment>>() {
        });
    }

    /**
     * Report a set of results to the server.
     *
     * @param jsonReport a json text representation of the response.
     * @return
     * @throws Exception
     */
    public int sendReportResults(String jsonReport) throws Exception {
        //do not send them twice
        if (jsonReport.hashCode() == lastHash) {
            return 0;
        }

        DynamixService.logToFile(jsonReport);
        try {
            post("/data", jsonReport);
            lastHash = jsonReport.hashCode();
            return 0;
        } catch (HttpClientErrorException e) {
            //ignore
            return 0;

        }
    }

    /**
     * Retrieve a list of all the available plugins for the given phoneId.
     *
     * @param phoneId the phoneId that queries for plugins.
     * @return a list of plugins the user can install on his smartphone.
     * @throws Exception
     */
    public List<Plugin> sendGetPluginList(final String phoneId) throws Exception {

        URI targetUrl = UriComponentsBuilder.fromUriString(Constants.URL)
                .path("/api/v1/plugin")
                .queryParam("phoneId", phoneId)
                .build()
                .toUri();

        final String pluginListStr = get(targetUrl);
        return new ObjectMapper().readValue(pluginListStr, new TypeReference<List<Plugin>>() {
        });
    }

    private String sendRegisterSmartphone(String jsonSmartphone) throws Exception {
        Log.d(TAG, "Register Smartphone" + jsonSmartphone);
        return post("/smartphone", jsonSmartphone);
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

    private String get(final URI uri) throws IOException {

        // Make the HTTP GET request, marshaling the response to a String
        return restTemplate.getForObject(uri, String.class);
    }

    public void setLastMessage(String message) {
        this.message = message;
    }

    public String getLastMessage() {
        return this.message;
    }

}
