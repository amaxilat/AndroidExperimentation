package eu.smartsantander.androidExperimentation.operations;

import android.content.SharedPreferences;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.ambientdynamix.core.BaseActivity;
import org.ambientdynamix.core.DynamixService;
import org.apache.http.auth.AuthScope;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.OrganicityProfile;
import eu.smartsantander.androidExperimentation.tabs.StatisticsTab;
import eu.smartsantander.androidExperimentation.util.Constants;
import gr.cti.android.experimentation.model.Plugin;
import gr.cti.android.experimentation.model.Smartphone;
import gr.cti.android.experimentation.model.SmartphoneStatistics;

public class OrganicityAAA extends Thread implements Runnable {
    //private Handler handler;
    private final String TAG = this.getClass().getSimpleName();
    private final String BASE_URL = "https://accounts.organicity.eu/realms/organicity/protocol/openid-connect/";
    private final ObjectMapper mapper;

    public OrganicityAAA() {
        mapper = new ObjectMapper();
    }

    public OrganicityProfile getProfile(final String accessToken) {
        try {
            final String responseString = get("userinfo",accessToken);
            if (responseString != null) {
                return mapper.readValue(responseString, OrganicityProfile.class);
            } else {
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, e.getMessage() + " while getting profile.");
            return null;
        }
    }

    private String get(final String path, final String accessToken) throws IOException {

        try {
            if (accessToken != null) {
                final String profileUrl = BASE_URL + path;
                URL url = new URL(profileUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                return readInputStreamToString(connection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readInputStreamToString(HttpsURLConnection connection) {
        String result = null;
        StringBuffer sb = new StringBuffer();
        InputStream is = null;

        try {
            is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        } catch (Exception e) {
            Log.i(TAG, "Error reading InputStream");
            result = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.i(TAG, "Error closing InputStream");
                }
            }
        }

        return result;
    }
}
