package eu.smartsantander.androidExperimentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.smartsantander.androidExperimentation.GcmMessageData;
import eu.smartsantander.androidExperimentation.entities.Reading;
import eu.smartsantander.androidExperimentation.entities.Report;
import eu.smartsantander.androidExperimentation.model.*;
import eu.smartsantander.androidExperimentation.repository.ExperimentRepository;
import eu.smartsantander.androidExperimentation.repository.ResultRepository;
import eu.smartsantander.androidExperimentation.repository.SmartphoneRepository;
import eu.smartsantander.androidExperimentation.service.GCMService;
import eu.smartsantander.androidExperimentation.service.InfluxDbService;
import eu.smartsantander.androidExperimentation.service.ModelManager;
import eu.smartsantander.androidExperimentation.service.OrionService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping(value = "/api/v1")
public class AndroidExperimentationWS extends BaseController {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(AndroidExperimentationWS.class);
    private static final int LIDIA_PHONE_ID = 11;


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
    @Autowired
    OrionService orionService;
    @Autowired
    GCMService gcmService;


    @PostConstruct
    public void init() {
//        for (Result newResult : resultRepository.findAll()) {
//            try {
//                final Set<Result> res = resultRepository.findByExperimentIdAndDeviceIdAndTimestampAndMessage(newResult.getExperimentId(), newResult.getDeviceId(), newResult.getTimestamp(), newResult.getMessage());
//                if (res == null || (res.isEmpty())) {
//                } else {
//                    if (res.size() > 1) {
//                        resultRepository.delete(newResult);
//                    }
//                }
//            } catch (Exception e) {
//            }
//        }
    }

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

    @ResponseBody
    @RequestMapping(value = {"/entities/{entity_id}/readings", "/devices/{entity_id}/readings"}, method = RequestMethod.GET)
    public HistoricData experimentView(final Map<String, Object> model, @PathVariable("entity_id") final String entityId,
                                       @RequestParam(value = "attribute_id") final String attributeId,
                                       @RequestParam(value = "from") final String from,
                                       @RequestParam(value = "to") final String to,
                                       @RequestParam(value = "all_intervals", required = false, defaultValue = "true") final boolean allIntervals,
                                       @RequestParam(value = "rollup", required = false, defaultValue = "") final String rollup,
                                       @RequestParam(value = "function") final String function) throws JSONException {

        HistoricData historicData = new HistoricData();
        historicData.setEntity_id(entityId);
        historicData.setAttribute_id(attributeId);
        historicData.setFunction(function);
        historicData.setRollup(rollup);
        historicData.setFrom(from);
        historicData.setTo(to);
        historicData.setReadings(new ArrayList<>());

        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);

        List<TempReading> tempReadings = new ArrayList<>();
        long fromLong;
        long toLong;
        try {

            try {
                fromLong = Long.parseLong(from);
            } catch (NumberFormatException e) {
                fromLong = df.parse(from).getTime();
            }
            try {
                toLong = Long.parseLong(to);
            } catch (NumberFormatException e) {
                toLong = df.parse(to).getTime();
            }

            final String[] parts = entityId.split(":");
            final String phoneId = parts[parts.length - 1];

            LOGGER.info("phoneId: " + phoneId + " from: " + from + " to: " + to);

            Set<Result> results = resultRepository.findByDeviceIdAndTimestampBetween(Integer.parseInt(phoneId), fromLong, toLong);

            Set<Result> resultsCleanup = new HashSet<>();

            for (Result result : results) {
                try {
                    final JSONObject readingList = new JSONObject(result.getMessage());
                    final Iterator<String> keys = readingList.keys();
                    while (keys.hasNext()) {
                        final String key = keys.next();
                        if (key.contains(attributeId)) {
                            tempReadings.add(new TempReading(result.getTimestamp(), readingList.getDouble(key)));
                        }
                    }
                } catch (JSONException e) {
                    resultsCleanup.add(result);
                } catch (Exception e) {
                    LOGGER.error(e, e);
                }
            }
            resultRepository.delete(resultsCleanup);
        } catch (ParseException e) {
            LOGGER.error(e, e);
        }

        List<TempReading> rolledUpTempReadings = new ArrayList<>();

        if ("".equals(rollup)) {
            rolledUpTempReadings = tempReadings;
        } else {
            final Map<Long, SummaryStatistics> dataMap = new HashMap<>();
            for (final TempReading tempReading : tempReadings) {
                Long millis = null;
                if (rollup.endsWith("m")) {
                    millis = new DateTime(tempReading.getTimestamp())
                            .withMillisOfSecond(0).withSecondOfMinute(0).getMillis();
                } else if (rollup.endsWith("h")) {
                    millis = new DateTime(tempReading.getTimestamp())
                            .withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0).getMillis();
                } else if (rollup.endsWith("d")) {
                    millis = new DateTime(tempReading.getTimestamp())
                            .withMillisOfDay(0).getMillis();
                }
                if (millis != null) {
                    if (!dataMap.containsKey(millis)) {
                        dataMap.put(millis, new SummaryStatistics());
                    }
                    dataMap.get(millis).addValue(tempReading.getValue());
                }
            }
            for (final Long millis : dataMap.keySet()) {
                rolledUpTempReadings.add(parse(millis, function, dataMap.get(millis)));
            }
        }

        for (final TempReading tempReading : rolledUpTempReadings) {
            List<Object> list = new ArrayList<>();
            list.add(df.format(tempReading.getTimestamp()));
            list.add(tempReading.getValue());
            historicData.getReadings().add(list);
        }
        return historicData;
    }

    /**
     * Parse a time instant and create a TempReading object.
     *
     * @param millis     the millis of the timestamp.
     * @param function   the function to aggregate.
     * @param statistics the data values
     * @return the aggregated TempReading for this time instant.
     */
    private TempReading parse(final long millis, final String function, SummaryStatistics statistics) {
        final Double value;
        switch (function) {
            case "avg":
                value = statistics.getMean();
                break;
            case "max":
                value = statistics.getMax();
                break;
            case "min":
                value = statistics.getMin();
                break;
            case "var":
                value = statistics.getVariance();
                break;
            case "sum":
                value = statistics.getSum();
                break;
            default:
                value = statistics.getMean();
        }
        return new TempReading(millis, value);
    }


    /**
     * Lists all avalialalbe plugins in the system.
     *
     * @return a json list of all available plugins in the system.
     */
    @ResponseBody
    @RequestMapping(value = "/plugin", method = RequestMethod.GET, produces = "application/json")
    public Set<Plugin> getPluginList(@RequestParam(value = "phoneId", required = false, defaultValue = "0") final int phoneId) {
        Experiment experiment = modelManager.getEnabledExperiments().get(0);
        if (phoneId == LIDIA_PHONE_ID) {
            experiment = experimentRepository.findById(7);
        }
        Set<String> dependencies = new HashSet<String>();
        for (final String dependency : experiment.getSensorDependencies().split(",")) {
            dependencies.add(dependency);
        }
        final Set<Plugin> plugins = modelManager.getPlugins(dependencies);
        LOGGER.info("getPlugins Called: " + plugins);
        return plugins;
    }

    @ResponseBody
    @RequestMapping(value = "/experiment", method = RequestMethod.GET, produces = "application/json")
    public List<Experiment> getExperiment(@RequestParam(value = "phoneId", required = false, defaultValue = "0") final int phoneId) {
        try {
            if (phoneId == LIDIA_PHONE_ID) {
                ArrayList<Experiment> experiements = new ArrayList<Experiment>();
                experiements.add(experimentRepository.findById(7));
                return experiements;
            } else {
                return modelManager.getEnabledExperiments();
            }
//            } else {
//
//                final Smartphone smartphone = smartphoneRepository.findById(phoneId);
//                Experiment experiment = modelManager.getExperiment(smartphone);
//                if (phoneId == LIDIA_PHONE_ID) {
//                    experiment = experimentRepository.findById(7);
//                }
//                LOGGER.debug("getExperiment: Device:" + phoneId);
//                LOGGER.debug("getExperiment:" + experiment);
//                LOGGER.debug("-----------------------------------");
//                final ArrayList<Experiment> list = new ArrayList<Experiment>();
//                list.add(experiment);
//                return list;
//            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.debug(e.getMessage());
        }
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/experiment", method = RequestMethod.POST, produces = "text/plain", consumes = "text/plain")
    public JSONObject saveExperiment(@RequestBody final String body, final HttpServletResponse response) throws
            JSONException, IOException {
        LOGGER.info("saveExperiment Called");
        Report result = new ObjectMapper().readValue(body, Report.class);
        LOGGER.info("saving for deviceId:" + result.getDeviceId() + " jobName:" + result.getJobName());
        final Smartphone phone = smartphoneRepository.findById(result.getDeviceId());
        final Experiment experiment = experimentRepository.findById(Integer.parseInt(result.getJobName()));
        LOGGER.info("saving for PhoneId:" + phone.getPhoneId() + " ExperimentName:" + experiment.getName());


        final Result newResult = new Result();
        final JSONObject objTotal = new JSONObject();

        for (final String jobResult : result.getJobResults()) {

            LOGGER.info(jobResult);
            if (jobResult.isEmpty()) {
                continue;
            }

            final Reading readingObj = new ObjectMapper().readValue(jobResult, Reading.class);
            final String value = readingObj.getValue();
            final long readingTime = readingObj.getTimestamp();

            try {
                final Set<Result> res =
                        resultRepository.findByExperimentIdAndDeviceIdAndTimestampAndMessage(experiment.getId(), phone.getId(), readingTime, value);
                if (!res.isEmpty()) {
                    continue;
                }
            } catch (Exception e) {
                LOGGER.error(e, e);
            }
            LOGGER.info(jobResult);
            newResult.setDeviceId(phone.getId());
            newResult.setExperimentId(experiment.getId());
            final JSONObject obj = new JSONObject(value);
            for (final String key : JSONObject.getNames(obj)) {
                objTotal.put(key, obj.get(key));
            }
            newResult.setTimestamp(readingTime);
        }

        newResult.setMessage(objTotal.toString());

        LOGGER.info(newResult.toString());
        try {
            orionService.storeOrion(String.valueOf(phone.getId()), newResult);


            long total = resultRepository.countByDeviceIdAndTimestampAfter(phone.getId(),
                    new DateTime().withMillisOfDay(0).getMillis());

            LOGGER.info("Total measurements : " + total + " device: " + phone.getId());

            if (total == 200) {
                GcmMessageData data = new GcmMessageData();
                data.setType("encourage");
                data.setCount((int) total);
                gcmService.send2Device(phone.getId(), new ObjectMapper().writeValueAsString(data));

            } else if (total == 1000) {
                GcmMessageData data = new GcmMessageData();
                data.setType("encourage");
                data.setCount((int) total);
                gcmService.send2Device(phone.getId(), new ObjectMapper().writeValueAsString(data));
            } else {

            }
        } catch (Exception e) {
            LOGGER.error(e, e);
        }

//            boolean res = influxDbService.store(newResult);
//            LOGGER.info(res);


        LOGGER.info("saving result");
        try {
            try {
                final Set<Result> res = resultRepository.findByExperimentIdAndDeviceIdAndTimestampAndMessage(newResult.getExperimentId(), newResult.getDeviceId(), newResult.getTimestamp(), newResult.getMessage());
                if (res == null || (res.isEmpty())) {
                    resultRepository.save(newResult);
                }
            } catch (Exception e) {
                resultRepository.save(newResult);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            LOGGER.info("saveExperiment: OK");
            LOGGER.info("saveExperiment: Stored:");
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
    public Map<Long, Long> statisticsByPhone(@PathVariable("phoneId") final String phoneId,
                                             final HttpServletResponse response) throws JSONException, IOException {

        final Map<Long, Long> counters = new HashMap<Long, Long>();
        for (long i = 0; i <= 7; i++) {
            counters.put(i, 0L);
        }

        final DateTime date = new DateTime().withMillisOfDay(0);
        final Set<Result> results = resultRepository.findByDeviceIdAndTimestampAfter(Integer.parseInt(phoneId), date.minusDays(7).getMillis());
        final Map<DateTime, Long> datecounters = new HashMap<DateTime, Long>();
        for (final Result result : results) {
            final DateTime index = new DateTime(result.getTimestamp()).withMillisOfDay(0);
            if (!datecounters.containsKey(index)) {
                datecounters.put(index, 0L);
            }
            datecounters.put(index, datecounters.get(index) + 1);
        }

        for (final DateTime dateTime : datecounters.keySet()) {
            counters.put((date.getMillis() - dateTime.getMillis()) / 86400000, datecounters.get(dateTime));
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

}
