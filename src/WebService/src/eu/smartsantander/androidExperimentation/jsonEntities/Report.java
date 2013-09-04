package eu.smartsantander.androidExperimentation.jsonEntities;

import java.util.List;

public class Report{

	private String jobName;
    private int  deviceId;
    private List<String> jobResults;
	
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
}
