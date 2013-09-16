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

import java.util.HashSet;
import java.util.Set;

import android.content.Intent;
import android.os.BatteryManager;
import android.os.Parcel;
import android.os.Parcelable;

class BatteryLevelInfo implements IBatteryLevelInfo {
	/**
	 * Required CREATOR field that generates instances of this Parcelable class from a Parcel.
	 * 
	 * @see http://developer.android.com/reference/android/os/Parcelable.Creator.html
	 */
	public static Parcelable.Creator<BatteryLevelInfo> CREATOR = new Parcelable.Creator<BatteryLevelInfo>() {
		/**
		 * Create a new instance of the Parcelable class, instantiating it from the given Parcel whose data had
		 * previously been written by Parcelable.writeToParcel().
		 */
		public BatteryLevelInfo createFromParcel(Parcel in) {
			return new BatteryLevelInfo(in);
		}

		/**
		 * Create a new array of the Parcelable class.
		 */
		public BatteryLevelInfo[] newArray(int size) {
			return new BatteryLevelInfo[size];
		}
	};
	// Public static variable for our supported context type
	public static String CONTEXT_TYPE = "org.ambientdynamix.contextplugins.batterylevel";
	// Private data
	private int batteryLevel;
	private int temperature;
	private int voltage;
	private int scale;
	private boolean present;
	private int plugged;
	private String technology;
	private int status;
	private int health;

	/**
	 * Returns the type of the context information represented by the IContextInfo. This string must match one of the
	 * supported context information type strings described by the source ContextPlugin.
	 */
	@Override
	public String getContextType() {
		return "org.ambientdynamix.contextplugins.batterylevel";
	}

	/**
	 * Returns the fully qualified class-name of the class implementing the IContextInfo interface. This allows Dynamix
	 * applications to dynamically cast IContextInfo objects to their original type using reflection. A Java
	 * "instanceof" compare can also be used for this purpose.
	 */
	@Override
	public String getImplementingClassname() {
		return this.getClass().getName();
	}

	/**
	 * Returns a Set of supported string-based context representation format types or null if no representation formats
	 * are supported. Examples formats could include MIME, Dublin Core, RDF, etc. See the plug-in documentation for
	 * supported representation types.
	 */
	@Override
	public Set<String> getStringRepresentationFormats() {
		Set<String> formats = new HashSet<String>();
		formats.add("text/plain");
		return formats;
	}

	/**
	 * Returns a string-based representation of the IContextInfo for the specified format string (e.g.
	 * "application/json") or null if the requested format is not supported.
	 */
	@Override
	public String getStringRepresentation(String format) {
		if (format.equalsIgnoreCase("text/plain"))
			return "Battery level: " + batteryLevel;
		else
			// Format not supported, so return an empty string
			return "";
	}

	/**
	 * Create a BatteryLevelInfo
	 * 
	 * @param batteryLevel
	 *            The device's detected battery level as a percentage of 100.
	 */
	public BatteryLevelInfo(Intent intent) {
		present = intent.getExtras().getBoolean(BatteryManager.EXTRA_PRESENT);
		batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
		voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
		plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		technology = intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
		temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getBatteryLevel() {
		return batteryLevel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getTemperature() {
		return temperature;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getVoltage() {
		return voltage;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getScale() {
		return scale;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPresent() {
		return present;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPlugged() {
		return plugged;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTechnology() {
		return technology;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getStatus() {
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getHealth() {
		return health;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	};

	/**
	 * Used by Parcelable when sending (serializing) data over IPC.
	 */
	public void writeToParcel(Parcel out, int flags) {
		out.writeByte((byte) (present ? 1 : 0));
		out.writeInt(batteryLevel);
		out.writeInt(scale);
		out.writeInt(health);
		out.writeInt(voltage);
		out.writeInt(plugged);
		out.writeInt(status);
		out.writeInt(temperature);
		out.writeString(technology);
	}

	/**
	 * Used by the Parcelable.Creator when reconstructing (deserializing) data sent over IPC.
	 */
	private BatteryLevelInfo(final Parcel in) {
		present = in.readByte() == 1;
		batteryLevel = in.readInt();
		scale = in.readInt();
		health = in.readInt();
		voltage = in.readInt();
		plugged = in.readInt();
		status = in.readInt();
		temperature = in.readInt();
		technology = in.readString();
	}

	/**
	 * Default implementation that returns 0.
	 * 
	 * @return 0
	 */
	@Override
	public int describeContents() {
		return 0;
	}
}