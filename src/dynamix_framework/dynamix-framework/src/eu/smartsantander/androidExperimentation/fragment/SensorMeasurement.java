package eu.smartsantander.androidExperimentation.fragment;

import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;

/**
 * A measurement from a sensor.
 */
public class SensorMeasurement {

    String type;
    Double value;
    ValueLineSeries series;

    public SensorMeasurement(String type, Double value) {
        this.type = type;
        this.value = value;
        this.series = new ValueLineSeries();
        this.series.setWidthOffset(50);
        this.series.setColor(0xFFEF4270);
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

    public void add(Double doubleVal) {
        this.series.addPoint(new ValueLinePoint(doubleVal.floatValue()));
        this.value = doubleVal;
    }

    public ValueLineSeries getSeries() {
        return series;
    }
}
