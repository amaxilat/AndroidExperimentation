package eu.smartsantander.androidExperimentation.operations;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import eu.smartsantander.androidExperimentation.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Plugin;
import eu.smartsantander.androidExperimentation.jsonEntities.PluginList;

public class Demon extends Thread implements Runnable {

	private Handler handler;
	private Scheduler scheduler;
	private PhoneProfiler phoneProfiler;
	private Context context;
	private Communication communication;
	private SensorProfiler sensorProfiler;
	private AsyncExperimentTask pingExp = new AsyncExperimentTask();
	private SharedPreferences pref;
	private Editor editor;
	private boolean isDeviceRegistered;
	private boolean isProperlyInitiallized;

	String runningJob = "-1";
	String lastRunned = "-1";

	// get TAG name for reporting to LogCat
	private final String TAG = this.getClass().getSimpleName();

	public Demon(Handler handler, Context context, Communication communication,
			Scheduler scheduler, PhoneProfiler phoneProfiler,
			SensorProfiler sensorProfiler) {
		this.context = context;
		this.handler = handler;
		this.scheduler = scheduler;
		this.phoneProfiler = phoneProfiler;
		this.communication = communication;
		this.sensorProfiler = sensorProfiler;
		pref = context.getApplicationContext().getSharedPreferences("runningJob", 0); // 0 - for private mode
		editor = pref.edit();
		runningJob = pref.getString("runningJob", "-1");
		lastRunned = pref.getString("lastExperiment", "-1");
		// registration of device if needed
		if (phoneProfiler.getPhoneId() == Constants.PHONE_ID_UNITIALIZED) {
			this.isDeviceRegistered = false;
		} else {
			this.isDeviceRegistered = true;
		}
	}

	public void run() {
		try {
			Log.d(TAG, "AndroidExperimentation Running");
			if (isDeviceRegistered == false) {
				Log.d(TAG, "AndroidExperimentation Running Unregistered Device");
				return;
			}
			Thread.sleep(1000);
			/*File root = android.os.Environment.getExternalStorageDirectory();
			File dir = new File(root.getAbsolutePath() + "/dynamix");
			if (dir.exists() == false) {
				dir.mkdirs();
			}

			List<Plugin> pluginList = communication.sendGetPluginList();
			Plugin pluginXML = null;
			for (Plugin plug : pluginList) {
				Constants.checkFile(plug.getFilename(), plug.getInstallUrl());
				if (plug.getName().equals("plugs.xml")) {
					pluginXML = plug;
				}
			}
			pluginList.remove(pluginXML);
			PluginList plist = new PluginList();
			plist.setPluginList(pluginList);
			String plistString = (new Gson()).toJson(plist, PluginList.class);
			editor = (this.context.getSharedPreferences("pluginObjects", 0)).edit();
			editor.putString("pluginObjects", plistString);
			editor.commit();
			*/
			this.isProperlyInitiallized = true;
			handler.postDelayed(runnable, 1000);
		} catch (Exception e) {
			this.isProperlyInitiallized = false;
			e.printStackTrace();
			Log.d(TAG, "AndroidExperimentation:" + e.getMessage());
		}
	}

	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (isProperlyInitiallized = false)
				return;
			pingExp.cancel(true);
			pingExp = new AsyncExperimentTask();
			pingExp.execute();
			handler.postDelayed(this, Constants.EXPERIMENT_POLL_INTERVAL);
		}
	};

	

	private void updateDynamixRepository() {
		Log.i(TAG, "send update dynamix repository intent");
		Intent i = new Intent();
		i.setAction("org.ambiendynamix.core.DynamixService");
		context.sendBroadcast(i);
	}

	public void sendThreadMessage(String message) {
		Message msg = new Message();
		msg.obj = message;
		handler.sendMessage(msg);
	}

	public class AsyncExperimentTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			runningJob = pref.getString("runningJob", "-1");
			lastRunned = pref.getString("lastExperiment", "-1");
			Log.i("AsyncExperimentTask", runningJob);

			if (runningJob.equals("-1")) {

				// if registered ask for experiment
				if (phoneProfiler.getPhoneId() != Constants.PHONE_ID_UNITIALIZED) {
					String jsonExperiment = "0";
					try {
						jsonExperiment = communication.getExperiment(
								phoneProfiler.getPhoneId(),
								sensorProfiler.getSensorRules());
					} catch (Exception e) {
						// TODO handle this
						e.printStackTrace();
						return "No experiment Fetched";
					}

					Log.i(TAG, jsonExperiment);
					if (jsonExperiment.equals("0")) {
						Log.i(TAG, "No experiment Fetched");
						return "No experiment Fetched";
					} else {
						try {
							Gson gson = new Gson();

							Experiment experiment = (Experiment) gson.fromJson(jsonExperiment, Experiment.class);
							String[] smarDeps = sensorProfiler.getSensorRules().split(",");
							String[] expDeps = experiment.getSensorDependencies().split(",");


							if (match(smarDeps,expDeps)==true) {
								String contextType = experiment.getContextType();
								String url = experiment.getUrl();

								Downloader downloader = new Downloader();
								try {
									downloader.DownloadFromUrl(url, experiment.getFilename());
								} catch (Exception e) {
									e.printStackTrace();
									return "Failed to Download Experiment";
								}

								editor.putString("runningJob", contextType);
								editor.putString("runningExperimentUrl",experiment.getUrl());
								editor.commit();
								sendThreadMessage("job_name:"+ experiment.getName());

								// tell to dynamix Framework to update its
								// repository
								updateDynamixRepository();
								scheduler.commitJob(contextType);
								return "Experiment Commited";
							} else {
								Log.i(TAG, "Experiment violates Sensor Rules");
								return "Experiment violates Sensor Rules";
							}
						} catch (Exception e) {
							e.printStackTrace();
							Log.i(TAG, "Exception in consuming experiment" +e.getMessage());
							return "Exception in consuming experiment";
						}
					}
				} else {
					Log.i("AndroidExperimentation", "Ping failed");
					return "Ping failed";
				}
			} else {
				if (scheduler.currentJob.jobState == null) {
					String runningExperimentUrl = pref.getString(
							"runningExperimentUrl", "-1");
					try {
						Constants.checkExperiment(runningJob, runningExperimentUrl);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sendThreadMessage("job_name:" + runningJob);
					scheduler.commitJob(runningJob);
					return "Experiment Commited";
				}

			}
			return "AndroidExperimentation Async Experiment Task Executed";
		}

		@Override
		protected void onPostExecute(String result) {
			Log.i("AndroidExperimentation",
					"AndroidExperimentation Async Experiment Task Post Execute:"
							+ result);
		}

		@Override
		protected void onPreExecute() {
			Log.i("AndroidExperimentation",
					"AndroidExperimentation Async Experiment Task pre execute");
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			Log.i("AndroidExperimentation",
					"AndroidExperimentation Async Experiment Task  update progress");
		}

		@Override
		protected void onCancelled() {
			Log.i("AndroidExperimentation",
					"AndroidExperimentation Async Experiment Task cancelled");
		}
	}
	
    private static boolean match(String[] smartphoneDependencies, String[] experimentDependencies) {
        for (String expDependency : experimentDependencies) {
            boolean found = false;
            for (String smartphoneDependency : smartphoneDependencies) {
                if (smartphoneDependency.equals(expDependency)) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                return false;
            }
        }
        return true;
    }

}
