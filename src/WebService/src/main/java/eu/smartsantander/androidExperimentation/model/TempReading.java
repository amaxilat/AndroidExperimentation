package eu.smartsantander.androidExperimentation.model;

/**
 * Created by amaxilatis on 10/11/2015.
 */
public class TempReading {
    private long timestamp;
    private double value;

    public TempReading() {
    }

    public TempReading(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
