
class RemoteNodesInfo {

	private def clusterInfo
	private def hostInfo

	public RemoteNodesInfo( remoteHost ) {
		def ant = new AntBuilder()   // create an antbuilder
		ant.exec(
			outputproperty:"cmdOut",
			errorproperty: "cmdErr",
			resultproperty:"cmdExit",
			executable: "wsrf-query"
			) { arg(line:" -s "+
				"https://$remoteHost:8443/wsrf/services/DefaultIndexService " +
				"\"//*/glue:GLUECE//glue:ComputingElement" +
				"[glue:Info/@glue:LRMSType='SGE' and glue:State/@glue:FreeCPUs>=0]\"") }
		def matcher = ( ant.project.properties.cmdOut =~ /ns[0-9]*:/ );
		clusterInfo = new XmlSlurper().parseText(matcher.replaceAll(""));

		ant = new AntBuilder()
		ant.exec(
			outputproperty:"cmdOut",
			errorproperty: "cmdErr",
			resultproperty:"cmdExit",
			executable: "wsrf-query"
			) { arg(line:" -s "+
				"https://$remoteHost:8443/wsrf/services/DefaultIndexService " +
				"\"//ns1:Entry[ns1:MemberServiceEPR[*[local-name()=\'ReferenceProperties\']" +
				"/*[local-name()=\'ResourceID\' and text()=\'Multi\']]]" +
				"//*[local-name()=\'GLUECE\']//glue:Cluster//glue:SubCluster\"") }
		matcher = ( ant.project.properties.cmdOut =~ /ns[0-9]*:/ );
		hostInfo = new XmlSlurper().parseText(matcher.replaceAll(""));
	}

	public String freeCPUs() {
		return clusterInfo.'State'.@'FreeCPUs';
	}

	public String runningJobs() {
		return clusterInfo.'State'.@'RunningJobs';
	}

	public String waitingJobs() {
		return clusterInfo.'State'.@'WaitingJobs';
	}

	public String coresList() {
		return hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text() };
	}

	public String memoryList() {
		return hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text() };
	}

	public String processorList() {
		return hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text() };
	}

	public String totalCores() {
		return hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() }.sum();
	}

	public String totalMemory() {
		return hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() }.sum();
	}

	public String totalProcessor() {
		return hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() }.sum();
	}

	public String averageCores() {
		def infos = hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() };
		return infos.sum()/infos.size();
	}

	public String averageMemory() {
		def infos = hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() };
		return infos.sum()/infos.size();
	}

	public String averageProcessor() {
		def infos = hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() };
		return infos.sum()/infos.size();
	}

	public String maxCores() {
		return hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() }.max();
	}

	public String maxMemory() {
		return hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() }.max();
	}

	public String maxProcessor() {
		return hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() }.max();
	}

	public String minCores() {
		return hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() }.min();
	}

	public String minMemory() {
		return hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() }.min();
	}

	public String minProcessor() {
		return hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() }.min();
	}

}

def info = new RemoteNodesInfo("gs.alab.ip.titech.ac.jp");
println "FreeCPUs: ${info.freeCPUs()}";
println "run: ${info.runningJobs()}";
println "wait: ${info.waitingJobs()}";
println "Cores: ${info.coresList()} ${info.totalCores()} ${info.averageCores()} ${info.minCores()} ${info.maxCores()}";
println "Mem: ${info.memoryList()} ${info.totalMemory()} ${info.averageMemory()} ${info.minMemory()} ${info.maxMemory()}";
println "CPU: ${info.processorList()} ${info.totalProcessor()} ${info.averageProcessor()} ${info.minProcessor()} ${info.maxProcessor()}";
