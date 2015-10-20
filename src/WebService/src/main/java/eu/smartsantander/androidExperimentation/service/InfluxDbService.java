package eu.smartsantander.androidExperimentation.service;

import eu.smartsantander.androidExperimentation.Application;
import eu.smartsantander.androidExperimentation.model.Result;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class InfluxDbService {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger log = Logger.getLogger(Application.class);
    InfluxDB influxDB;
    private static final String dbUrl = "http://localhost:8086";
    private static final String dbName = dbUrl + "/write?db=experiments";

    @Value("${influxDBAuthorizationToken}")
    String influxDBAuthorizationToken;

    @PostConstruct
    public void init() {
    }


    public boolean store(Result newResult) {
        HashMap<String, Double> parameters = new HashMap<String, Double>();
        parameters.put("experimentId", Double.valueOf(newResult.getExperimentId()));
        parameters.put("deviceId", Double.valueOf(newResult.getDeviceId()));
        int i = 1;
        Double val = null;
        for (String s : newResult.getMessage().split(",")) {
            if (val == null) {
                val = Double.valueOf(s);
            } else {
                parameters.put("param" + i, Double.valueOf(s));
                i++;
            }
        }
        return store(newResult.getTimestamp(), parameters, val);
    }

    public boolean store(final long timestamp, Map<String, Double> parameters, final Double val) {

        String data = "data,";
        final Set<String> args = new HashSet<String>();
        for (String s : parameters.keySet()) {
            args.add(s + "=" + parameters.get(s));
        }
        data += StringUtils.join(args, ",");
        data += " value=" + val;
        log.info(data);
        try {
            postPath(data);

        } catch (Exception e) {
            log.error(e, e);

        }
        return true;
    }

    /**
     * Execute a put request to the specified path.
     *
     * @param entity the text containing the entity to store.
     * @return the response string from the server.
     */
    private String postPath(final String entity) {
        final Entity payload = Entity.text(entity);
        log.debug(payload);
        final Response response = getClientForPath("write").post(payload);
        log.info("status: " + response.getStatus());
        log.info("status: " + response.getEntity());
        log.info("status: " + response.getStatusInfo());
        return response.readEntity(String.class);
    }

    /**
     * Get a client to the orion context broker for the selected path.
     *
     * @param path the path to request.
     * @return a client to execute an http request.
     */
    private Invocation.Builder getClientForPath(final String path) {
        Client client = ClientBuilder.newClient();
        log.debug("path: " + path);
        return client.target(dbUrl)
                .path(path)
                .queryParam("db", "experiments")
                .request()
                .header("Authorization", "Basic " + influxDBAuthorizationToken);
    }

}
