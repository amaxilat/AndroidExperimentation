package eu.smartsantander.androidExperimentation.jsonEntities;

import eu.smartsantander.androidExperimentation.entities.Plugin;

import java.util.ArrayList;
import java.util.List;

public class PluginList
{
	private List<Plugin> plugList;
	
	public PluginList()
	{
		plugList = new ArrayList<Plugin>();
	}
	
	public void setPluginList(List<Plugin> plugList)
	{
		this.plugList = plugList;
	}
	
	public List<Plugin> getPluginList()
	{
		return this.plugList;
	}
}
