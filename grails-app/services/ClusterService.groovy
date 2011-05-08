
class ClusterService {

	boolean transactional = true;

	def schedulerCoreService() { return ServiceReferenceService.references.schedulerCoreService; }
	def executorService() { return ServiceReferenceService.references.executorService; }

	def createCluster( name, price, numberOfNodes, corePerCPU, cpuInfo) {
		try {
			def preCluster = Cluster.list().find { it.name == name; }
			if( !preCluster ) {
				println "[${new Date()}] add cluster $name ";
				new Cluster( name:name, newestPrice:price, numberOfNodes:numberOfNodes,
						numberOfCorePerNode:corePerCPU, cpuInfo:cpuInfo ).mySave();
				// makeAdvice( [] as Job[], " replanning with added $name " );
				rePlanning();
			} else {
				preCluster.newestPrice = price;
				preCluster.numberOfNodes = numberOfNodes;
				preCluster.numberOfCorePerNode = corePerCPU;
				preCluster.cpuInfo = cpuInfo;
				preCluster.save();
			}
		} catch( Exception e ) {
			println "[${new Date()}] error at adding cluster: $e ";
		}
	}

	def removeCluster( long clusterId ) {
		log.info "delete cluster: $clusterId ";
	}

	def rePlanning() {
		if( Job.count() > 0 ) {
			println "[${new Date()}] pause InterS ";
			schedulerCoreService().pause();
			println "[${new Date()}] restart InterS ";
			schedulerCoreService().resume();
		}
	}

	def makeAdvice( long[] jobIdList, String reason ) {
		println "[${new Date()}] makeAdvise for [reason:$reason] ";
		def advise = new Advise( reason:reason );
		println "[${new Date()}] $advise made for [reason:$reason] ";
		jobIdList.each { aJobId -> advise.addToJobIds( aJobId ); }
		advise.mySave();
		executorService().invoke( 100 ) {
			while( advise.isValid() ) {
				if( advise.used ) { break; }
				sleep 1000;
			}
			println "[${new Date()}] $advise start replanning... ";
			rePlanning();
		}
	}

}
