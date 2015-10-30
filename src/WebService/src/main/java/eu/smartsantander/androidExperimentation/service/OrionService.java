package eu.smartsantander.androidExperimentation.service;

import com.amaxilatis.orion.OrionClient;
import eu.organicity.entities.handler.attributes.Attribute;
import eu.organicity.entities.handler.entities.SmartphoneDevice;
import eu.organicity.entities.handler.metadata.Datatype;
import eu.organicity.entities.namespace.OrganicityAttributeTypes;
import eu.organicity.entities.namespace.OrganicityDatatypes;
import eu.smartsantander.androidExperimentation.model.Result;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;


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
        orionClient = new OrionClient("http://localhost:1026", "");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

    }

    @Async
    public void storeOrion(final String id, Result newResult) {
        LOGGER.info("store-orion:" + id);

        final String uri = String.format(ORION_SMARTPHONE_ID_FORMAT, siteName, id);
        LOGGER.info(uri);

        final SmartphoneDevice phoneEntity = new SmartphoneDevice(uri);

        phoneEntity.setTimestamp(new Date());

        try {
            LOGGER.info(newResult.getMessage());
            final JSONObject readingList = new JSONObject(newResult.getMessage());

            final Iterator<String> keys = readingList.keys();
            String latitude = null;
            String longitude = null;
            while (keys.hasNext()) {
                final String key = keys.next();
                if (key.contains("Latitude")) {
                    latitude = String.valueOf(readingList.get(key));
                } else if (key.contains("Longitude")) {
                    longitude = String.valueOf(readingList.get(key));
                } else if (key.contains("NoiseLevel")) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.SOUND_PRESSURE_LEVEL, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains("AmbientTemperature")) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.TEMPERATURE, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                }
            }
            try {
                if (longitude != null && latitude != null) {
                    phoneEntity.setPosition(Double.parseDouble(latitude), Double.parseDouble(longitude));
                }
                final String res = orionClient.postContextEntity(uri, phoneEntity.getContextElement());
                LOGGER.info(res);
            } catch (Exception e) {
                LOGGER.error(e, e);
            }

        } catch (JSONException e) {
            LOGGER.warn(e.getMessage());
        }

    }
}
