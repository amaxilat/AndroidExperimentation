package eu.smartsantander.androidExperimentation.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Smartphone {
    @Id
    @GeneratedValue
    private Integer id;
    private String deviceType;

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        this.phoneId = this.id;
    }

    private Integer phoneId;

    public Integer getPhoneId() {
        return phoneId;
    }

    public void setPhoneId(Integer phoneId) {
        this.phoneId = phoneId;
    }

    private String sensorsRules;

    public String getSensorsRules() {
        return sensorsRules;
    }

    public void setSensorsRules(String sensorsRules) {
        this.sensorsRules = sensorsRules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Smartphone that = (Smartphone) o;

        if (id != that.id) return false;
        if (phoneId != that.phoneId) return false;
        if (sensorsRules != null ? !sensorsRules.equals(that.sensorsRules) : that.sensorsRules != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + phoneId;
        result = 31 * result + (sensorsRules != null ? sensorsRules.hashCode() : 0);
        return result;
    }
}
