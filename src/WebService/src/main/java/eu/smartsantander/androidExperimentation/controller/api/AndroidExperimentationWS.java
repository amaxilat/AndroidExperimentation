package eu.smartsantander.androidExperimentation.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.organicity.entities.namespace.OrganicityAttributeTypes;
import eu.smartsantander.androidExperimentation.GcmMessageData;
import eu.smartsantander.androidExperimentation.controller.BaseController;
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
import java.util.*;

@Controller
@RequestMapping(value = "/api/v1")
public class AndroidExperimentationWS extends BaseController {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(AndroidExperimentationWS.class);
    private static final int LIDIA_PHONE_ID = 11;
    private static final int MYLONAS_PHONE_ID = 6;


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


    /**
     * Lists all avalialalbe plugins in the system.
     *
     * @return a json list of all available plugins in the system.
     */
    @ResponseBody
    @RequestMapping(value = "/plugin", method = RequestMethod.GET, produces = "application/json")
    public Set<Plugin> getPluginList(@RequestParam(value = "phoneId", required = false, defaultValue = "0") final int phoneId) {
        Experiment experiment = modelManager.getEnabledExperiments().get(0);
        if (phoneId == LIDIA_PHONE_ID || phoneId == MYLONAS_PHONE_ID) {
            experiment = experimentRepository.findById(7);
        }
        Set<String> dependencies = new HashSet<>();
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
            if (phoneId == LIDIA_PHONE_ID || phoneId == MYLONAS_PHONE_ID) {
                ArrayList<Experiment> experiements = new ArrayList<>();
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
    public JSONObject saveExperiment(@RequestBody String body, final HttpServletResponse response) throws
            JSONException, IOException {
        LOGGER.info("saveExperiment Called");

        body = body.replaceAll("org.ambientdynamix.contextplugins.10pm", OrganicityAttributeTypes.Types.PARTICLES10.getUrn())
                .replaceAll("org.ambientdynamix.contextplugins.25pm", OrganicityAttributeTypes.Types.PARTICLES25.getUrn())
                .replaceAll("org.ambientdynamix.contextplugins.co", OrganicityAttributeTypes.Types.CARBON_MONOXIDE.getUrn())
                .replaceAll("org.ambientdynamix.contextplugins.lpg", OrganicityAttributeTypes.Types.LPG.getUrn())
                .replaceAll("org.ambientdynamix.contextplugins.ch4", OrganicityAttributeTypes.Types.METHANE.getUrn())
                .replaceAll("org.ambientdynamix.contextplugins.temperature", OrganicityAttributeTypes.Types.TEMPERATURE.getUrn())
                .replaceAll("org.ambientdynamix.contextplugins.battery%", OrganicityAttributeTypes.Types.BATTERY_LEVEL.getUrn())
                .replaceAll("org.ambientdynamix.contextplugins.batteryv", OrganicityAttributeTypes.Types.BATTERY_VOLTAGE.getUrn());
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
                                             final HttpServletResponse response) {

        final Map<Long, Long> counters = new HashMap<>();
        for (long i = 0; i <= 7; i++) {
            counters.put(i, 0L);
        }

        final DateTime date = new DateTime().withMillisOfDay(0);
        final Set<Result> results = resultRepository.findByDeviceIdAndTimestampAfter(Integer.parseInt(phoneId), date.minusDays(7).getMillis());
        final Map<DateTime, Long> datecounters = new HashMap<>();
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
