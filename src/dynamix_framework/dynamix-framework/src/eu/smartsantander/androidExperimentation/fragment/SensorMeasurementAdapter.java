package eu.smartsantander.androidExperimentation.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.ambientdynamix.core.R;

import java.util.ArrayList;

public class SensorMeasurementAdapter extends ArrayAdapter<SensorMeasurement> {
    public SensorMeasurementAdapter(Context context, ArrayList<SensorMeasurement> sensorFragments) {
        super(context, 0, sensorFragments);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final SensorMeasurement sensorFragment = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.sensor_view, parent, false);
        }
        // Lookup view for data population
        final ImageView image = (ImageView) convertView.findViewById(R.id.sensor_image);
        final TextView text = (TextView) convertView.findViewById(R.id.sensor_value);
        //value
        text.setText(String.valueOf(sensorFragment.getValue().intValue()));

        //image
        final String type = sensorFragment.getType();
        if (type.contains("temperature")) {
            image.setImageResource(R.drawable.temperature);
        } else if (type.contains("humidity")) {
            image.setImageResource(R.drawable.humidity);
        } else if (type.contains("soundPressureLevel")) {
            image.setImageResource(R.drawable.sound);
        } else if (type.contains("atmosphericPressure")) {
            image.setImageResource(R.drawable.pressure);
        } else {
            image.setImageResource(R.drawable.humidity);
        }
        // Populate the data into the template view using the data object

        // Return the completed view to render on screen
        return convertView;
    }

}