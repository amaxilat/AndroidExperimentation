package eu.smartsantander.androidExperimentation;

import java.io.File;

import eu.smartsantander.androidExperimentation.operations.Downloader;

public class Constants {
	public static String URL="http://blanco.cti.gr";
	public static int PHONE_ID_UNITIALIZED=-1;
	public static final long EXPERIMENT_POLL_INTERVAL = 30000;//5*60000;

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

}



