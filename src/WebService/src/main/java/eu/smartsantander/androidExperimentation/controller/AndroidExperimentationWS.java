package eu.smartsantander.androidExperimentation.controller;

import com.google.gson.Gson;
import eu.smartsantander.androidExperimentation.service.ModelManager;
import eu.smartsantander.androidExperimentation.entities.PluginList;
import eu.smartsantander.androidExperimentation.entities.Report;
import eu.smartsantander.androidExperimentation.model.Experiment;
import eu.smartsantander.androidExperimentation.model.Smartphone;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebMethod;
import javax.jws.WebService;
import java.io.IOException;
import java.util.Properties;

@WebService
public class AndroidExperimentationWS {
    private static final Log log = LogFactory.getLog(ModelManager.class);

    @Autowired
    ModelManager modelManager;

    public AndroidExperimentationWS() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("log4j.properties"));
            PropertyConfigurator.configure(props);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    @WebMethod
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


    @WebMethod
    public String registerSmartphone(String smartphoneJson) {
        try {
            Gson gson = new Gson();
            Smartphone smartphone = gson.fromJson(smartphoneJson, Smartphone.class);
            smartphone = modelManager.registerSmartphone(smartphone);
            log.debug("register Smartphone: Device:" + smartphone.getId());
            log.debug("register Smartphone: Device Sensor Rules:" + smartphone.getSensorsRules());
            log.debug("register Smartphone: Device Type:" + smartphone.getDeviceType());
            log.debug("-----------------------------------");
            return Integer.toString(smartphone.getPhoneId());
        } catch (Exception e) {
            e.printStackTrace();
            log.debug(e.getMessage());
        }
        return "-1";
    }

    @WebMethod
    public String getPluginList() throws Exception {
        try {
            Gson gson = new Gson();
            PluginList pluginList = modelManager.getPlugins();
            String jsonPluginList = "";
            jsonPluginList = gson.toJson(pluginList);
            log.debug("getPluginList:" + jsonPluginList);
            log.debug("-----------------------------------");
            return jsonPluginList;
        } catch (Exception e) {
            e.printStackTrace();
            log.debug(e.getMessage());
        }
        return "";
    }


    @WebMethod
    public String getExperiment(String smartphoneJson) {
        try {
            Gson gson = new Gson();
            Smartphone smartphone = gson.fromJson(smartphoneJson, Smartphone.class);
            Experiment exp = modelManager.getExperiment(smartphone);
            log.debug("getExperiment: Device:" + smartphoneJson);
            String experiment = "";
            if (exp == null) {
                experiment = "0";
            } else {
                experiment = gson.toJson(exp, Experiment.class);
            }
            log.debug("getExperiment:" + experiment);
            log.debug("-----------------------------------");
            return experiment;
        } catch (Exception e) {
            e.printStackTrace();
            log.debug(e.getMessage());
        }
        return "0";
    }

    @WebMethod
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


    @WebMethod
    public String Ping(String pingJson) {
        log.debug("Ping:" + pingJson);
        log.debug("-----------------------------------");
        return "1";
    }
}
