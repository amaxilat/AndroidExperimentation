package eu.smartsantander.androidExperimentation.fragment;

/**
 * A measurement from a sensor.
 */
public class SensorMeasurement {

    String type;
    Double value;

    public SensorMeasurement(String type, Double value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
