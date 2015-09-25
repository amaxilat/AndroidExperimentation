package eu.smartsantander.androidExperimentation.service;

import eu.smartsantander.androidExperimentation.Application;
import eu.smartsantander.androidExperimentation.entities.Report;
import eu.smartsantander.androidExperimentation.model.Experiment;
import eu.smartsantander.androidExperimentation.model.Plugin;
import eu.smartsantander.androidExperimentation.model.Result;
import eu.smartsantander.androidExperimentation.model.Smartphone;
import eu.smartsantander.androidExperimentation.repository.ExperimentRepository;
import eu.smartsantander.androidExperimentation.repository.PluginRepository;
import eu.smartsantander.androidExperimentation.repository.ResultRepository;
import eu.smartsantander.androidExperimentation.repository.SmartphoneRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
public class ModelManager {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger log = Logger.getLogger(Application.class);

    @Autowired
    PluginRepository pluginRepository;
    @Autowired
    SmartphoneRepository smartphoneRepository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    ResultRepository resultRepository;

    public Set<Plugin> getPlugins() {
        log.info("getPlugins Called");
        return pluginRepository.findAll();
    }


    public Experiment getExperiment(final Smartphone smartphone) {

        Smartphone device = smartphoneRepository.findByPhoneId(smartphone.getPhoneId());
        device.setSensorsRules(smartphone.getSensorsRules());
        String[] smartphoneDependencies = smartphone.getSensorsRules().split(",");

        Iterator<Experiment> experimentsListIterator = experimentRepository.findAll().iterator();
        Experiment experiment = null;
        do {
            experiment = experimentsListIterator.next();
        } while ((experimentsListIterator.hasNext()));
        if (experiment != null) {
            String[] experimentDependencies = experiment.getSensorDependencies().split(",");
            if (!experiment.getStatus().equals("finished")
                    && match(smartphoneDependencies, experimentDependencies)) {
                return experiment;
            }
        }
        log.info("getExperiment Called");
        return null;
    }

    public Experiment getExperiment() {

        Iterable<Experiment> experimentsList = experimentRepository.findAll();
        for (Experiment exp : experimentsList) {
            if (exp.getStatus().equals("active")) return exp;
        }
        log.info("getExperiment Called");
        return null;
    }

    public List<Experiment> getExperiments() {

        Iterator<Experiment> experimentsListIterator = experimentRepository.findAll().iterator();
        List<Experiment> experimentsList = new ArrayList<Experiment>();
        while (experimentsListIterator.hasNext()) {
            experimentsList.add(experimentsListIterator.next());
        }
        log.info("getExperiment Called");
        return experimentsList;
    }

    public List<Experiment> getEnabledExperiments() {

        Iterator<Experiment> experimentsListIterator = experimentRepository.findAll().iterator();
        List<Experiment> experimentsList = new ArrayList<Experiment>();
        while (experimentsListIterator.hasNext()) {
            Experiment experiment = experimentsListIterator.next();
            if (experiment.getEnabled()) {
                experimentsList.add(experiment);
            }
        }
        log.info("getExperiment Called");
        return experimentsList;
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
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public void saveExperiment(Experiment experiment) {
        log.info("saveExperiment Called");
        experimentRepository.save(experiment);
    }


    public Smartphone registerSmartphone(Smartphone smartphone) {
        if (smartphone.getId() == -1) {
            smartphone.setId(null);
        }
        log.info("registerSmartphone: id:" + smartphone.getId() + " phoneId:" + smartphone.getPhoneId());
        Smartphone phone = smartphoneRepository.findByPhoneId(smartphone.getPhoneId());
        log.info("registerSmartphone: phone:" + phone);
        if (phone == null) {
            return smartphoneRepository.save(smartphone);
        } else {
            return phone;
        }

    }

    public void reportResults(Report report) {
        String expId = report.getName();
        List<String> experimentResults = report.getResults();
        System.out.println("experiment Id: " + expId);

        Experiment experiment = experimentRepository.findById(Integer.parseInt(expId));

        for (final String result : experimentResults) {
            Result resultsEntity = new Result();
            resultsEntity.setExperimentId(experiment.getId());
            resultsEntity.setDeviceId(report.getDeviceId());
            resultsEntity.setTimestamp(System.currentTimeMillis());

            if (result != null) {
                resultsEntity.setMessage(result);
            } else {
                resultsEntity.setMessage("");
            }

            resultRepository.save(resultsEntity);
        }
    }


    public List<Result> getResults(Integer experimentId) {
        if (experimentId == null)
            return new ArrayList<Result>();
        final Set<Result> resultSet = resultRepository.findByExperimentId(experimentId);
        return new ArrayList<Result>(resultSet);
    }

    public List<Result> getLastResults(Integer experimentId) {
//        TODO: Replace
        return getResults(experimentId);

//        if (experimentId == null)
//            return new ArrayList<Result>();
//        Query q = getCurrentSession().createQuery("from Result where experimentId = :expId order by timestamp desc");
//        q.setFirstResult(0);
//        q.setMaxResults(50);
//        q.setParameter("expId", Integer.valueOf(experimentId));
//        return (List<Result>) q.list();
    }

    public Long getResultSize(Integer experimentId) {
        if (experimentId == null)
            return 0L;
        return Long.valueOf(getResults(experimentId).size());
    }


    public String[] getResults(Integer experimentId, Long timestamp, int deviceID) {
        //TODO: replace
        return new String[]{""};
//        Query q = getCurrentSession().createQuery("from Result where experimentId = :expId AND deviceId= :devId AND timestamp>= :t");
//        q.setParameter("expId", Integer.valueOf(experimentId));
//        q.setParameter("devId", Integer.valueOf(deviceID));
//        q.setParameter("t", Long.valueOf(timestamp));
//        List<Result> results = (List<Result>) q.list();
//        if (results.size() == 0) return null;
//
//        HashMap<Long, Result> resultMap = new HashMap<Long, Result>();
//        for (Result result : results) {
//            if (result.getMessage() == null || result.getMessage().equals("")) continue;
//            Reading r;
//            try {
//                r = Reading.fromJson(result.getMessage());
//            } catch (Exception e) {
//                //e.printStackTrace();
//                continue;
//            }
//            resultMap.put(r.getTimestamp(), result);
//        }
//        Object[] times = resultMap.keySet().toArray();
//        Arrays.sort(times);
//        String[] messages = new String[times.length];
//        int i = 0;
//        for (Object time : times) {
//            messages[i++] = resultMap.get(time).getMessage();
//        }
//        return messages;
    }


    public String durationString(long millis) {
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

    public String getDailyStats(Long timestamp1, int deviceID) {
        return "";
//
//        // create and additional timestamp for the end of same day, 24 hours minus 1 millisecond
//        Long timestamp2 = timestamp1 + (24 * 60 * 60 * 1000) - 1;
//
//        Query q = getCurrentSession().createQuery("select count(*) from Result where deviceId= :devId and timestamp>= :t1 and timestamp<= :t2");
//
//        q.setParameter("devId", Integer.valueOf(deviceID));
//        q.setParameter("t1", Long.valueOf(timestamp1));
//        q.setParameter("t2", Long.valueOf(timestamp2));
//        if (q.list().size() > 0)
//            return String.valueOf(q.list().get(0));
//        else
//            return "0";
//        //String msgTotal = String.valueOf(results.size());
//
//        //return msgTotal;

    }


    // returns the total number of messages produced by a single device during a specific week as an array
    // with a number per day per array position. It takes as input a timestamp pointing to 00:00 of that day.
    // Used for displaying stats for each device over the web, it utilizes getDailyStats to get stats for each day.

    public String[] getWeeklyStats(Long timestamp, int deviceID) {

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


    public float pert() {
        Random r = new Random();
        int x = r.nextInt(3);
        float y = (float) x / 1000;
        return y;
    }

    public String formatDouble(Float d) {
        return String.format("%.6f", d).replace(',', '.');
    }


    public String assignColor(HashMap<Integer, String> deviceColors, Integer deviceID) {
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
