class ClusterInfoProviderService {
	
    boolean transactional = true 

	private def getInfoTimeSpanLimit = [:];
	private def getInfoTimeSpanDefault = 600000l;
	private def defaultTimeOut = 60000;
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
		// TODO: disabled for sge
		// getClusterInfoThread.start();

		def getNodesInfoThread = new Thread() { getNodesInfos.call( remoteHost ); }
		// getNodesInfoThread.start();

		// log.debug "waiting for MDSInfo. ${remoteHost} ${new Date()} ";
		getClusterInfoThread.join( defaultTimeOut );
		// log.debug "waiting for MDSInfo. ${remoteHost} ${new Date()} ";
		getNodesInfoThread.join( defaultTimeOut );
		// log.debug "finished getting MDSInfo. ${remoteHost} ${new Date()} ";
	}

	def getClusterInfos = { remoteHost ->
		// log.debug "try to get ${remoteHost} clusterInfo from MDS.";
		def query = "https://$remoteHost:8443/wsrf/services/DefaultIndexService " + 
			"\"//*/glue:GLUECE//glue:ComputingElement" +
			"[glue:Info/@glue:LRMSType='PBS' and glue:State/@glue:FreeCPUs>=0]\"";

		def ant = new AntBuilder()   // create an antbuilder
			ant.exec(
					outputproperty:"cmdOut",
					errorproperty: "cmdErr",
					resultproperty:"cmdExit",
					executable: "wsrf-query"
					) { arg(line:" -s $query") }
		try {
			def matcher = ( ant.project.properties.cmdOut =~ /ns[0-9]*:/ );
			def clusterInfo = new XmlSlurper().parseText(matcher.replaceAll(""));
			clustersInfo[ remoteHost ] = clusterInfo;
			if(ant.project.properties.cmdErr != null) {
				throw new Exception("Ant return with error: ");
			}
		} catch ( Exception e ) {
			log.debug "can not get ${remoteHost} clusterInfo from MDS. \n\t $e";
			log.debug "\t query is: ${query} ";
			log.debug "\t ${ant.project.properties.cmdOut} ";
			log.debug "\t ${ant.project.properties.cmdErr} ";
			if( ant.project.properties.cmdErr =~ /Connection timed out/ ) {
				log.debug "\t TimeOut: ${ant.project.properties.cmdErr} ${new Date()} ";
				getInfoTimeSpanLimit[ remoteHost ] = 
					2 * getInfoTimeSpanLimit.get(remoteHost, getInfoTimeSpanDefault);
				return;
			}
		}
	}

	def getNodesInfos = { remoteHost ->
		log.debug "try to get ${remoteHost} nodesInfo from MDS.";
		def query = "https://$remoteHost:8443/wsrf/services/DefaultIndexService " +
			"\"//*[local-name()=\'GLUECE\']//glue:Cluster//glue:SubCluster\"";
		def ant = new AntBuilder();
		ant.exec(
				outputproperty:"cmdOut",
				errorproperty: "cmdErr",
				resultproperty:"cmdExit",
				executable: "wsrf-query"
				) { arg(line:" -s $query") }
		try {
			def hostInfo = ant.project.properties.cmdOut;
			if( !(hostInfo =~ /^</) ) { 
                hostInfo = null;
                log.info "can not get MDS information from host:${remoteHost}";
            }
			def matcher = ( hostInfo =~ /ns[0-9]*:/ );
			if( matcher ) {
				hostInfo = new XmlSlurper().parseText(matcher.replaceAll(""));
			}
			hostsInfo[ remoteHost ] = hostInfo;
			if(ant.project.properties.cmdErr != null) {
				throw new Exception("Ant return with error: ");
			}
		} catch ( Exception e ) {
			log.debug "can not get ${remoteHost} nodesInfo from MDS. \n\t $e ";
			log.debug "\t query is: ${query} ";
			log.debug "\t ant.output: ${ant.project.properties.cmdOut} ";
			log.debug "\t ant.error: ${ant.project.properties.cmdErr} ";
		}
	}

	public String freeCPUs( remoteHost ) {
		def clusterInfo = clustersInfo[ remoteHost ];
        if (!clusterInfo) return
		def result = clusterInfo.'State'.@'FreeCPUs';
		log.debug "freeCPUs $result";
		return (clusterInfo)? result : null;
	}

	public String runningJobs( remoteHost ) {
		def clusterInfo = clustersInfo[ remoteHost ];
        if (!clusterInfo) return
		def result = clusterInfo.'State'.@'RunningJobs';
		log.debug "runningJobs $result";
		return (clusterInfo)? result : null;
	}

	public String waitingJobs( remoteHost ) {
		def clusterInfo = clustersInfo[ remoteHost ];
        if (!clusterInfo) return
		def result = clusterInfo.'State'.@'WaitingJobs';
		log.debug "waitingJobs $result";
		return (clusterInfo)? result : null;
	}

	public String coresList( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text() }
		log.debug "coresList $result";
		return (hostInfo)? result : null;
	}

	public String memoryList( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text() };
		log.debug "memoryList $result";
		return (hostInfo)? result : null;
	}

	public String processorList( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text() };
		log.debug "processorList $result";
		return (hostInfo)? result : null;
	}

	public String totalCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() };
		log.debug "totalCores $result";
		return (hostInfo)? result.sum() : null;
	}

	public String totalMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() };
		log.debug "totalMemory $result";
		return (hostInfo)? result.sum() : null;
	}

	public String totalProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() };
		log.debug "totalProcessor $result";
		return (hostInfo)? result.size() : null;
	}

	public String averageCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() };
		log.debug "averageCores $result";
		def infos = (hostInfo)? result : null;
		return (infos)? infos.sum()/infos.size(): null;
	}

	public String averageMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() };
		log.debug "averageMemory $result";
		def infos = (hostInfo)? result : null;
		return (infos)? infos.sum()/infos.size(): null;
	}

	public String averageProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() };
		log.debug "averageProcessor $result";
		def infos = (hostInfo)? result : null;
		return (infos)? infos.sum()/infos.size(): null;
	}

	public String maxCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() };
		log.debug "maxCores $result";
		return (hostInfo)? result.max() : null;
	}

	public String maxMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() };
		log.debug "maxMemory $result";
		return (hostInfo)? result.max() : null;
	}

	public String maxProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() };
		log.debug "maxProcessor $result";
		return (hostInfo)? result.max() : null;
	}

	public String minCores( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Architecture.@'SMPSize'.collect{ it.text().toInteger() };
		log.debug "minCores $result";
		return (hostInfo)? result.min() : null;
	}

	public String minMemory( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.MainMemory.@'RAMSize'.collect{ it.text().toInteger() };
		log.debug "minMemory $result";
		return (hostInfo)? result.min() : null;
	}

	public String minProcessor( remoteHost ) {
		def hostInfo = hostsInfo[ remoteHost ];
        if (!hostInfo) return
		def result = hostInfo.Host.Processor.@'ClockSpeed'.collect{ it.text().toInteger() };
		log.debug "minProcessor $result";
		return (hostInfo)? result.min(): null;
	}
}
