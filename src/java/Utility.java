
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class Utility {

	public final static String pathSeparator = "/";

	private static String _serverHome = null;
	
	public static String getConfigFilePath(String configFileName) {
		if(_serverHome != null) { return _serverHome; }
		String serverHome = ".";
		StringTokenizer classpath = new StringTokenizer(System
				.getProperty("java.class.path"), ":");
		while (classpath.hasMoreTokens()) {
			String aPath = classpath.nextToken();
			//System.out.println("Examnie path: " + aPath);
			if (aPath.matches(".*" + "jar")) {
				StringTokenizer scheHomePre = new StringTokenizer(aPath,
						pathSeparator);
				serverHome = "";
				for (int i = 0; i < scheHomePre.countTokens(); i++) {
					serverHome += pathSeparator + scheHomePre.nextToken();
				}
				serverHome += pathSeparator + "webapps" + pathSeparator
						+ "jp.ac.titech.ip.alab.grpc.scheduler.Webmanager";
				_serverHome = serverHome;
				//System.out.println("SCHEDULER HOME is: " + serverHome);
				File configFile = new File(serverHome + pathSeparator
						+ configFileName);
				if (configFile.exists()) {
					return serverHome;
				}
			}
		}
		return serverHome;
	}
	
	public static String getCurrentDate(String... pattern) {
		Date date1 = new Date(); 
		String usePattern = "yyyy/MM/dd HH:mm:ss:S";
		if (pattern.length > 0) {
			usePattern = pattern[0];
		}
	    SimpleDateFormat sdf1 = new SimpleDateFormat(usePattern);
	    return sdf1.format(date1);
	}
}
