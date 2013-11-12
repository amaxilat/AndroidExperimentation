package eu.smartsantander.androidExperimentation;

import java.io.File;

import eu.smartsantander.androidExperimentation.operations.Downloader;

public class Constants {
	public static String URL="http://blanco.cti.gr"; //http://83.212.110.88
	public static int PHONE_ID_UNITIALIZED=-1;
	public static final long EXPERIMENT_POLL_INTERVAL = 15000;//5*60000;
	
	
	
	//public enum Datatype {Integer,Float,String};
	
	public static void checkFile(String filename, String url) throws Exception {
		File root = android.os.Environment.getExternalStorageDirectory();
		File myfile = new File(root.getAbsolutePath() + "/dynamix/" + filename);

		if (myfile.exists() == false) {
			Downloader downloader = new Downloader();
			downloader.DownloadFromUrl(url, filename);
		}
	}

	static public void checkExperiment(String contextType, String url)throws Exception {
		File root = android.os.Environment.getExternalStorageDirectory();
		File myfile = new File(root.getAbsolutePath() + "/dynamix/"	+ contextType);

		if (myfile.exists() == false) {
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
            if (found == false) {
                return false;
            }
        }
        return true;
    }

}



