package eu.smartsantander.androidExperimentation.controller;

import eu.smartsantander.androidExperimentation.model.Result;
import eu.smartsantander.androidExperimentation.repository.ResultRepository;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Dimitrios Amaxilatis.
 */
@Controller
public class RestDataController {
    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(RestDataController.class);
    private static final String AMBIENT_TEMPERATURE = "org.ambientdynamix.contextplugins.AmbientTemperature";
    private static final String NOISE_LEVEL = "org.ambientdynamix.contextplugins.NoiseLevel";


    @Autowired
    ResultRepository resultRepository;


    @RequestMapping(value = "/experiment/data/{experimentId}", method = RequestMethod.GET)
    public String experimentView(final Map<String, Object> model, @PathVariable("experimentId") final String experiment, @RequestParam(value = "deviceId", defaultValue = "0", required = false) final int deviceId, @RequestParam(value = "after", defaultValue = "0", required = false) final String after) throws JSONException {
        LOGGER.debug("experiment:" + experiment);
        if (deviceId == 0) {
            model.put("title", "Experiment " + experiment);
        } else {
            model.put("title", "Experiment " + experiment + " device:" + deviceId);
        }
        model.put("addressPoints", getExperimentData(experiment, deviceId, after).toString());
        LOGGER.debug("-----------------------------------");
        return "experiment-data";
    }

    @ResponseBody
    @RequestMapping(value = "/api/v1/experiment/data/{experimentId}", method = RequestMethod.GET, produces = "application/json")
    public String experimentViewApi(@PathVariable("experimentId") final String experiment, @RequestParam(value = "deviceId", defaultValue = "0", required = false) final int deviceId, @RequestParam(value = "after", defaultValue = "0", required = false) final String after) throws JSONException {
        return getExperimentData(experiment, deviceId, after).toString();
    }

    private JSONArray getExperimentData(final String experiment, final int deviceId, final String after) {
        DecimalFormat df = new DecimalFormat("#.0000");
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
        } else {
            results = resultRepository.findByExperimentIdAndDeviceIdAndTimestampAfterOrderByTimestampAsc(Integer.parseInt(experiment), deviceId, start);
        }

        Map<String, Map<String, Map<String, DescriptiveStatistics>>> dataAggregates = new HashMap<String, Map<String, Map<String, DescriptiveStatistics>>>();
        String longitude = null;
        String latitude = null;
        DescriptiveStatistics wholeDataStatistics = new DescriptiveStatistics();
        Map<String, Map<String, Long>> locationsHeatMap = new HashMap<String, Map<String, Long>>();
        for (Result result : results) {
            try {
                if (!result.getMessage().startsWith("{")) {
                    continue;
                }
                final JSONObject message = new JSONObject(result.getMessage());

                if (message.has("org.ambientdynamix.contextplugins.Latitude")
                        && message.has("org.ambientdynamix.contextplugins.Longitude")) {
                    longitude = df.format(message.getDouble("org.ambientdynamix.contextplugins.Longitude"));
                    latitude = df.format(message.getDouble("org.ambientdynamix.contextplugins.Latitude"));
                    if (!dataAggregates.containsKey(longitude)) {
                        dataAggregates.put(longitude, new HashMap<String, Map<String, DescriptiveStatistics>>());
                    }
                    if (!dataAggregates.get(longitude).containsKey(latitude)) {
                        dataAggregates.get(longitude).put(latitude, new HashMap<String, DescriptiveStatistics>());
                    }

                    //HeatMap
                    if (!locationsHeatMap.containsKey(longitude)) {
                        locationsHeatMap.put(longitude, new HashMap<String, Long>());
                    }
                    if (!locationsHeatMap.get(longitude).containsKey(latitude)) {
                        locationsHeatMap.get(longitude).put(latitude, 0L);
                    }
                    final Long val = locationsHeatMap.get(longitude).get(latitude);
                    locationsHeatMap.get(longitude).put(latitude, val + 1);


                    final Iterator iterator = message.keys();
                    if (longitude != null && latitude != null) {
                        while (iterator.hasNext()) {
                            final String key = (String) iterator.next();
                            if (key.equals("org.ambientdynamix.contextplugins.Latitude")
                                    || key.equals("org.ambientdynamix.contextplugins.Longitude")) {
                                continue;
                            }

                            if (!dataAggregates.get(longitude).get(latitude).containsKey(key)) {
                                dataAggregates.get(longitude).get(latitude).put(key, new DescriptiveStatistics());
                            }
                            try {
                                dataAggregates.get(longitude).get(latitude).get(key).addValue(
                                        message.getDouble(key)
                                );
                                wholeDataStatistics.addValue(message.getDouble(key));
                            } catch (Exception e) {
                                LOGGER.error(e, e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e, e);
            }
        }
        final JSONArray addressPoints = new JSONArray();
        double max = wholeDataStatistics.getMax();
        for (String longit : dataAggregates.keySet()) {
            for (String latit : dataAggregates.get(longit).keySet()) {
                LOGGER.info("{" + longit + ":" + latit + "}");
                JSONArray measurement = new JSONArray();
                try {
                    measurement.put(Double.parseDouble(latit));
                    measurement.put(Double.parseDouble(longit));
                    if (locationsHeatMap.containsKey(longit) &&
                            locationsHeatMap.get(longit).containsKey(latit)) {
                        measurement.put(String.valueOf(locationsHeatMap.get(longit).get(latit)));
                    } else {
                        measurement.put(1);
                    }
                    final JSONObject data = new JSONObject();
                    measurement.put(data);
                    for (final Object key : dataAggregates.get(longit).get(latit).keySet()) {
                        final String keyString = (String) key;
                        final String part = keyString.split("\\.")[keyString.split("\\.").length - 1];
                        data.put(part, dataAggregates.get(longit).get(latit).get(keyString).getMean());
                    }
                    addressPoints.put(measurement);
                } catch (JSONException e) {
                    LOGGER.error(e, e);
                }
            }
        }
        LOGGER.info(addressPoints.toString());
        return addressPoints;
    }
}
