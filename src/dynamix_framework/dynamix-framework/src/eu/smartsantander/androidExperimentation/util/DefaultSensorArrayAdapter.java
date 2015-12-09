package eu.smartsantander.androidExperimentation.util;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import org.ambientdynamix.core.R;

import java.util.List;

/**
 * Created by amaxilatis on 8/12/2015.
 */
public class DefaultSensorArrayAdapter extends ArrayAdapter<DefaultSensor> {
    Context context;
    int layoutResourceId;
    List<DefaultSensor> data = null;

    public DefaultSensorArrayAdapter(Context context, int layoutResourceId, List<DefaultSensor> data) {
        super(context, layoutResourceId, data.toArray(new DefaultSensor[data.size()]));
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SensorHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new SensorHolder();
            holder.txName = (TextView) row.findViewById(R.id.sensor_name);
            holder.txDescription = (TextView) row.findViewById(R.id.sensor_description);
            holder.swStatus = (Switch) row.findViewById(R.id.sensor_switch);

            row.setTag(holder);
        } else {
            holder = (SensorHolder) row.getTag();
        }

        DefaultSensor sensor = data.get(position);
        holder.txName.setText(sensor.getName());
        holder.txDescription.setText(sensor.getDescription());
        holder.swStatus.setChecked(sensor.isState());
        holder.swStatus.setOnCheckedChangeListener(sensor.getListener());

        return row;
    }

    static class SensorHolder {
        TextView txName;
        TextView txDescription;
        Switch swStatus;
    }
}
