/*
 * Copyright (C) The Ambient Dynamix Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambientdynamix.update.contextplugin;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.ambientdynamix.api.application.VersionInfo;
import org.ambientdynamix.api.contextplugin.PluginConstants.PLATFORM;
import org.ambientdynamix.util.RepositoryInfo;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;

import android.util.Log;

public class SimpleSourceBase {
	private final String TAG = this.getClass().getSimpleName();
	private final String URLS = Constants.URL;

	protected List<DiscoveredContextPlugin> createDiscoveredPlugins(RepositoryInfo repo, InputStream input, PLATFORM platform, VersionInfo platformVersion, VersionInfo frameworkVersion,boolean processSingle) throws Exception {

		// SmartSantander Modifications
		Log.i("AndroidExperimentation", "Start Plugin Discovery");
		String jsonPluginList = "";
		List<DiscoveredContextPlugin> plugs = new ArrayList<DiscoveredContextPlugin>();
		try {
			jsonPluginList = getPluginList();
			Log.i(TAG, jsonPluginList);
			if (jsonPluginList.equals("0")) {
				Log.i(TAG, "No plugin List");
			} else {
				PluginList pluginList = (new Gson()).fromJson(jsonPluginList,PluginList.class);
				Log.i(TAG, "Plugin List setted");
				List<Plugin> plugList = pluginList.getPluginList();
				for (Plugin plugInfo : plugList) {
					ContextPluginBinder plugBinder = new ContextPluginBinder();
						DiscoveredContextPlugin plug = plugBinder.createDiscoveredPlugin(repo, plugInfo);
						plugs.add(plug);		
				}
				return plugs;
			}
		} catch (Exception e) {
			Log.w(TAG, "Exception Installin plugins: "+ e.getMessage());
			return plugs;
		}
		return plugs;
	}

	
	private String getPluginList() throws Exception {
		return sendGetPluginList();
	}

	private String sendGetPluginList() throws Exception {
		final String NAMESPACE = "http://androidExperimentation.smartsantander.eu/";
		final String URL = Constants.URL+":8080/services/AndroidExperimentationWS?wsdl";
		final String METHOD_NAME = "getPluginList";
		final String SOAP_ACTION = "\"" + "http://AndroidExperimentationWS/getPluginList"+ "\"";
		String test = "0";
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
		PropertyInfo propInfo = new PropertyInfo();
		propInfo.name = "arg0";
		propInfo.type = PropertyInfo.STRING_CLASS;
		propInfo.setValue("");
		request.addProperty(propInfo);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
		try {
			androidHttpTransport.call(SOAP_ACTION, envelope);
			SoapPrimitive resultsRequestSOAP = (SoapPrimitive) envelope.getResponse();
			test = resultsRequestSOAP.toString();
		} catch (Exception e) {
			throw e;
		}
		return test;
	}
}
