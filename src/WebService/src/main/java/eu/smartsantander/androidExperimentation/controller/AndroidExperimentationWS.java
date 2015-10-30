package eu.smartsantander.androidExperimentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.smartsantander.androidExperimentation.entities.Reading;
import eu.smartsantander.androidExperimentation.entities.Report;
import eu.smartsantander.androidExperimentation.model.*;
import eu.smartsantander.androidExperimentation.repository.ExperimentRepository;
import eu.smartsantander.androidExperimentation.repository.ResultRepository;
import eu.smartsantander.androidExperimentation.repository.SmartphoneRepository;
import eu.smartsantander.androidExperimentation.service.InfluxDbService;
import eu.smartsantander.androidExperimentation.service.ModelManager;
import eu.smartsantander.androidExperimentation.service.OrionService;
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


    @PostConstruct
    public void init() {
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

    @RequestMapping(value = "/devices/{entity_id}/readings", method = RequestMethod.GET)
    public HistoricData experimentView(final Map<String, Object> model, @PathVariable("entity_id") final int entityId,
                                       @RequestParam(value = "attribute_id") final String attributeId,
                                       @RequestParam(value = "from") final String from,
                                       @RequestParam(value = "to") final String to,
                                       @RequestParam(value = "function") final String function) throws JSONException {


        HistoricData historicData = new HistoricData();
        historicData.setEntiry_id(entityId);
        historicData.setAttribute_id(attributeId);
        historicData.setFrom(from);
        historicData.setTo(to);
        historicData.setReadings(new ArrayList<List<Object>>());

        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);


        long fromLong;
        long toLong;
        try {

            fromLong = df.parse(from).getTime();
            toLong = df.parse(to).getTime();

            Set<Result> results = resultRepository.findByDeviceIdAndTimestampBetween(entityId, fromLong, toLong);

            for (Result result : results) {
                final JSONObject readingList = new JSONObject(result.getMessage());

                final Iterator<String> keys = readingList.keys();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    if (key.contains(attributeId)) {
                        List<Object> list = new ArrayList<Object>();
                        list.add(df.format(result.getTimestamp()));
                        list.add(readingList.getDouble(key));
                        historicData.getReadings().add(list);
                    }
                }
            }
        } catch (ParseException e) {
            LOGGER.error(e, e);
        }


        return historicData;
    }


    /**
     * Lists all avalialalbe plugins in the system.
     *
     * @return a json list of all available plugins in the system.
     */
    @ResponseBody
    @RequestMapping(value = "/plugin", method = RequestMethod.GET, produces = "application/json")
    public Set<Plugin> getPluginList() {
        Experiment experiemnt = modelManager.getEnabledExperiments().get(0);
        Set<String> dependencies = new HashSet<String>();
        for (String dependency : experiemnt.getSensorDependencies().split(",")) {
            dependencies.add(dependency);
        }
        Set<Plugin> plugins = modelManager.getPlugins(dependencies);
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
    @RequestMapping(value = "/experiment", method = RequestMethod.POST, produces = "text/plain", consumes = "text/plain")
    public JSONObject saveExperiment(@RequestBody final String body, final HttpServletResponse response) throws JSONException, IOException {
        LOGGER.info("saveExperiment Called");
        Report result = new ObjectMapper().readValue(body, Report.class);
        LOGGER.info("saving for deviceId:" + result.getDeviceId() + " jobName:" + result.getJobName());
        final Smartphone phone = smartphoneRepository.findById(result.getDeviceId());
        final Experiment experiment = experimentRepository.findById(Integer.parseInt(result.getJobName()));
        LOGGER.info("saving for PhoneId:" + phone.getPhoneId() + " ExperimentName:" + experiment.getName());

        final List<Result> results = new ArrayList<Result>();
        for (final String jobResult : result.getJobResults()) {

            final Reading readingObj = new ObjectMapper().readValue(jobResult, Reading.class);
            final String value = readingObj.getValue();
            final long readingTime = readingObj.getTimestamp();
            final Result newResult = new Result();
            final Set<Result> res =
                    resultRepository.findByExperimentIdAndDeviceIdAndTimestampAndMessage(experiment.getId(), phone.getId(), readingTime, value);
            if (!res.isEmpty()) {
                continue;
            }
            LOGGER.info(jobResult);
            newResult.setDeviceId(phone.getId());
            newResult.setExperimentId(experiment.getId());
            newResult.setMessage(value);
            newResult.setTimestamp(readingTime);
            results.add(newResult);

            orionService.storeOrion(String.valueOf(phone.getId()), newResult);

//            boolean res = influxDbService.store(newResult);
//            LOGGER.info(res);

        }

        LOGGER.info("saving " + results.size() + " results");
        try {
            if (!results.isEmpty()) {
                resultRepository.save(results);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            LOGGER.info("saveExperiment: OK");
            LOGGER.info("saveExperiment: Stored:" + results.size());
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
    public Map<Long, Long> statisticsByPhone(@PathVariable("phoneId") final String phoneId, final HttpServletResponse response) throws JSONException, IOException {

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
