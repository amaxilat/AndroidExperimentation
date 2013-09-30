package eu.smartsantander.androidExperimentation;

import com.google.gson.Gson;
import eu.smartsantander.androidExperimentation.entities.Experiment;
import eu.smartsantander.androidExperimentation.entities.Smartphone;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import eu.smartsantander.androidExperimentation.jsonEntities.Report;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class AndroidExperimentationWS {

    @WebMethod
    public String reportResults(String reportJson) {
        Gson gson = new Gson();
        Report report = gson.fromJson(reportJson, Report.class);
        ModelManager.reportResults(report);

        return "1";
    }


    @WebMethod
    public String registerSmartphone(String smartphoneJson) {
        Gson gson = new Gson();
        Smartphone smartphone = gson.fromJson(smartphoneJson, Smartphone.class);
        smartphone = ModelManager.registerSmartphone(smartphone);
        return Integer.toString(smartphone.getPhoneId());
    }

    @WebMethod
    public String getPluginList() throws Exception {
        Gson gson = new Gson();
        PluginList pluginList = ModelManager.getPlugins();
        String jsonPluginList = "";
        jsonPluginList = gson.toJson(pluginList);
        System.out.println(jsonPluginList);
        return jsonPluginList;
    }


    @WebMethod
    public String getExperiment(String smartphoneJson) {
        Gson gson = new Gson();
        Smartphone smartphone = gson.fromJson(smartphoneJson, Smartphone.class);
        Experiment exp = ModelManager.getExperiment(smartphone);
        if (exp == null) return "0";
        return gson.toJson(exp, Experiment.class);
    }

    @WebMethod
    public boolean saveExperiment(String experimentJson) {
        Gson gson = new Gson();
        Experiment experiment = gson.fromJson(experimentJson, Experiment.class);
        try {
            ModelManager.saveExperiment(experiment);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @WebMethod
    public String Ping(String pingJson) {
        return "1";
    }
}
