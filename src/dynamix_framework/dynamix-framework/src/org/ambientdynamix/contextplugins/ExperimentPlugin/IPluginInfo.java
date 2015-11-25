package org.ambientdynamix.contextplugins.ExperimentPlugin;

import java.util.List;
import java.util.Set;

import eu.smartsantander.androidExperimentation.jsonEntities.Reading;
public interface IPluginInfo {
	String getStringRepresentation(String format);

	String getImplementingClassname();

	String getContextType();

	Set<String> getStringRepresentationFormats();
	
	String getState();

	void setState(String state);
	
	String getPayload();
	
	void setPayload(List<Reading> payload);
}