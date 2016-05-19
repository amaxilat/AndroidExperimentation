package eu.smartsantander.androidExperimentation.util;

import java.io.File;

import eu.smartsantander.androidExperimentation.operations.Downloader;

public class Constants {
    public static final String EXPERIMENT_PLUGIN_CONTEXT_TYPE = "org.ambientdynamix.contextplugins.ExperimentPlugin";
    public static final String MIXPANEL_TOKEN = "2341a661b1da35ad47227521fbf6945f";
    public static String URL = "http://set.organicity.eu:8080";
    public static final int PHONE_ID_UNITIALIZED = -1;
    public static String activityStatus = "unknown";
    public static final long EXPERIMENT_POLL_INTERVAL = 15000;


    // Foursquare App params
    public static final String ORGANICITY_APP_KEY = "smartphone-experiment-management";
    public static final String ORGANICITY_APP_CALLBACK_OAUTHCALLBACK = "http://set.organicity.eu/oauth/complete";
    public static final String ORGANICITY_APP_OAUTH_BASEURL = "https://accounts.organicity.eu/realms/organicity/protocol/openid-connect";
    public static final String ORGANICITY_APP_OAUTH_URL = "/auth";

    public static void checkFile(String filename, String url) throws Exception {
        File root = android.os.Environment.getExternalStorageDirectory();
        File myfile = new File(root.getAbsolutePath() + "/dynamix/" + filename);

        if (!myfile.exists()) {
            Downloader downloader = new Downloader();
            downloader.DownloadFromUrl(url, filename);
        }
    }

    static public void checkExperiment(String contextType, String url) throws Exception {
        File root = android.os.Environment.getExternalStorageDirectory();
        File myfile = new File(root.getAbsolutePath() + "/dynamix/" + contextType);

        if (!myfile.exists()) {
            Downloader downloader = new Downloader();
            downloader.DownloadFromUrl(url, contextType);
        }
    }

    public static boolean match(String[] smartphoneDependencies, String[] experimentDependencies) {
        for (String expDependency : experimentDependencies) {
            boolean found = false;
            for (String smartphoneDependency : smartphoneDependencies) {
                if (smartphoneDependency.equals(expDependency)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

}



