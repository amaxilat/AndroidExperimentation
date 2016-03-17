package eu.smartsantander.androidExperimentation.jsonEntities;


import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class Badge implements Serializable {
    private int id;

    private long timestamp;

    private int experimentId;

    public int getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    private int deviceId;

    private String message;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Badge badge = (Badge) o;

        if (id != badge.id) return false;
        if (timestamp != badge.timestamp) return false;
        if (experimentId != badge.experimentId) return false;
        if (deviceId != badge.deviceId) return false;
        return message.equals(badge.message);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + experimentId;
        result = 31 * result + deviceId;
        result = 31 * result + message.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Badge{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", experimentId=" + experimentId +
                ", deviceId=" + deviceId +
                ", message='" + message + '\'' +
                '}';
    }
}
