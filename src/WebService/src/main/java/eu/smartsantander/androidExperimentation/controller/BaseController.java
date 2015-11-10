package eu.smartsantander.androidExperimentation.controller;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletResponse;

@Controller
public class BaseController {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(BaseController.class);
    protected static final String LATITUDE = "org.ambientdynamix.contextplugins.Latitude";
    protected static final String LONGITUDE = "org.ambientdynamix.contextplugins.Longitude";

    protected JSONObject ok(final HttpServletResponse servletResponse) throws JSONException {
        servletResponse.setStatus(200);
        final JSONObject response = new JSONObject();
        response.put("status", "Ok");
        response.put("code", 200);
        return response;
    }

    protected JSONObject internalServerError(final HttpServletResponse response) throws JSONException {
        response.setStatus(500);
        final JSONObject res = new JSONObject();
        res.put("status", "Internal Server Error");
        res.put("code", 5500);
        return res;
    }

}
