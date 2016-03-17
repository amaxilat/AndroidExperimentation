package eu.smartsantander.androidExperimentation.jsonEntities;

import java.util.Map;
import java.util.Set;

/**
 * Created by amaxilatis on 12/3/2016.
 */
public class SmartphoneStatistics {
    private int id;
    private long readings;
    private long experimentReadings;
    private int experiments;
    private Map<Long, Long> last7Days;
    private String sensorRules;
    private Set<RankingEntry> rankings;
    private Set<RankingEntry> experimentRankings;
    private Set<Badge> badges;
    private Set<Badge> experimentBadges;

    public SmartphoneStatistics() {
    }

    public SmartphoneStatistics(final int id) {
        this.id = id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setReadings(final long readings) {
        this.readings = readings;
    }

    public long getReadings() {
        return readings;
    }

    public void setExperimentReadings(final long experimentReadings) {
        this.experimentReadings = experimentReadings;
    }

    public long getExperimentReadings() {
        return experimentReadings;
    }

    public void setExperiments(final int experiments) {
        this.experiments = experiments;
    }

    public int getExperiments() {
        return experiments;
    }

    public void setLast7Days(final Map<Long, Long> last7Days) {
        this.last7Days = last7Days;
    }

    public Map<Long, Long> getLast7Days() {
        return last7Days;
    }

    public void setSensorRules(final String sensorRules) {
        this.sensorRules = sensorRules;
    }

    public String getSensorRules() {
        return sensorRules;
    }

    public void setRankings(Set<RankingEntry> rankings) {
        this.rankings = rankings;
    }

    public Set<RankingEntry> getRankings() {
        return rankings;
    }

    public void setBadges(Set<Badge> badges) {
        this.badges = badges;
    }

    public Set<Badge> getBadges() {
        return badges;
    }

    public void setExperimentRankings(Set<RankingEntry> experimentRankings) {
        this.experimentRankings = experimentRankings;
    }

    public Set<RankingEntry> getExperimentRankings() {
        return experimentRankings;
    }

    public void setExperimentBadges(Set<Badge> experimentBadges) {
        this.experimentBadges = experimentBadges;
    }

    public Set<Badge> getExperimentBadges() {
        return experimentBadges;
    }
}
