package eu.smartsantander.androidExperimentation.operations;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.IdResult;
import org.ambientdynamix.core.DynamixService;

import java.util.List;

import eu.smartsantander.androidExperimentation.util.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;

public class AsyncExpTask extends AsyncTask<String, Void, String> {
    private final String TAG = this.getClass().getSimpleName();
    private boolean stateActive = false;


    public AsyncExpTask() {
    }

    public boolean isStateActive() {
        return stateActive;
    }

    @Override
    protected String doInBackground(String... params) {
        this.stateActive = true;
        if (!DynamixService.isEnabled()) {
            return "AndroidExperimentation Async Experiment Task Executed Dynamix Disabled";
        }
        Log.d(TAG, "doInBackground " + DynamixService.sessionStarted);
        try {
            if (!DynamixService.sessionStarted) {
                DynamixServiceListenerUtility.start();
            } else {

                if (DynamixService.getExperiment() != null) {
                    try {
                        final List<ContextPluginInformation> information = DynamixService.dynamix.getAllContextPluginInformation().getContextPluginInformation();
                        for (final ContextPluginInformation contextPluginInformation : information) {
                            if (contextPluginInformation.isEnabled()) {
                                Log.d(TAG, contextPluginInformation.getPluginId() + " [" + contextPluginInformation.getInstallStatus() + "]");
                                DynamixService.dynamix.contextRequest(DynamixService.dynamixCallback, contextPluginInformation.getPluginId(), contextPluginInformation.getPluginId());
                            }
                        }


                        if (!DynamixService.isExperimentInstalled(Constants.EXPERIMENT_PLUGIN_CONTEXT_TYPE)) {
                            DynamixService.startExperiment();
                        }
                        // ping experiment....
                        Log.i(TAG, "Ping Experiment");
                        DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback,
                                Constants.EXPERIMENT_PLUGIN_CONTEXT_TYPE,
                                Constants.EXPERIMENT_PLUGIN_CONTEXT_TYPE,
                                DynamixService.getReadingStorage().getBundle());
                        try {
                            manageExperiment();
                        } catch (Exception e) {
                            return e.getMessage();
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        this.stateActive = false;
        return "AndroidExperimentation Async Experiment Task Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        this.stateActive = false;
    }

    @Override
    protected void onPreExecute() {
        this.stateActive = true;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        this.stateActive = true;
    }

    @Override
    protected void onCancelled() {
        this.stateActive = false;
    }

    public String manageExperiment() throws Exception {
//        if (DynamixService.getExperiment() != null) {
//            if (DynamixService.getExperiment().getToTime() != null
//                    && DynamixService.getExperiment().getToTime() < System
//                    .currentTimeMillis()) {
//                // DynamixService.removeExperiment();
//            }
//        }

        List<Experiment> experimentList = null;

        try {
//            experimentList = DynamixService.getCommunication().getExperiments();
            experimentList = DynamixService.getCommunication().getExperimentsById(
                    String.valueOf(DynamixService.getPhoneProfiler().getPhoneId()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Failed to fetch Experiment Info");
        }
        if (experimentList.isEmpty()) {
            Log.w(TAG, "No experiment Fetched");
            DynamixService.removeExperiment();
            throw new Exception("No experiment Fetched");
        } else {
            return "";
        }
    }
}
