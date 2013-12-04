package eu.smartsantander.androidExperimentation;

import eu.smartsantander.androidExperimentation.entities.*;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;
import eu.smartsantander.androidExperimentation.jsonEntities.Report;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    protected static void closeCurrentSession() {
        try {
              HibernateUtil.closeSession();
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
        try {
            Transaction tx = getCurrentSession().beginTransaction();
            getCurrentSession().saveOrUpdate(smartphone);
            tx.commit();
        } catch (Exception e) {
            getCurrentSession().getTransaction().commit();
            closeCurrentSession();
        }
        List<Smartphone> recordList = getCurrentSession().createQuery("from Smartphone where phoneId=?").setInteger(0, smartphone.getPhoneId()).list();
        if (recordList.size() > 0) {
            Smartphone device = recordList.get(0);
            device.setSensorsRules(smartphone.getSensorsRules());
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
        for (Experiment exp : experimentsList) {
            if (exp.getStatus().equals("active")) return exp;
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
        Transaction tx = getCurrentSession().beginTransaction();
        Experiment current = getExperiment();
        if (current != null) {
            current.setStatus("");
            getCurrentSession().saveOrUpdate(current);
        }
        getCurrentSession().saveOrUpdate(experiment);
        tx.commit();
        log.info("saveExperiment Called");
    }


    public static Smartphone registerSmartphone(Smartphone smartphone) {
        if (smartphone.getId() == -1) {
            smartphone.setId(null);
        }
        Transaction tx = getCurrentSession().beginTransaction();
        getCurrentSession().saveOrUpdate(smartphone);
        tx.commit();
        return smartphone;
    }

    public static void reportResults(Report report) {
        String expId = report.getName();
        List<String> experimentResults = report.getResults();
        System.out.println("experiment Id: " + expId);

        Transaction tx = getCurrentSession().beginTransaction();

        Query q = getCurrentSession().createQuery("from Experiment where id = :expId ");
        q.setParameter("expId", Integer.valueOf(expId));
        Experiment experiment = (Experiment) q.list().get(0);
        getCurrentSession().update(experiment);

        for (String result : experimentResults) {
            Result resultsEntity = new Result();
            resultsEntity.setExperimentId(experiment.getId());
            resultsEntity.setDeviceId(report.getDeviceId());
            resultsEntity.setTimestamp(System.currentTimeMillis());

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


    public static List<Result> getResults(Integer experimentId) {
        if (experimentId == null)
            return new ArrayList<Result>();
        Query q = getCurrentSession().createQuery("from Result where experimentId = :expId order by id asc");
        q.setParameter("expId", Integer.valueOf(experimentId));
        return (List<Result>) q.list();
    }

    public static List<Result> getLastResults(Integer experimentId) {
        if (experimentId == null)
            return new ArrayList<Result>();
        Query q = getCurrentSession().createQuery("from Result where experimentId = :expId order by timestamp desc");
        q.setFirstResult(0);
        q.setMaxResults(50);
        q.setParameter("expId", Integer.valueOf(experimentId));
        return (List<Result>) q.list();
    }

    public static Long getResultSize(Integer experimentId) {
        if (experimentId == null)
            return 0L;
        Query q = getCurrentSession().createQuery("Select count(*) from Result where experimentId = :expId ");
        q.setParameter("expId", Integer.valueOf(experimentId));
        Object x = q.list().get(0);
        String xval = String.valueOf(x);
        return Long.valueOf(xval);
    }


    public static String[] getResults(Integer experimentId, Long timestamp, int deviceID) {
        Query q = getCurrentSession().createQuery("from Result where experimentId = :expId AND deviceId= :devId AND timestamp>= :t");
        q.setParameter("expId", Integer.valueOf(experimentId));
        q.setParameter("devId", Integer.valueOf(deviceID));
        q.setParameter("t", Long.valueOf(timestamp));
        List<Result> results = (List<Result>) q.list();
        if (results.size() == 0) return null;

        HashMap<Long, Result> resultMap = new HashMap<Long, Result>();
        for (Result result : results) {
            if (result.getMessage() == null || result.getMessage().equals("")) continue;
            Reading r;
            try {
                r = Reading.fromJson(result.getMessage());
            } catch (Exception e) {
                //e.printStackTrace();
                continue;
            }
            resultMap.put(r.getTimestamp(), result);
        }
        Object[] times = resultMap.keySet().toArray();
        Arrays.sort(times);
        String[] messages = new String[times.length];
        int i = 0;
        for (Object time : times) {
            messages[i++] = resultMap.get(time).getMessage();
        }
        return messages;
    }


    public static String durationString(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        sb.append(days);
        sb.append(" Days ");
        sb.append(hours);
        sb.append(" Hours ");
        sb.append(minutes);
        sb.append(" Minutes ");
        sb.append(seconds);
        sb.append(" Seconds");

        return (sb.toString());

    }

    // returns the total number of messages produced by a single device during a specific day
    // Used for displaying stats for each device over the web
    // timestamp1 is midnight for the beginning of a day, for 5/11/2013 we take into account readings after 00:00 hours

    public static String getDailyStats(Long timestamp1, int deviceID) {

        // create and additional timestamp for the end of same day, 24 hours minus 1 millisecond
        Long timestamp2 = timestamp1 + (24 * 60 * 60 * 1000) - 1;

        Query q = getCurrentSession().createQuery("select count(*) from Result where deviceId= :devId and timestamp>= :t1 and timestamp<= :t2");

        q.setParameter("devId", Integer.valueOf(deviceID));
        q.setParameter("t1", Long.valueOf(timestamp1));
        q.setParameter("t2", Long.valueOf(timestamp2));
        if (q.list().size() > 0)
            return String.valueOf(q.list().get(0));
        else
            return "0";
        //String msgTotal = String.valueOf(results.size());

        //return msgTotal;

    }


    // returns the total number of messages produced by a single device during a specific week as an array
    // with a number per day per array position. It takes as input a timestamp pointing to 00:00 of that day.
    // Used for displaying stats for each device over the web, it utilizes getDailyStats to get stats for each day.

    public static String[] getWeeklyStats(Long timestamp, int deviceID) {

        // dummy initialisation in case of no actual readings available
        String[] resultStats = new String[14];
        int t = 6;

        for (int i = 0; i <= 12; i += 2) {

            Long tt = timestamp - t * (24 * 60 * 60 * 1000);
            DateFormat df = new SimpleDateFormat("dd/MM/yy");
            String day = df.format(new Date(tt));
            resultStats[i] = day;
            resultStats[i + 1] = getDailyStats(tt, deviceID);
            t -= 1;
        }

        return resultStats;

    }


    public static float pert() {
        Random r = new Random();
        int x = r.nextInt(3);
        float y = (float) x / 1000;
        return y;
    }

    public static String formatDouble(Float d) {
        return String.format("%.6f", d).replace(',', '.');
    }


    public static String assignColor(HashMap<Integer, String> deviceColors, Integer deviceID) {
        String color = "http://maps.google.com/mapfiles/ms/icons/blue-dot.png";

        if (deviceColors.containsKey(deviceID)) {
            color = deviceColors.get(deviceID);
            return color;
        }
        int numOfDev = deviceColors.keySet().size();
        if (numOfDev % 6 == 0) color = "http://maps.google.com/mapfiles/ms/icons/blue-dot.png";
        else if (numOfDev % 6 == 1) color = "http://maps.google.com/mapfiles/ms/icons/red-dot.png";
        else if (numOfDev % 6 == 2) color = "http://maps.google.com/mapfiles/ms/icons/green-dot.png";
        else if (numOfDev % 6 == 3) color = "http://maps.google.com/mapfiles/ms/icons/yellow-dot.png";
        else if (numOfDev % 6 == 4) color = "http://maps.google.com/mapfiles/ms/icons/purple-dot.png";
        else if (numOfDev % 6 == 5) color = "http://maps.google.com/mapfiles/ms/icons/pink-dot.png";
        deviceColors.put(deviceID, color);
        return color;
    }

}
