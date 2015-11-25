package eu.smartsantander.androidExperimentation.util;

/**
 * Data from GCM.
 */
public class GcmMessageData {
    private String type;
    private Integer count;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "GcmMessageData{" +
                "type='" + type + '\'' +
                ", count=" + count +
                '}';
    }
}
