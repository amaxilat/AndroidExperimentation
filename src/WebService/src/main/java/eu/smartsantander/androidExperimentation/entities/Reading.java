package eu.smartsantander.androidExperimentation.entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

//@Entity
public class Reading implements Serializable {
    public static enum Datatype {Integer, Float, String}

    ;

    private String context;
    private String value;
    private Datatype type;
    private long timestamp;

    public Reading() {
        context = "";
        value = "";
        type = Datatype.String;

    }

    public Reading(Datatype t, String val, String context) {
        this.type = t;
        this.value = val;
        this.context = context;
        this.timestamp = System.currentTimeMillis();
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Datatype getType() {
        return type;
    }

    public void setType(Datatype t) {
        this.type = t;
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static Reading fromJson(String json) {
        return (new Gson()).fromJson(json, Reading.class);
    }

    public static Reading[] arrayFromJson(String json) {
        Type listType = new TypeToken<ArrayList<Reading>>() {
        }.getType();
        List<Reading> readings = (new Gson()).fromJson(json, listType);
        if (readings.size() == 0) return new Reading[0];
        else return (Reading[]) readings.toArray();
    }
}
