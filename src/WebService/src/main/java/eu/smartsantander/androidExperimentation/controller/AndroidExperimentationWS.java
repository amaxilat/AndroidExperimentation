package eu.smartsantander.androidExperimentation.controller;

import com.google.gson.Gson;
import eu.smartsantander.androidExperimentation.entities.Report;
import eu.smartsantander.androidExperimentation.model.Experiment;
import eu.smartsantander.androidExperimentation.model.Plugin;
import eu.smartsantander.androidExperimentation.model.Smartphone;
import eu.smartsantander.androidExperimentation.repository.SmartphoneRepository;
import eu.smartsantander.androidExperimentation.service.ModelManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
@RequestMapping(value = "/api/v1")
public class AndroidExperimentationWS {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger log = Logger.getLogger(AndroidExperimentationWS.class);


    @Autowired
    ModelManager modelManager;
    @Autowired
    SmartphoneRepository smartphoneRepository;

    //@ResponseBody
    //@RequestMapping(value = "/report")
    public String reportResults(String reportJson) {
        try {
            Report report = Report.fromJson(reportJson);
            modelManager.reportResults(report);
            log.debug("Report Stored: Device:" + report.getDeviceId());
            log.debug("Report Stored:" + reportJson);
            log.debug("-----------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
            log.debug(e.getMessage());
        }
        return "1";
    }

    /**
     * Registers a {@see Smartphone} to the service.
     *
     * @param smartphone the {@see Smartphone} object to register.
     * @return the phoneId generated or -1 if there was a error.
     */
    @ResponseBody
    @RequestMapping(value = "/smartphone", method = RequestMethod.POST, produces = "text/plain")
    public Integer registerSmartphone(@ModelAttribute("smartphone") Smartphone smartphone) {
        try {
            smartphone = modelManager.registerSmartphone(smartphone);
            log.debug("register Smartphone: Device:" + smartphone.getId());
            log.debug("register Smartphone: Device Sensor Rules:" + smartphone.getSensorsRules());
            log.debug("register Smartphone: Device Type:" + smartphone.getDeviceType());
            log.debug("-----------------------------------");
            return smartphone.getPhoneId();
        } catch (Exception e) {
            log.error(e, e);
            log.debug(e.getMessage());
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
        log.info("getPlugins Called: " + plugins);
        return plugins;
    }

    @ResponseBody
    @RequestMapping(value = "/experiment", method = RequestMethod.GET, produces = "application/json")
    public Experiment getExperiment(@RequestParam("phoneId") final int phoneId) {
        try {
            Smartphone smartphone = smartphoneRepository.findByPhoneId(phoneId);
            Experiment experiment = modelManager.getExperiment(smartphone);
            log.debug("getExperiment: Device:" + phoneId);
            log.debug("getExperiment:" + experiment);
            log.debug("-----------------------------------");
            return experiment;
        } catch (Exception e) {
            e.printStackTrace();
            log.debug(e.getMessage());
        }
        return null;
    }

    //@ResponseBody
    //@RequestMapping(value = "/experiment", method = RequestMethod.POST, produces = "application/json")
    public boolean saveExperiment(String experimentJson) {
        Gson gson = new Gson();
        Experiment experiment = gson.fromJson(experimentJson, Experiment.class);
        log.debug("saveExperiment:" + experimentJson);
        try {
            modelManager.saveExperiment(experiment);
            log.debug("saveExperiment: OK");
            log.debug("-----------------------------------");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("saveExperiment: FAILEd" + e.getMessage());
            log.debug("-----------------------------------");
            return false;
        }
    }

    @ResponseBody
    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = "text/plain")
    public String Ping(String pingJson) {
        log.debug("Ping:" + pingJson);
        log.debug("-----------------------------------");
        return "1";
    }
}
