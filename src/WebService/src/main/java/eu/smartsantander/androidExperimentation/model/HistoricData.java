package eu.smartsantander.androidExperimentation.model;

import java.util.List;

/**
 * Created by amaxilatis on 28/10/2015.
 */
public class HistoricData {

    private int entiry_id;
    private String  attribute_id;
    private String function;
    private String from;
    private String to;
    private List<List<Object>> readings;

    public int getEntiry_id() {
        return entiry_id;
    }

    public void setEntiry_id(int entiry_id) {
        this.entiry_id = entiry_id;
    }

    public String getAttribute_id() {
        return attribute_id;
    }

    public void setAttribute_id(String attribute_id) {
        this.attribute_id = attribute_id;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<List<Object>> getReadings() {
        return readings;
    }

    public void setReadings(List<List<Object>> readings) {
        this.readings = readings;
    }
}
