import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.StreamGobbler;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.ServerHostKeyVerifier;

class SshExecutorService {

	static def hostname = "kuruwa-no-hosts";
	static def username = "login-user-name";
	static def password = "login-password";

	static def set( params ) {
		hostname = params.hostname;
		username = params.username || "root";
		password = params.password || "";
		return this;
	}

	static def execute( String command ) {
		def database = new KnownHosts();
		def knownHosts = new File("/dev/null");
		if (knownHosts.exists()) database.addHostkeys(knownHosts);
		try {
			def env = System.getenv();
			def conn = new Connection(hostname);
			conn.connect(new SimpleVerifier(database));
			conn.authenticateWithPublicKey(username,new File("${env["HOME"]}/.ssh/inters-dev"),password);
			def sess = conn.openSession();
			sess.execCommand( command );
			def stdout = new StreamGobbler(sess.getStdout());
			new BufferedReader(new InputStreamReader(stdout)).eachLine { line ->
				println "${hostname.split(/\./)[0]}> $line";
			}
			sess.close();
			conn.close();
			return stdout;
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}

class SimpleVerifier implements ServerHostKeyVerifier {
	KnownHosts database;

	public SimpleVerifier(KnownHosts database) {
		if (database == null) throw new IllegalArgumentException();
		this.database = database;
	}

	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
		int result = database.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
		switch (result) {
			case KnownHosts.HOSTKEY_IS_OK:
				return true;
			case KnownHosts.HOSTKEY_IS_NEW:
				database.addHostkey( [ hostname ] as String[], serverHostKeyAlgorithm, serverHostKey);
				return true;
			case KnownHosts.HOSTKEY_HAS_CHANGED:
				return false;
			default:
				throw new IllegalStateException();
		}
	}
}

