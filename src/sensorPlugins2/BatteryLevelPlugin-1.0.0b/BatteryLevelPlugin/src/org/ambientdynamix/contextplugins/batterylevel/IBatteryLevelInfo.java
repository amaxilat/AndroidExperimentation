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
package org.ambientdynamix.contextplugins.batterylevel;

import org.ambientdynamix.api.application.IContextInfo;

/**
 * Represents battery status information
 * 
 * @author Darren Carlson
 * 
 */
public interface IBatteryLevelInfo extends IContextInfo {

	/**
	 * Integer field containing the current battery level, from 0 to Scale (obtained from getScale()).
	 * 
	 * @return battery level.
	 */
	public int getBatteryLevel();

	/**
	 * Integer containing the current battery temperature.
	 * 
	 * @return The current battery temperature.
	 */
	public int getTemperature();

	/**
	 * Integer containing the current battery voltage level.
	 * 
	 * @return The current battery voltage level.
	 */
	public int getVoltage();

	/**
	 * Integer containing the maximum battery level.
	 * 
	 * @return The maximum battery level.
	 */
	public int getScale();

	/**
	 * Boolean indicating whether a battery is present.
	 * 
	 * @return Whether a battery is present.
	 */
	public boolean isPresent();

	/**
	 * Integer indicating whether the device is plugged in to a power source; 0 means it is on battery, other constants
	 * are different types of power sources.
	 * 
	 * @return Plugged status.
	 */
	public int getPlugged();

	/**
	 * String describing the technology of the current battery.
	 * 
	 * @return The technology of the current battery.
	 */
	public String getTechnology();

	/**
	 * Integer containing the current status constant.
	 * 
	 * @return The current status constant.
	 */
	public int getStatus();

	/**
	 * Integer containing the current health constant. 
	 * @return The current health constant.
	 */
	public int getHealth();
}