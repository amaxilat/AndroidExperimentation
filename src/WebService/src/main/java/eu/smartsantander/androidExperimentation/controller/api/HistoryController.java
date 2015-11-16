package eu.smartsantander.androidExperimentation.controller.api;

import eu.smartsantander.androidExperimentation.controller.BaseController;
import eu.smartsantander.androidExperimentation.model.HistoricData;
import eu.smartsantander.androidExperimentation.model.Result;
import eu.smartsantander.androidExperimentation.model.TempReading;
import eu.smartsantander.androidExperimentation.repository.ResultRepository;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class HistoryController extends BaseController {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(HistoryController.class);

    @Autowired
    ResultRepository resultRepository;

    @ResponseBody
    @RequestMapping(value = {"/api/v1/entities/{entity_id}/readings"}, method = RequestMethod.GET)
    public HistoricData experimentView(@PathVariable("entity_id") final String entityId,
                                       @RequestParam(value = "attribute_id") final String attributeId,
                                       @RequestParam(value = "from") final String from,
                                       @RequestParam(value = "to") final String to,
                                       @RequestParam(value = "all_intervals", required = false, defaultValue = "true") final boolean allIntervals,
                                       @RequestParam(value = "rollup", required = false, defaultValue = "") final String rollup,
                                       @RequestParam(value = "function", required = false, defaultValue = "avg") final String function) {

        final HistoricData historicData = new HistoricData();
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
        final SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df1.setTimeZone(tz);

        final List<TempReading> tempReadings = new ArrayList<>();
        long fromLong;
        long toLong = 0;
        try {

            try {
                fromLong = Long.parseLong(from);
            } catch (NumberFormatException e) {
                try {
                    fromLong = df.parse(from).getTime();
                } catch (Exception e1) {
                    fromLong = df1.parse(from).getTime();
                }
            }
            try {
                toLong = Long.parseLong(to);
            } catch (NumberFormatException e) {
                try {
                    toLong = df.parse(to).getTime();
                } catch (Exception e1) {
                    toLong = df1.parse(to).getTime();
                }
            }

            final String[] parts = entityId.split(":");
            final String phoneId = parts[parts.length - 1];

            LOGGER.info("phoneId: " + phoneId + " from: " + from + " to: " + to);

            final Set<Result> results = resultRepository.findByDeviceIdAndTimestampBetween(Integer.parseInt(phoneId), fromLong, toLong);

            final Set<Result> resultsCleanup = new HashSet<>();

            for (final Result result : results) {
                try {
                    final JSONObject readingList = new JSONObject(result.getMessage());
                    final Iterator keys = readingList.keys();
                    while (keys.hasNext()) {
                        final String key = (String) keys.next();
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
                //TODO: make rollup understand the first integer part
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

            final TreeSet<Long> treeSet = new TreeSet<>();
            treeSet.addAll(dataMap.keySet());

            if (allIntervals) {
                fillMissingIntervals(treeSet, rollup, toLong);
            }

            for (final Long millis : treeSet) {
                if (dataMap.containsKey(millis)) {
                    rolledUpTempReadings.add(parse(millis, function, dataMap.get(millis)));
                } else {
                    rolledUpTempReadings.add(new TempReading(millis, 0));
                }
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

    private void fillMissingIntervals(TreeSet<Long> treeSet, String rollup, long toLong) {

        //TODO: add non existing intervals
        if (rollup.endsWith("d")) {
            DateTime firstDate = new DateTime(treeSet.iterator().next());

            while (firstDate.isBefore(toLong)) {
                firstDate = firstDate.plusDays(1);
                if (!treeSet.contains(firstDate.getMillis())) {
                    treeSet.add(firstDate.getMillis());
                }
            }
        } else if (rollup.endsWith("h")) {
            DateTime firstDate = new DateTime(treeSet.iterator().next());

            while (firstDate.isBefore(toLong)) {
                firstDate = firstDate.plusHours(1);
                if (!treeSet.contains(firstDate.getMillis())) {
                    treeSet.add(firstDate.getMillis());
                }
            }
        } else if (rollup.endsWith("m")) {
            DateTime firstDate = new DateTime(treeSet.iterator().next());

            while (firstDate.isBefore(toLong)) {
                firstDate = firstDate.plusMinutes(1);
                if (!treeSet.contains(firstDate.getMillis())) {
                    treeSet.add(firstDate.getMillis());
                }
            }
        }
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
}
