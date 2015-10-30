package eu.smartsantander.androidExperimentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class GCMController extends BaseController {
    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(GCMController.class);
    @Value("${gcm.key}")
    private String gcmKey;

    @ResponseBody
    @RequestMapping(value = "/gcm/send", method = RequestMethod.GET)
    public JSONObject experimentView(final Map<String, Object> model, HttpServletResponse response
            , @RequestParam(value = "experiment", defaultValue = "0", required = false) final int experiment
            , @RequestParam(value = "deviceId", defaultValue = "0", required = false) final int deviceId
            , @RequestParam(value = "message", defaultValue = "", required = false) final String message) throws JSONException {
        LOGGER.debug("experiment:" + experiment);
        LOGGER.debug("deviceId:" + deviceId);

        final JSONObject ok = ok(response);

        if (deviceId != 0) {
            ok.put("message", send2Device(deviceId, message));
        } else if (experiment != 0) {
            ok.put("message", send2Experiment(experiment, message));
        }

        return ok;
    }

    private String send2Experiment(int experiment, String message) {
        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("message", message);
        return send2Topic("/topics/experiment-" + experiment, dataMap);
    }

    private String send2Device(int deviceId, String message) {
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


    public String post(final Map<String, Object> message) {
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
