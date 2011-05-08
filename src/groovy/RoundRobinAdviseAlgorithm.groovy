

class RoundRobinAdviseAlgorithm implements AdviseAlgorithm {

	boolean disposeHandle = true
	boolean disposeSharedHandle = false
	boolean haveJobsToSubmit = true

	synchronized
	def setBestCluster( Advisor advisor ) {
		def threadId = Thread.currentThread().name;
		helperService.pushNDC("[$threadId%RoundRobinAdviseAlgorithm]");
		def aJobId = advisorInfoService.fetchJob( advisor );
		def job = Job.get( aJobId );
		if( !job.cluster ) {
			def clusterList = [:];
			def jobListByAd = advisorInfoService.fetchAdvisedJobList();
			Cluster.list().each { aCluster ->
				def jobList = jobListByAd.get( aCluster.name, [:] );
				clusterList[ aCluster.name ] = 
					jobList.get( 'running', []).findAll { 
						def funcName = jobInfoService.getFunctionName(it);
						funcName == job.function.name
					}.size() + 
				jobList.get( 'finished', [] ).findAll { 
					def funcName = jobInfoService.getFunctionName(it);
					funcName == job.function.name 
				}.size() + 
				jobList.get( 'clone', [] ).findAll { 
					def funcName = jobInfoService.getFunctionName(it);
					funcName == job.function.name 
				}.size()
			}
			if( !clusterList ) {
				advisorInfoService.getRandomCluster( aJobId );
			} else {
				def avg = 0; clusterList.each{ avg += it.value/Cluster.count() }
				def theCluster = Cluster.list().find { cluster ->
					clusterList.get( cluster.name, 0 ) <= avg;
				}
				// job = job.updateContents( [cluster:theCluster] );
				jobInfoService.updateJob( aJobId, [cluster:theCluster, 
						disposeHandle:disposeHandle, 
						disposeSharedHandle:disposeSharedHandle] );
				// log.debug " $job *GCLUSINFO* $avg -- ${clusterList} "
			}
		}
		advisorInfoService.renewJobMap( aJobId );
		jobInfoService.executeJob( aJobId );
		log.info "${this} executed $job";
		helperService.popNDC();
		return true;
	}

}

