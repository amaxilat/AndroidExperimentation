package eu.smartsantander.androidExperimentation.entities;

import java.util.List;

import com.google.gson.Gson;

public class Report{

    private String jobName;
    private int  deviceId;
    private List<String> jobResults;

    public Report()
    {
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Report(String jobName)
    {
        this.jobName = jobName;
    }

    public void setResults(List<String> jobResults)
    {
        this.jobResults = jobResults;
    }

    public List<String> getResults()
    {
        return this.jobResults;
    }

    public String getName()
    {
        return jobName;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String toJson(){
        return (new Gson()).toJson(this);
    }

    public static Report fromJson(String json){
        return (new Gson()).fromJson(json, Report.class);
    }
}
