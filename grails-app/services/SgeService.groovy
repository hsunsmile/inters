
class SgeService {

	static def INST_TEMPLATE = "conf/inst_template.conf";

	static def addNode( params ) {
		def hostname = params.hostname;
		def isEC2 = (params.isEC2 == null)? true : params.isEC2;
		def configure_file = "/tmp/sge_execd_template_${hostname}";
		def inst_conf = new File( configure_file );
		inst_conf.delete();
		inst_conf << new File(INST_TEMPLATE).text.replaceAll("__HOSTNAME__",hostname);
		def addhost = "qconf -ah ${hostname}".execute().text;
		println "added host ${hostname}: ${addhost}";
		def execute_dir = new File(System.env['SGE_ROOT']);
		def installhost = "./inst_sge -x -auto ${configure_file}".execute(null,execute_dir).text;
		println "install host ${hostname}: ${installhost}";
		addhost = "qconf -aattr hostgroup hostlist ${hostname} @inters".execute().text;
		println "addq host ${hostname}: ${addhost}";
		def numOfCores = (isEC2)? 2:4;
		println "qconf -aattr queue slots [${hostname}=${numOfCores}] inters.q".execute().text;
		println "ssh root@${hostname} qconf -ke".execute().text;
		println "ssh root@${hostname} sudo -usgeadmin /etc/init.d/sgeexecd.inters-ec2 start".execute().text;
		// TODO: auto detect core amounts
		def nodeInfo = [ name:hostname, isEC2:false, numOfCores:4 ];
		if( isEC2 ) {
			nodeInfo = [name:hostname, ipAddress:params.ipAddress, instanceId:params.instanceId, isEC2:true];
		}
		def node = new ClusterNode( nodeInfo );
		node.save();
		return node.id;
	}

	static def deleteNode( params ) {
		def hostname = params.hostname;
		println "qconf -de ${hostname}".execute().text;
		def result = "qconf -dattr hostgroup hostlist ${hostname} @inters".execute().text;
		println "del host ${hostname}: ${result}";
		println "qconf -dattr queue slots [${hostname}=2] inters.q".execute().text;
	}

	static def getClusterNodes() {
		ClusterNode.list();
	}

}

// SgeService.deleteNode([hostname:"kuruwa12"]);
// SgeService.addNode([hostname:"kuruwa12"]);
// class Pipe {
// 	private Process p;
// 	def Pipe(cmd) {
// 		p = cmd.execute();
// 		p.out.close();
// 	}
// 	def to(cmd) {
// 		def p2 = cmd.execute();
// 		p2.out << p.in;
// 		p2.out.close();
// 		p = p2;
// 		this;
// 	}
// 	def text() {
// 		p.text;
// 	}
// }
// 
// class ShellExecutor {
// }
