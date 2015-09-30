package eu.smartsantander.androidExperimentation.operations;

import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.ContextPluginInformationResult;
import org.ambientdynamix.api.application.IdResult;
import org.ambientdynamix.core.BaseActivity;
import org.ambientdynamix.core.DynamixService;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class AsyncExperimentTask extends AsyncTask<String, Void, String> {
    private final String TAG = this.getClass().getSimpleName();
    private boolean stateActive = false;

    private NotificationHQManager noteManager;

    public AsyncExperimentTask() {
    }

    public boolean isStateActive() {
        return stateActive;
    }

    @Override
    protected String doInBackground(String... params) {
        this.stateActive = true;
        Log.i("AsyncExperimentTask", "Experiment Connecting...");
        if (!DynamixService.isEnabled()) {
            return "AndroidExperimentation Async Experiment Task Executed Dynamix Disabled";
        }

        if (!DynamixService.sessionStarted) {
            DynamixServiceListenerUtility.start();
        } else {
            try {

                List<ContextPluginInformation> information = DynamixService.dynamix.getAllContextPluginInformation().getContextPluginInformation();
                for (ContextPluginInformation contextPluginInformation : information) {
                    Log.i(TAG, contextPluginInformation.getPluginName() + " status:" + contextPluginInformation.getInstallStatus() + " id:" + contextPluginInformation.getPluginId() + " context:" + contextPluginInformation.getContextPluginType().name());
                    if (contextPluginInformation.isEnabled()) {
                        DynamixService.dynamix.contextRequest(DynamixService.dynamixCallback, contextPluginInformation.getPluginId(), contextPluginInformation.getPluginId());
                    }
                }

                if (DynamixService.getExperiment() != null) {

                    if (!DynamixService.isExperimentInstalled("org.ambientdynamix.contextplugins.ExperimentPlugin")) {
                        DynamixService.startExperiment();
                    }

                }
                // ping experiment....
                final IdResult r = DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback, "org.ambientdynamix.contextplugins.ExperimentPlugin", "org.ambientdynamix.contextplugins.ExperimentPlugin", DynamixService.getReadingStorage().getBundle());
                Log.i("contextRequest", r.getMessage());

                try {
                    manageExperiment();
                } catch (Exception e) {
                    this.stateActive = false;
                    return e.getMessage();
                }

                this.stateActive = false;
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.stateActive = false;
        return "AndroidExperimentation Async Experiment Task Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.i("AndroidExperimentation",
                "AndroidExperimentation Async Experiment Task Post Execute:"
                        + result);
        this.stateActive = false;
    }

    @Override
    protected void onPreExecute() {
        Log.i("AndroidExperimentation",
                "AndroidExperimentation Async Experiment Task pre execute");
        this.stateActive = true;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        Log.i("AndroidExperimentation",
                "AndroidExperimentation Async Experiment Task  update progress");
        this.stateActive = true;
    }

    @Override
    protected void onCancelled() {
        Log.i("AndroidExperimentation",
                "AndroidExperimentation Async Experiment Task cancelled");
        this.stateActive = false;
    }

    public String manageExperiment() throws Exception {
        Log.d(TAG, "manageExperiment");

        if (DynamixService.getExperiment() != null) {
            if (DynamixService.getExperiment().getToTime() != null
                    && DynamixService.getExperiment().getToTime() < System
                    .currentTimeMillis()) {
                // DynamixService.removeExperiment();
            }
        }

        List<Experiment> experimentList = null;

        noteManager = NotificationHQManager.getInstance();

        try {
            experimentList = DynamixService.getCommunication().getExperiments();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Failed to fetch Experiment Info");
        }
        Log.i(TAG, String.valueOf(experimentList));
        if (experimentList.isEmpty()) {
            Log.i(TAG, "No experiment Fetched");
            DynamixService.removeExperiment();
            throw new Exception("No experiment Fetched");
        } else {
            try {
                Experiment experiment = experimentList.get(0);
                Log.d(TAG, "Experiment:" + experiment.getId());
                String[] smarDeps = DynamixService.getPhoneProfiler()
                        .getSensorRules().split(",");
                String[] expDeps = experiment.getSensorDependencies()
                        .split(",");
                if (Constants.match(smarDeps, expDeps)) {
                    int oldExpId = -1;
                    if (DynamixService.getExperiment() != null) {
                        oldExpId = DynamixService.getExperiment().getId();
                    }
                    if (experiment.getId() == oldExpId
                            && DynamixService.isExperimentInstalled(experiment.getContextType())) {
                        DynamixService                                .addTotalTimeConnectedOnline(Constants.EXPERIMENT_POLL_INTERVAL);
                        Log.i(TAG, "Experiment still the same");
                        throw new Exception("Experiment still the same");
                    }
                    String url = experiment.getUrl();
                    Downloader downloader = new Downloader();
                    try {
                        downloader.DownloadFromUrl(url,
                                experiment.getFilename());

                        IdResult r = DynamixService.dynamix.configuredContextRequest(DynamixService.dynamixCallback, "org.ambientdynamix.contextplugins.ExperimentPlugin", "org.ambientdynamix.contextplugins.ExperimentPlugin",
                                DynamixService.getReadingStorage()
                                        .getBundle());
                        DynamixService.removeExperiment();
                        DynamixService.setExperiment(experiment);
                        Thread.sleep(5000);
                        DynamixService.startExperiment();
                        DynamixService.stopFramework();
                        DynamixService.setRestarting(true);
                        DynamixService.setTitleBarRestarting(true);
                        Thread.sleep(5000);
                        DynamixService.startFramework();
                        Thread.sleep(7000);
                        DynamixServiceListenerUtility.start();
                        DynamixService.setRestarting(false);
                        DynamixService.setTitleBarRestarting(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (!DynamixService.isNetworkAvailable()) {
                            // Toast.makeText(DynamixService.getAndroidContext(),
                            // "Please Check Internet Connection!",
                            // 10000).show();
                            noteManager.postNotification("Please Check Internet Connection!");
                        } else {
                            // Toast.makeText(DynamixService.getAndroidContext(),
                            // "Please Check Internet Connection!",
                            // 10000).show();
                            noteManager
                                    .postNotification("Please Check Internet Connection!");
                        }
                        throw new Exception("Failed to Download Experiment");
                    }

                    // Toast.makeText(DynamixService.getAndroidContext(),
                    // "Experiment Pushed", 8000).show();
                    noteManager.postNotification("Experiment Pushed");
                    return "Experiment Commited";
                } else {
                    // Log.i(TAG, "Experiment violates Sensor Rules");
                    throw new Exception("Experiment violates Sensor Rules");
                }
            } catch (Exception e) {
                // Log.i(TAG, "Exception in consuming experiment" +
                // e.getMessage());
                throw new Exception("Exception in consuming experiment:" + e.getMessage());
            }
        }

    }
}
