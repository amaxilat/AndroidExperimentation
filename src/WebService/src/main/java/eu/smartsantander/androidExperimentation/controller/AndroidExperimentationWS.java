package eu.smartsantander.androidExperimentation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.smartsantander.androidExperimentation.entities.Reading;
import eu.smartsantander.androidExperimentation.entities.Report;
import eu.smartsantander.androidExperimentation.model.Experiment;
import eu.smartsantander.androidExperimentation.model.Plugin;
import eu.smartsantander.androidExperimentation.model.Result;
import eu.smartsantander.androidExperimentation.model.Smartphone;
import eu.smartsantander.androidExperimentation.repository.ExperimentRepository;
import eu.smartsantander.androidExperimentation.repository.ResultRepository;
import eu.smartsantander.androidExperimentation.repository.SmartphoneRepository;
import eu.smartsantander.androidExperimentation.service.InfluxDbService;
import eu.smartsantander.androidExperimentation.service.ModelManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

@Controller
@RequestMapping(value = "/api/v1")
public class AndroidExperimentationWS extends BaseController {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(AndroidExperimentationWS.class);


    @Autowired
    ModelManager modelManager;
    @Autowired
    SmartphoneRepository smartphoneRepository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    ResultRepository resultRepository;
    @Autowired
    InfluxDbService influxDbService;


//    //@ResponseBody
//    //@RequestMapping(value = "/report")
//    public String reportResults(String reportJson) {
//        try {
//            Report report = Report.fromJson(reportJson);
//            modelManager.reportResults(report);
//            LOGGER.debug("Report Stored: Device:" + report.getDeviceId());
//            LOGGER.debug("Report Stored:" + reportJson);
//            LOGGER.debug("-----------------------------------");
//        } catch (Exception e) {
//            e.printStackTrace();
//            LOGGER.debug(e.getMessage());
//        }
//        return "1";
//    }

    /**
     * Registers a {@see Smartphone} to the service.
     *
     * @return the phoneId generated or -1 if there was a error.
     */
    @ResponseBody
    @RequestMapping(value = "/smartphone", method = RequestMethod.POST)
    public Integer registerSmartphone(@RequestBody String smartphoneString) {
        try {
            LOGGER.info(smartphoneString);
            Smartphone smartphone = new ObjectMapper().readValue(smartphoneString, Smartphone.class);
            smartphone = modelManager.registerSmartphone(smartphone);
            LOGGER.info("register Smartphone: Device:" + smartphone.getId());
            LOGGER.info("register Smartphone: Device Sensor Rules:" + smartphone.getSensorsRules());
            LOGGER.info("register Smartphone: Device Type:" + smartphone.getDeviceType());
            LOGGER.info("----------------------.-------------");
            return smartphone.getId();
        } catch (Exception e) {
            LOGGER.error(e, e);
            LOGGER.debug(e.getMessage());
        }
        return -1;
    }

    /**
     * Lists all avalialalbe plugins in the system.
     *
     * @return a json list of all available plugins in the system.
     */
    @ResponseBody
    @RequestMapping(value = "/plugin", method = RequestMethod.GET, produces = "application/json")
    public Set<Plugin> getPluginList() {
        Set<Plugin> plugins = modelManager.getPlugins();
        LOGGER.info("getPlugins Called: " + plugins);
        return plugins;
    }

    @ResponseBody
    @RequestMapping(value = "/experiment", method = RequestMethod.GET, produces = "application/json")
    public List<Experiment> getExperiment(@RequestParam(value = "phoneId", required = false, defaultValue = "0") final int phoneId) {
        try {
            if (phoneId == 0) {
                return modelManager.getEnabledExperiments();
            } else {
                final Smartphone smartphone = smartphoneRepository.findByPhoneId(phoneId);
                final Experiment experiment = modelManager.getExperiment(smartphone);
                LOGGER.debug("getExperiment: Device:" + phoneId);
                LOGGER.debug("getExperiment:" + experiment);
                LOGGER.debug("-----------------------------------");
                final ArrayList<Experiment> list = new ArrayList<Experiment>();
                list.add(experiment);
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.debug(e.getMessage());
        }
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/experiment", method = RequestMethod.POST, produces = "application/json")
    public JSONObject saveExperiment(@RequestBody final String body, final HttpServletResponse response) throws JSONException, IOException {
        System.out.println("saveExperiment Called");
        LOGGER.info("saveExperiment Called");
        Report result = new ObjectMapper().readValue(body, Report.class);
        LOGGER.info("saving for deviceId:" + result.getDeviceId() + " jobName:" + result.getJobName());
        final Smartphone phone = smartphoneRepository.findById(result.getDeviceId());
        final Experiment experiment = experimentRepository.findById(Integer.parseInt(result.getJobName()));
        LOGGER.info("saving for PhoneId:" + phone.getPhoneId() + " ExperimentName:" + experiment.getName());

        final List<Result> results = new ArrayList<Result>();
        for (final String jobResult : result.getJobResults()) {
            LOGGER.info(jobResult);
            Reading readingObj = new ObjectMapper().readValue(jobResult, Reading.class);
            final String value = readingObj.getValue();
            final long readingTime = readingObj.getTimestamp();
            final Result newResult = new Result();
            newResult.setDeviceId(phone.getId());
            newResult.setExperimentId(experiment.getId());
            newResult.setMessage(value);
            newResult.setTimestamp(readingTime);
            results.add(newResult);

//            boolean res = influxDbService.store(newResult);
//            LOGGER.info(res);

        }

        LOGGER.info("saving " + results.size() + " results");
        try {
            resultRepository.save(results);
            LOGGER.info("saveExperiment: OK");
            LOGGER.info("-----------------------------------");
            return ok(response);
        } catch (Exception e) {
            LOGGER.info("saveExperiment: FAILEd" + e.getMessage(), e);
            LOGGER.info("-----------------------------------");
            return internalServerError(response);
        }

    }

    @ResponseBody
    @RequestMapping(value = "/statistics/{phoneId}", method = RequestMethod.GET, produces = "application/json")
    public Map<Integer, Long> statisticsByPhone(@PathVariable("phoneId") final String phoneId, final HttpServletResponse response) throws JSONException, IOException {

        LOGGER.info("saveExperiment Called");
        Map<Integer, Long> counters = new HashMap<Integer, Long>();

        DateTime date = new DateTime();
        final Set<Result> results = resultRepository.findByDeviceIdAndTimestampAfter(Integer.parseInt(phoneId), date.withMillisOfDay(0).getMillis());
        long prev = results.size();
        long totalPrev = results.size();
        counters.put(0, prev);

        for (int i = 1; i < 7; i++) {
            long now = date.minusDays(i).withMillisOfDay(0).getMillis();
            final Set<Result> results1 = resultRepository.findByDeviceIdAndTimestampAfter(Integer.parseInt(phoneId), now);
            counters.put(i, results1.size() - totalPrev);
            totalPrev += results1.size();
        }
        return counters;
    }

    @ResponseBody
    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = "text/plain")
    public JSONObject ping(final String pingJson, final HttpServletResponse response) throws JSONException {
        LOGGER.debug("Ping:" + pingJson);
        LOGGER.debug("-----------------------------------");
        return ok(response);
    }

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
        } else {
            results = resultRepository.findByExperimentIdAndDeviceIdAndTimestampAfter(Integer.parseInt(experiment), deviceId, start);
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
