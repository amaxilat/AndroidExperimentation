package eu.smartsantander.androidExperimentation;

import eu.smartsantander.androidExperimentation.entities.Experiment;
import eu.smartsantander.androidExperimentation.entities.Plugin;
import eu.smartsantander.androidExperimentation.entities.Result;
import eu.smartsantander.androidExperimentation.entities.Smartphone;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import eu.smartsantander.androidExperimentation.jsonEntities.Report;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class ModelManager {
    private static final Log log = LogFactory.getLog(ModelManager.class);

    public ModelManager() {
    }

    protected static Session getCurrentSession() {
        try {
            return HibernateUtil.currentSession();
        } catch (Exception e) {
            throw new IllegalStateException("Could not get current session");
        }
    }


    public static List<Plugin> getPluginList() throws Exception {
        log.info("getPlugins Called");
        return getCurrentSession().createQuery("from Plugin").list();
    }

    public static PluginList getPlugins() throws Exception {
        List<Plugin> pluginsList = getCurrentSession().createQuery("from Plugin").list();
        PluginList pluginList = new PluginList();
        pluginList.setPluginList(pluginsList);
        log.info("getPlugins Called");
        return pluginList;
    }


    public static Experiment getExperiment(Smartphone smartphone) {
        List<Smartphone> recordList = getCurrentSession().createQuery("from Smartphone where phoneId=?").setInteger(0, smartphone.getPhoneId()).list();
        if (recordList.size() > 0) {
            Smartphone device = recordList.get(0);
            device.setSensorsRules(smartphone.getSensorsRules());
            getCurrentSession().update(device);
            String[] smartphoneDependencies = smartphone.getSensorsRules().split(",");

            if (recordList.size() == 1) {
                List<Experiment> experimentsList = getCurrentSession().createQuery("from Experiment").list();
                if (experimentsList.size() > 0) {
                    Experiment experiment = experimentsList.get(experimentsList.size() - 1);
                    String[] experimentDependencies = experiment.getSensorDependencies().split(",");
                    if (experiment.getStatus().equals("finished") == false
                            && match(smartphoneDependencies, experimentDependencies) == true) {
                        return experiment;
                    }
                }
            }
        }
        log.info("getExperiment Called");
        return null;
    }

    public static Experiment getExperiment() {

        List<Experiment> experimentsList = getCurrentSession().createQuery("from Experiment").list();
        if (experimentsList.size() > 0) {
            Experiment experiment = experimentsList.get(experimentsList.size() - 1);
            return experiment;
        }
        log.info("getExperiment Called");
        return null;
    }

    public static List<Experiment> getExperiments() {

        List<Experiment> experimentsList = getCurrentSession().createQuery("from Experiment").list();
        if (experimentsList.size() > 0) {
            return experimentsList;
        }
        log.info("getExperiment Called");
        return new ArrayList<Experiment>();
    }


    private static boolean match(String[] smartphoneDependencies, String[] experimentDependencies) {
        for (String expDependency : experimentDependencies) {
            boolean found = false;
            for (String smartphoneDependency : smartphoneDependencies) {
                if (smartphoneDependency.equals(expDependency)) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                return false;
            }
        }
        return true;
    }

    public static void saveExperiment(Experiment experiment) {
        getCurrentSession().saveOrUpdate(experiment);
        getCurrentSession().flush();
        log.info("saveExperiment Called");
    }


    public static Smartphone registerSmartphone(Smartphone smartphone) {
        int phoneId = smartphone.getPhoneId();
        String sensors_rules = smartphone.getSensorsRules();
        Session session = getCurrentSession();
        Transaction tx = session.beginTransaction();
        session.saveOrUpdate(smartphone);
        smartphone.setPhoneId(smartphone.getId());
        session.saveOrUpdate(smartphone);
        tx.commit();
        return smartphone;
    }

    public static void reportResults(Report report) {
        String contextType = report.getName();
        List<String> experimentResults = report.getResults();
        System.out.println("contextType: " + contextType);

        Transaction tx = getCurrentSession().beginTransaction();

        Query q = getCurrentSession().createQuery("from Experiment where contextType = :contextType ");
        q.setParameter("contextType", contextType);
        Experiment experiment = (Experiment) q.list().get(0);
        getCurrentSession().update(experiment);

        for (String result : experimentResults) {
            Result resultsEntity = new Result();
            resultsEntity.setExperimentId(experiment.getId());
            resultsEntity.setDeviceId(report.getDeviceId());

            if (result != null) {
                resultsEntity.setMessage(result);
            } else {
                resultsEntity.setMessage("");
            }

            getCurrentSession().save(resultsEntity);
            getCurrentSession().flush();
        }
        tx.commit();
    }
}
