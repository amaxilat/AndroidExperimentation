package eu.smartsantander.androidExperimentation.jsonEntities;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class Smartphone {



    private int id;

    public Smartphone( ){

	}
    public Smartphone(int id){
		this.id=id;
	}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int phoneId;

    public int getPhoneId() {
        return phoneId;
    }

    public void setPhoneId(int phoneId) {
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
        return !(sensorsRules != null ? !sensorsRules.equals(that.sensorsRules) : that.sensorsRules != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + phoneId;
        result = 31 * result + (sensorsRules != null ? sensorsRules.hashCode() : 0);
        return result;
    }
}
