package eu.smartsantander.androidExperimentation.operations;

import android.net.Uri;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.util.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.Smartphone;

import org.ambientdynamix.core.DynamixService;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class Communication extends Thread implements Runnable {

    final String NAMESPACE = "http://androidExperimentation.smartsantander.eu/";
    final String URL = Constants.URL + ":8080/services/AndroidExperimentationWS?wsdl";

    //private Handler handler;
    private final String TAG = this.getClass().getSimpleName();

    final RestTemplate restTemplate;
//    Cache<String, String> alreadySent;

    public Communication() {
//        alreadySent = CacheBuilder.newBuilder()
//                .expireAfterWrite(30, TimeUnit.SECONDS)
//                .build();

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

    private String get(final URI uri) throws IOException {

        // Make the HTTP GET request, marshaling the response to a String
        return restTemplate.getForObject(uri, String.class);
    }


    public int registerSmartphone(int phoneId, String sensorsRules) throws Exception {
        int serverPhoneId = 0;

        Smartphone smartphone = new Smartphone();
        smartphone.setPhoneId(phoneId);
        smartphone.setSensorsRules(sensorsRules);
        String jsonSmartphone = (new Gson()).toJson(smartphone);
        String serverPhoneId_s;
        try {
            serverPhoneId_s = sendRegisterSmartphone(jsonSmartphone);
            serverPhoneId = Integer.parseInt(serverPhoneId_s);
            //ParsePush.subscribeInBackground("phone:" + serverPhoneId);
        } catch (Exception e) {
            e.printStackTrace();
            serverPhoneId = Constants.PHONE_ID_UNITIALIZED;
            Log.i(TAG, "Device Registration Exception:" + e.getMessage());
        }
        return serverPhoneId;
    }


    public SortedMap<Integer, Double> getLastStatistics(final int phoneId) {

        try {
            String stats = get("/statistics/" + phoneId);
            Log.i(TAG, stats);
            Map values = (new Gson()).fromJson(stats, Map.class);
            SortedMap<Integer, Double> sortedMap = new TreeMap<Integer, Double>(new Comparator<Integer>() {
                @Override
                public int compare(Integer lhs, Integer rhs) {
                    return rhs - lhs;
                }
            });
            for (Object key : values.keySet()) {
                sortedMap.put(Integer.valueOf(((String) key)), (Double) values.get(key));
            }
            return sortedMap;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public JSONArray getLastPoints(int phoneId) {
        if (DynamixService.getExperiment() == null || DynamixService.getExperiment().getId() == null) {
            Log.e(TAG, "No Experiment");
            return null;
        }
        final String path = "/experiment/" + DynamixService.getExperiment().getId() + "?deviceId=" + phoneId + "&after=today";
        try {
            final String stats = get(path);
            Log.i(TAG, stats);
            try {
                return new JSONArray(stats);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

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

    public int sendReportResults(String jsonReport) throws Exception {
//        if (alreadySent.getIfPresent(jsonReport) != null) {
//            return 0;
//        }
        DynamixService.logToFile(jsonReport);
        Log.i(TAG, "Report Call " + jsonReport);
        try {
            Log.i(TAG, jsonReport);
//            alreadySent.put(jsonReport, "1");
            post("/experiment", jsonReport);

            return 0;
        } catch (HttpClientErrorException e) {
            //ignore
            return 0;

        }
    }

    public List<Plugin> sendGetPluginList() throws Exception {
        final String pluginListStr = get("/plugin");
        return new ObjectMapper().readValue(pluginListStr, new TypeReference<List<Plugin>>() {
        });
    }

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


}
