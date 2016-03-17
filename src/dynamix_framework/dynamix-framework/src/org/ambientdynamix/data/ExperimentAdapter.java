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
package org.ambientdynamix.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ambientdynamix.core.DynamixService;
import org.ambientdynamix.core.R;
import org.ambientdynamix.util.EmptyListSupportAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import gr.cti.android.experimentation.model.Plugin;

/**
 * Local class used as a data-source for UpdateResults. This class extends a typed Generic ArrayAdapter and overrides
 * getView in order to update the UI state.
 *
 * @author Darren Carlson
 */
public class ExperimentAdapter extends EmptyListSupportAdapter<Experiment> {
    // Private data
    private final String TAG = this.getClass().getSimpleName();
    private final Map<Experiment, Integer> installables;
    private final LayoutInflater inflator;
    private final SimpleDateFormat sdf;

    /**
     * Creates a ContextPluginAdapter.
     *
     * @param context            The Android context.
     * @param textViewResourceId The text view resource id to manage.
     * @param inflator           The Android LayoutInflater.
     * @param updates            An ArrayList of UpdateResults.
     * @param installables       A Map of UpdateResults to their install completion percentage (e.g., 10 = 10%).
     * @param showAsUpdate       True if the adapter should be configured for updates; false to configure the adapter for
     *                           installations.
     * @param emptyTitle         The message to display in the title of an empty adapter.
     * @param emptyMessage       The message to display in the body of an empty adapter.
     */
    public ExperimentAdapter(Context context, int textViewResourceId, LayoutInflater inflator,
                             ArrayList<Experiment> updates, Map<Experiment, Integer> installables,
                             boolean showAsUpdate, String emptyTitle, String emptyMessage) {
        super(context, textViewResourceId, updates, emptyTitle, emptyMessage);
        this.installables = installables;
        this.inflator = inflator;
        sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    }

    /**
     * Returns the total number of UpdateResults.
     */
    public int getInstallableCount() {
        return this.installables.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (super.isListEmpty()) {
            final View v = inflator.inflate(R.layout.iconless_row, null);
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);
            tt.setText(getEmptyTitle());
            bt.setText(getEmptyMessage());
            return v;
        } else {
            final View v = inflator.inflate(R.layout.installable_experiment_row, null);
            try {
            /*
             * This issue is that the position is -1 here, which is not a valid index
			 */
                final Experiment experiment = getItem(position);
                if (experiment != null) {
//                if (DynamixService.getExperiment() != null
//                        && experiment.getId().equals(DynamixService.getExperiment().getId())
//                        && DynamixService.isExperimentInstalled(experiment.getContextType())) {
//                    //clicked the same experiment
//                    return null;
//                }

                    final Integer percentComplete = installables.get(experiment);

                    final TextView titleTextView = (TextView) v.findViewById(R.id.ex1_title);
                    final TextView pluginsTextView = (TextView) v.findViewById(R.id.ex1_plugins);
                    final TextView startDateTextView = (TextView) v.findViewById(R.id.ex1_start_date);
                    final ProgressBar progress = (ProgressBar) v.findViewById(R.id.progress);
                    final CheckedTextView checked = (CheckedTextView) v.findViewById(R.id.checkedTextView);
                    if (progress == null || checked == null) {
                        return v;
                    }
                    if (installables.containsKey(experiment) && installables.get(experiment) >= 0) {
                        progress.setVisibility(View.VISIBLE);
                        progress.setProgress(percentComplete);
                    } else {
                        progress.setVisibility(View.GONE);
                    }
                    checked.setChecked(installables.containsKey(experiment));
                    checked.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final CheckedTextView tv = (CheckedTextView) v;
                            if (!tv.isChecked()) {
                                installables.put(experiment, -1);
                                tv.setChecked(true);
                            } else {
                                installables.remove(experiment);
                                tv.setChecked(false);
                            }
                            notifyDataSetChanged();
                        }
                    });
                    if (titleTextView != null) {
                        titleTextView.setText(experiment.getName());
                    }
                    if (startDateTextView != null) {
                        // ex_start_date
                        final String formattedDate = "Added: " + sdf.format(new Date(experiment.getTimestamp()));
                        startDateTextView.setText(formattedDate);
                    }
                    if (pluginsTextView != null) {
                        // ex_plugins
                        final StringBuilder genreStr = new StringBuilder("Sensors: ");
                        final List<String> pluginNames = new ArrayList<>();
                        for (final String contextType : experiment.getSensorDependencies().split(",")) {
                            final Plugin plugin = DynamixService.getDiscoveredPluginByContextType(contextType);
                            if (plugin != null) {
                                pluginNames.add(plugin.getName());
                            }
                        }
                        genreStr.append(android.text.TextUtils.join(", ", pluginNames));
                        pluginsTextView.setText(genreStr.toString());
                    }
                }
            } catch (IndexOutOfBoundsException e) {

            }
            return v;
        }
    }
}