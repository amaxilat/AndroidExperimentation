package eu.smartsantander.androidExperimentation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.smartsantander.androidExperimentation.controller.BaseController;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dimitrios Amaxilatis.
 */
@Controller
public class GCMService {
    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(GCMService.class);

    @Value("${gcm.key}")
    private String gcmKey;


    public String send2Experiment(int experiment, String message) {
        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("message", message);
        return send2Topic("/topics/experiment-" + experiment, dataMap);
    }

    public String send2Device(int deviceId, String message) {
        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("message", message);
        return send2Topic("/topics/device-" + deviceId, dataMap);
    }

    private String send2Topic(String topic, Map<String, String> dataMap) {
        Map<String, Object> payloadMap = new HashMap<String, Object>();
        payloadMap.put("data", dataMap);
        payloadMap.put("to", topic);
        return post(payloadMap);
    }


    private String post(final Map<String, Object> message) {
        Entity entity = null;
        try {
            entity = Entity.json(new ObjectMapper().writeValueAsString(message));

            Response response = ClientBuilder.newClient()
                    .target("https://gcm-http.googleapis.com")
                    .path("gcm/send")
                    .request()
                    .header("Content-Type", "application/json")
                    .header("Authorization", "key=" + gcmKey)
                    .post(entity);
            Response.Status.Family statusFamily = response.getStatusInfo().getFamily();
            if (statusFamily == Response.Status.Family.SUCCESSFUL) {
                final String responseString = response.readEntity(String.class);
                return responseString;
            } else {
                return response.getStatusInfo().getReasonPhrase();
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
