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

    @Value("${siteName:london}")
    private String siteName;

    private OrionClient orionClient;

    private void setName(String siteName) {
        this.siteName = siteName;
    }

    @PostConstruct
    public void init() {
        orionClient = new OrionClient("http://localhost:1026", "", "organicity", "/");
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

            final Iterator keys = readingList.keys();
            String latitude = null;
            String longitude = null;
            while (keys.hasNext()) {
                final String key = (String) keys.next();
                if (key.contains("Latitude")) {
                    latitude = String.valueOf(readingList.get(key));
                } else if (key.contains("Longitude")) {
                    longitude = String.valueOf(readingList.get(key));
                } else if (key.startsWith(OrganicityAttributeTypes.Types.SOUND_PRESSURE_LEVEL.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.SOUND_PRESSURE_LEVEL, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.TEMPERATURE.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.TEMPERATURE, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.RELATIVE_HUMIDITY.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.RELATIVE_HUMIDITY, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.ATMOSPHERIC_PRESSURE.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.ATMOSPHERIC_PRESSURE, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.CARBON_MONOXIDE.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.CARBON_MONOXIDE, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.METHANE.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.METHANE, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.LPG.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.LPG, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.ILLUMINANCE.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.ILLUMINANCE, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.PARTICLES10.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.PARTICLES10, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.PARTICLES25.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.PARTICLES25, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.BATTERY_LEVEL.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.BATTERY_LEVEL, String.valueOf(readingList.get(key)));
                    Datatype dm = new Datatype(OrganicityDatatypes.DATATYPES.NUMERIC);
                    a.addMetadata(dm);
                    phoneEntity.addAttribute(a);
                } else if (key.contains(OrganicityAttributeTypes.Types.BATTERY_VOLTAGE.getUrn())) {
                    Attribute a = new Attribute(OrganicityAttributeTypes.Types.BATTERY_VOLTAGE, String.valueOf(readingList.get(key)));
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
