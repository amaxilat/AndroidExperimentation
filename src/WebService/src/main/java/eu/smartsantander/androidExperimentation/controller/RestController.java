package eu.smartsantander.androidExperimentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.smartsantander.androidExperimentation.model.Result;
import eu.smartsantander.androidExperimentation.repository.ResultRepository;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Dimitrios Amaxilatis.
 */
@Controller
public class RestController {
    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(RestController.class);


    @Autowired
    ResultRepository resultRepository;


    @RequestMapping(value = "/experiment/{experimentId}", method = RequestMethod.GET)
    public String ping(final Map<String, Object> model, @PathVariable("experimentId") final String experiment, @RequestParam(value = "deviceId", defaultValue = "0", required = false) final int deviceId, @RequestParam(value = "after", defaultValue = "0", required = false) final String after) throws JSONException, JsonProcessingException {
        LOGGER.debug("experiment:" + experiment);
        DecimalFormat df = new DecimalFormat("#.0000000");
        long start;
        try {
            start = Long.parseLong(after);
        } catch (Exception e) {
            if (after.equals("Today") || after.equals("today")) {
                start = new DateTime().withMillisOfDay(0).getMillis();
            } else if (after.equals("Yesterday") || after.equals("yesterday")) {
                start = new DateTime().withMillisOfDay(0).minusDays(1).getMillis();
            } else {
                start = 0;
            }
        }
        final Set<Result> results;
        if (deviceId == 0) {
            results = resultRepository.findByExperimentIdAndTimestampAfter(Integer.parseInt(experiment), start);
            model.put("title", "Experiment " + experiment);
        } else {
            results = resultRepository.findByExperimentIdAndDeviceIdAndTimestampAfter(Integer.parseInt(experiment), deviceId, start);
            model.put("title", "Experiment " + experiment + " device:" + deviceId);
        }

        Map<String, Map<String, Long>> locationsHeatMap = new HashMap<String, Map<String, Long>>();
        for (Result result : results) {
            final String message = result.getMessage();
            if (message.contains(",")) {
                String longitude = df.format(Double.parseDouble(message.split(",")[0]));
                String latitude = df.format(Double.parseDouble(message.split(",")[1]));
                if (!locationsHeatMap.containsKey(longitude)) {
                    locationsHeatMap.put(longitude, new HashMap<String, Long>());
                }
                if (!locationsHeatMap.get(longitude).containsKey(latitude)) {
                    locationsHeatMap.get(longitude).put(latitude, 0L);
                }
                Long val = locationsHeatMap.get(longitude).get(latitude);
                locationsHeatMap.get(longitude).put(latitude, val + 1);
            }
        }
        JSONArray addressPoints = new JSONArray();
        for (String longit : locationsHeatMap.keySet()) {
            for (String latit : locationsHeatMap.get(longit).keySet()) {
                LOGGER.info("{" + longit + ":" + latit + "}");
                JSONArray measurement = new JSONArray();
                measurement.put(Double.parseDouble(longit));
                measurement.put(Double.parseDouble(latit));
                measurement.put(String.valueOf(locationsHeatMap.get(longit).get(latit)));
                addressPoints.put(measurement);
            }
        }
        LOGGER.info(addressPoints.toString());
        model.put("addressPoints", addressPoints.toString());
        LOGGER.debug("-----------------------------------");
        return "experiment";
    }
}
