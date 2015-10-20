package eu.smartsantander.androidExperimentation.service;

import com.amaxilatis.orion.OrionClient;
import com.amaxilatis.orion.model.OrionContextElement;
import com.amaxilatis.orion.util.SensorMLTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.smartsantander.androidExperimentation.model.Result;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class OrionService {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(OrionService.class);
    private static final String ORION_SMARTPHONE_ID_FORMAT = "urn:oc:entity:%s:smartphone:phone:%s";
    private static final String ORION_SMARTPHONE_TYPE = "urn:oc:entitytype:smartphone";

    @Value("${siteName:patras}")
    private String siteName;
    private SimpleDateFormat df;

    private OrionClient orionClient;

    private void setName(String siteName) {
        this.siteName = siteName;
    }

    @PostConstruct
    public void init() {
        orionClient = new OrionClient("http://localhost:1026","");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

    }

    @Async
    public void storeOrion(final String id, Result newResult) {
        LOGGER.info("store-orion:" + id);

        final String uri = String.format(ORION_SMARTPHONE_ID_FORMAT, siteName, id);
        LOGGER.info(uri);
        OrionContextElement phoneEntity = new OrionContextElement();
        phoneEntity.setId(uri);
        phoneEntity.setIsPattern("false");
        phoneEntity.setType(ORION_SMARTPHONE_TYPE);

        phoneEntity.setAttributes(new ArrayList<Map<String, Object>>());
        phoneEntity.getAttributes().add(OrionClient.createAttribute("TimeInstant", SensorMLTypes.ISO8601_TIME, df.format(new Date())));

        JSONObject readingList = null;
        try {
            LOGGER.info(newResult.getMessage());
            readingList = new JSONObject(newResult.getMessage());

            Iterator<String> keys = readingList.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                if (key.contains("Latitude")) {
                    phoneEntity.getAttributes().add(OrionClient.createAttribute("Latitud", SensorMLTypes.LATITUDE, String.valueOf(readingList.get(key))));
                } else if (key.contains("Longitude")) {
                    phoneEntity.getAttributes().add(OrionClient.createAttribute("Longitud", SensorMLTypes.LONGITUDE, String.valueOf(readingList.get(key))));
                }
            }
            try {
                LOGGER.info(phoneEntity);
                LOGGER.info((new ObjectMapper()).writeValueAsString(phoneEntity));

                final String res = orionClient.postContextEntity(uri, phoneEntity);
                LOGGER.info(res);
            } catch (Exception e) {
                LOGGER.error(e, e);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
