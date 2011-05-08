
class MDSInfoProviderService {
	
    boolean transactional = false

	private def getInfoTimeSpanLimit = [:];
	private def getInfoTimeSpanDefault = 600000l;
	private def defaultTimeOut = 10000;
	private def clustersInfo = [:];
	private def hostsInfo = [:];
	private def lastGetInfoTime = [:];

	def initMDSInfoProvider( remoteHost ) {
		def needNewInfo = 
			(new Date()).time - lastGetInfoTime.get(remoteHost, 0) >= 
			getInfoTimeSpanLimit.get(remoteHost, getInfoTimeSpanDefault);
		if( !needNewInfo ) return;

		println "[DBG] need to renew MDSInfo. ${remoteHost} ${new Date()} ";
		lastGetInfoTime[ remoteHost ] = (new Date()).time;

		def getClusterInfoThread = new Thread() { getClusterInfos.call( remoteHost ); }
		getClusterInfoThread.start();

		def getNodesInfoThread = new Thread() { getNodesInfos.call( remoteHost ); }
		getNodesInfoThread.start();

		// println "[DBG] waiting for MDSInfo. ${remoteHost} ${new Date()} ";
		getClusterInfoThread.join( defaultTimeOut );
		// println "[DBG] waiting for MDSInfo. ${remoteHost} ${new Date()} ";
		getNodesInfoThread.join( defaultTimeOut );
		// println "[DBG] finished getting MDSInfo. ${remoteHost} ${new Date()} ";
	}

	def getClusterInfos = { remoteHost ->
		// println "[DBG] try to get ${remoteHost} clusterInfo from MDS.";
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
		try {
			def matcher = ( ant.project.properties.cmdOut =~ /ns[0-9]*:/ );
			def clusterInfo = new XmlSlurper().parseText(matcher.replaceAll(""));
			clustersInfo[ remoteHost ] = clusterInfo;
		} catch ( Exception e ) {
			println "[DBG] can not get ${remoteHost} clusterInfo from MDS.";
			// println "[DBG] \t ${ant.project.properties.cmdOut} ";
			// println "[DBG] \t ${ant.project.properties.cmdErr} ";
			if( ant.project.properties.cmdErr =~ /Connection timed out/ ) {
				println "[DBG] \t TimeOut: ${ant.project.properties.cmdErr} ${new Date()} ";
				getInfoTimeSpanLimit[ remoteHost ] = 
					2 * getInfoTimeSpanLimit.get(remoteHost, getInfoTimeSpanDefault);
				return;
			}
		}
	}

	def getNodesInfos = { remoteHost ->
		// println "[DBG] try to get ${remoteHost} nodesInfo from MDS.";
		def ant = new AntBuilder();
		ant.exec(
			outputproperty:"cmdOut",
			errorproperty: "cmdErr",
			resultproperty:"cmdExit",
			executable: "wsrf-query"
			) { arg(line:" -s "+
				"https://$remoteHost:8443/wsrf/services/DefaultIndexService " +
				"\"//ns1:Entry[ns1:MemberServiceEPR[*[local-name()=\'ReferenceProperties\']" +
				"/*[local-name()=\'ResourceID\' and text()=\'Fork\']]]" +
				"//*[local-name()=\'GLUECE\']//glue:Cluster//glue:SubCluster\"") }
		try {
				matcher = ( ant.project.properties.cmdOut =~ /ns[0-9]*:/ );
				if( matcher ) hostInfo = new XmlSlurper().parseText(matcher.replaceAll(""));
				hostsInfo[ remoteHost ] = hostInfo;
		} catch ( Exception e ) {
			println "[DBG] can not get ${remoteHost} nodesInfo from MDS.";
			// println "[DBG] \t ${ant.project.properties.cmdOut} ";
			// println "[DBG] \t ${ant.project.properties.cmdErr} ";
		}
	}

	public String freeCPUs( remoteHost ) {
		def clusterInfo = clustersInfo[ remoteHost ];
		return (clusterInfo)? clusterInfo.'State'.@'FreeCPUs' : null;
	}

	public String runningJobs( remoteHost ) {
		def clusterInfo = clustersInfo[ remoteHost ];
		return (clusterInfo)? clusterInfo.'State'.@'RunningJobs' : null;
	}

	public String waitingJobs( remoteHost ) {
		def clusterInfo = clustersInfo[ remoteHost ];
		return (clusterInfo)? clusterInfo.'State'.@'WaitingJobs': null;
	}

	public String coresList( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text() } : null;
	}

	public String memoryList( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text() } : null;
	}

	public String processorList( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text() } : null;
	}

	public String totalCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() }.sum() : null;
	}

	public String totalMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() }.sum() : null;
	}

	public String totalProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() }.sum() : null;
	}

	public String averageCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		def infos = (hostInfo)? hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() } : null;
		return (infos)? infos.sum()/infos.size(): null;
	}

	public String averageMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		def infos = (hostInfo)? hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() } : null;
		return (infos)? infos.sum()/infos.size(): null;
	}

	public String averageProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		def infos = (hostInfo)? hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() } : null;
		return (infos)? infos.sum()/infos.size(): null;
	}

	public String maxCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() }.max() : null;
	}

	public String maxMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() }.max() : null;
	}

	public String maxProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() }.max() : null;
	}

	public String minCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() }.min() : null;
	}

	public String minMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() }.min() : null;
	}

	public String minProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
		return (hostInfo)? hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() }.min(): null;
	}

}
