package eu.smartsantander.androidExperimentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.smartsantander.androidExperimentation.service.GCMService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    GCMService gcmService;

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
            ok.put("message", gcmService.send2Device(deviceId, message));
        } else if (experiment != 0) {
            ok.put("message", gcmService.send2Experiment(experiment, message));
        }

        return ok;
    }

}
