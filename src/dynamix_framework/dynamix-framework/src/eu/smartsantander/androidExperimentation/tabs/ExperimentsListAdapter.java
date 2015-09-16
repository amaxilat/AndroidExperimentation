package eu.smartsantander.androidExperimentation.tabs;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.ambientdynamix.core.R;

import java.util.ArrayList;
import java.util.List;

import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;

/**
 * Created by amaxilatis on 9/9/15.
 */
public class ExperimentsListAdapter extends ArrayAdapter<Experiment> {


    private Activity activity;
    private List<Experiment> experiements;


    private static LayoutInflater inflater = null;

    public ExperimentsListAdapter(Activity activity, int textViewResourceId, ArrayList<Experiment> experiements) {
        super(activity, textViewResourceId);
        try {
            this.activity = activity;
            this.experiements = experiements;
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        } catch (Exception e) {

        }
    }

    public int getCount() {
        return experiements.size();
    }

    public Experiment getItem(Experiment position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public static class ViewHolder {
        public TextView display_name;
        public TextView display_number;

    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        final ViewHolder holder;
        try {
            if (convertView == null) {
                vi = inflater.inflate(R.layout.experiment_row, null);
                holder = new ViewHolder();

                holder.display_name = (TextView) vi.findViewById(R.id.display_name);
                holder.display_number = (TextView) vi.findViewById(R.id.display_number);


                vi.setTag(holder);
            } else {
                holder = (ViewHolder) vi.getTag();
            }


            holder.display_name.setText(experiements.get(position).getName());
            holder.display_number.setText(experiements.get(position).getId());


        } catch (Exception e) {


        }
        return vi;
    }
}
