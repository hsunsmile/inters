
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as AdviseLock

class WaitAnyAdviseAlgorithm implements AdviseAlgorithm {

	def advisorInfoService
	boolean disposeHandle = true
	boolean disposeSharedHandle = false
	static AdviseLock adviseLock = new AdviseLock()
	static boolean clusterIsAvailable = true
	boolean clusterObserverIsStarted = false
	boolean haveJobsToSubmit = true
	private def advisorInst
	

	def setBestCluster( Advisor advisor ) {
		this.advisorInst = advisor
		clusterObserver.start()
	}

	def clusterObserver = new Thread() {
		synchronized ( clusterObserverIsStarted ) {
			if( clusterObserverIsStarted ) return
			clusterObserverIsStarted = true
		}
		while( haveJobsToSubmit ) {
			if ( clusterIsAvailable ) { // advisorInfoService.needAdvise( advisorInst ) && 
				adviseLock.lock()
				try {
					multiSumbmit()
					// fetchAvailableClusterByPropMatch()
				} finally { adviseLock.unlock() }
 			} else { sleep 500 }
		}
		println " [DWN] ${advisorInst.name} finalized.(${this}) No More Job to serve "
	}

	def fetchAllCluster() {
		def allCluster = []
		Cluster.list().each { aCluster ->
			def entry = [:]; entry[ 'cluster' ] = aCluster
			def limit = aCluster.calcTotalAvailableNodesNum()
			def runningJobs = advisorInfoService.fetchRunningJobListByCluster( aCluster )
 			def handleSize = limit - runningJobs.size(); entry[ 'num' ] = handleSize
			def handleStatus = runningJobs.collect { job ->
				def est = job.exectionEstimateTime()
				est = (est.after(new Date()))? est : new Date() + 7
				[ 'job':job, 'est':est ]
			}
			// def dummyJob = new Job( name:'waitingForJob', cluster: aCluster, executionTime:0 )
			handleSize.times { handleStatus << [ 'job':null, 'est': new Date() ] }
			entry[ 'handleStatus' ] = handleStatus.sort { it.est }
			allCluster << entry
		}
		return allCluster
	}

	def fetchAvailableClusterByPropMatch() {
		def jobList = []
		while( advisorInfoService.needAdvise( advisorInst ) ) {
			jobList << advisorInfoService.fetchJob( advisorInst )
		}
		if( !jobList ) return
		propertyMatchByJobCalcTime( jobList )
		jobList.each { advisorInfoService.addJob(it) }
		// println "**** ${advisorInst} *LEFTJOBS* $jobList "
	}

	def multiSumbmit() {
		def clusterInfo = fetchAllCluster().sort { -((double)it.cluster.cpuInfo) }
		def availableClusters = clusterInfo.findAll { it.num > 0 }
		if( !availableClusters ) {
			synchronized( clusterIsAvailable ) { clusterIsAvailable = false }
			return
		}
		clusterInfo.each { entry ->
			println "\n [CLT] ${entry.cluster}[${entry.num}] -- ${new Date()} "
			entry.handleStatus.each { println " [HDL] ${it} " }
		}
		def usedClusters = []
		availableClusters.each { aClusterInfo ->
			def handleSize = aClusterInfo.num
			while( advisorInfoService.needAdvise( advisorInst ) && handleSize ) {
				def job = advisorInfoService.fetchJob( advisorInst )
				assiagnCluster( job, aClusterInfo.cluster )
				handleSize--
			}
			if( handleSize == 0 ) { usedClusters << aClusterInfo }
		}

		availableClusters -= usedClusters
		if( advisorInfoService.jobSubmitionFinished ) {
			adviseMultipleJobSubmition( availableClusters, clusterInfo )
		}
	}

	private
	def adviseMultipleJobSubmition( availableClusters, clusterInfo ) {
		if( !availableClusters.size() ) return
		if( haveJobsToSubmit ) println " [TRY] Try to MultiSubmit Job.[${this}] "
		availableClusters.each { aClusterInfo ->
			def theJob
			clusterInfo.findAll { it.cluster.name != aClusterInfo.cluster.name }.find { uClusterInfo ->
				advisorInfoService.fetchRunningJobListByCluster( uClusterInfo.cluster ).find { job ->
					if( job.clonedJobs || job.advise ) return false
					theJob = job
				}
				theJob
			}
			println " [FND] ${aClusterInfo.cluster}[${aClusterInfo.num}] FIND $theJob -- ${new Date()} "
			if(theJob) {
				def theCluster = aClusterInfo.cluster; theCluster.incMultiSubmissionJobNumbers()
				def reason = " $theCluster is ready for use. "
				def postRemoveProc = { def cluster = theCluster; println " [DBG] advise is removed. "
						cluster.decMultiSubmissionJobNumbers() }
				def advise = new Advise( job:theJob, cluster:theCluster, method:'multiSubmission', 
								reason:reason, postRemoveProc:postRemoveProc );
				println " [DBG] make Advise ${advise} "
				theJob.advise = advise; advise.save()
				def waitAdviseAcception = new Thread() {
					println " [DBG] checking Advise ${advise} "
					while( !advise.used && advise.isValid() ) { 
						if( theCluster.calcAvailableNodesNumNow() < 0 ) {
							advise.valid = false; break
						}
						sleep 10
					}
					println " [DBG] processing Advise ${advise} "
					if( !advise.isValid() ) {
						println " [DEL] $advise Unvalid -- ${new Date()} "; advise.removeUnvalid(); return
					} else {
						def clnJob = theJob.clone()
						println " [CLN] $theCluster[${aClusterInfo.num}] CLONEJOB ${clnJob} -- ${new Date()} "
						assiagnCluster( clnJob, theCluster )
					}
				}
				waitAdviseAcception.start()
			} else {
				if ( !advisorInfoService.needAdvise( advisorInst ) ) haveJobsToSubmit = false 
				if( !haveJobsToSubmit ) println " [DBG] No job to serve. "
			}
		}
	}

	def assiagnCluster( Job job, Cluster cluster ) {
		if( !job.cluster ) job.cluster = cluster
		advisorInfoService.renewJobMap( job )
		def aThread = new Thread() {
			job.execute( disposeHandle, disposeSharedHandle )
		}
		aThread.start()
		def jobObserver = new Thread() {
			int maxValue = -1
			while ( !job.isFinished() ) {
				// maxValue = estimateJobExecutionTime( job, maxValue )
				sleep 1000
			}
			synchronized( clusterIsAvailable ) { clusterIsAvailable = true }
			advisorInfoService.renewJobMap( job )
			if( job.status == "finished" ) println " [FIN] ${job}[${job.cluster}] -- ${ new Date() }"
			// estimateCalcTime( job, 50 ).each { e -> println " [FIN] $e" }
		}
		jobObserver.start()
	}

	private
	def fetchMaxJobExecutionTime( Job job ) {
		def maxVal = estimateCalcTime( job, 50 ).
			findAll { e -> e.c.name == job.cluster.name }.sort{it.t}.reverse().get(0).t
		return maxVal
	}

	private
	def estimateJobExecutionTime( Job job, int aTime ) {
		def maxVal = fetchMaxJobExecutionTime( job )
		if( job.executionStartTime ) {
			calcJobExecutionTime( job, aTime != maxVal )
		} else { sleep 10 }
		return maxVal
	}

	private 
	calcJobExecutionTime( Job job, boolean force = false ) {
		if( force ) println " [TST] $job FORCE RENEW RANK "
		long executionTime = ( new Date().time - job.executionStartTime.time ) / 1000
		if( executionTime > job.executionTime || force ) {
			doCalcJobExecutionTime( job, executionTime )
		}
	}

	private 
	doCalcJobExecutionTime( Job job, long execTimeNow ) {
		if( job.isFinished() ) return
		def cTime = estimateCalcTime( job, 50 ).findAll { it.c.name == job.cluster.name && it.t > execTimeNow }
		if( cTime ) {
				cTime.sort{ it.t }; job.executionTime = cTime.get(0).r
				println " [CHG] $job *D* ${job.executionTime} " + 
						" < $execTimeNow -- ${new Date()} "
		} else { job.executionTime = execTimeNow }
		sleep 100000; //job.save()
	}

	private
	def estimateCalcTime( Job job, int threshold, boolean allInfo = false ) {
		estimateCalcTime( job.function, threshold, allInfo )
	}

	private
	def estimateCalcTime( RemoteFunction function, int threshold, boolean allInfo = false ) {
		def freqency = []
		Cluster.list().each { aCluster ->
			def jobList = advisorInfoService.fetchFinishedJobListByCluster( aCluster )
			if( allInfo ) jobList += advisorInfoService.fetchRunningJobListByCluster( aCluster )
			jobList.each { aJob -> 
				if( aJob.function.name == function.name ) {
					freqency << [ 'c': aCluster, 
						'q':aJob.queuingTime , 'e':aJob.executionTime ]
				}
			}
			if( !freqency.find { it.c.name == aCluster.name } ) {
				freqency << [ 'c': aCluster, 'q':0 , 'e':1000000 ]
			}
		}
		// println " [FRQ1] $function -- $freqency "
		def result = getClusteredExecTime( freqency, threshold )
		return result
	}
	
	private
	def getClusteredExecTime( freqency, int threshold ) {
		def result = []
		Cluster.list().each { aCluster -> 
			freqency.findAll { it.c.name == aCluster.name }.each {
				long estime = it.e-it.q, rank = ( (int)(estime/threshold) + 1) * threshold
				result << [ 'c':aCluster, 'r':rank, 't':estime ]
			}
		}
		return result
	}
}

/*
	private
	def doMultipleJobSubmition( availableClusters, clusterInfo ) {
		def functionList = [:]
		RemoteFunction.list().each { func -> functionList[ func.name ] = estimateCalcTime( func, 50, true ) }
		availableClusters.each { aClusterInfo ->
			// println " [XTR] ${aClusterInfo.cluster}[${aClusterInfo.num}] -- ${new Date()} "
			// println " [XTR2] $functionList "
			def candidatedJobs = []
			clusterInfo.findAll { it.num == 0 }.each { uClusterInfo ->
				advisorInfoService.fetchRunningJobListByCluster( uClusterInfo.cluster ).each { job ->
					if( job.name == "dummyJob" ) return
					if( job.clonedJobs || candidatedJobs.find { it.j.function.name == job.function.name } ) return
					def executionTimeRank = functionList.get( job.function.name )
					def baseExecTimeEntry = 
						executionTimeRank.findAll{ it.c.name == aClusterInfo.cluster.name }.sort{-it.t}
					// println " [XTR3] $executionTimeRank -- $baseExecTimeEntry "
					def baseExecTime = baseExecTimeEntry.getAt(0).t
					def cmpExecTimeEntry = executionTimeRank.findAll{ 
							it.c.name != aClusterInfo.cluster.name && it.t > baseExecTime }.sort{-it.t}
					// println " [XTR3.5] $executionTimeRank -- $cmpExecTimeEntry "
					if( cmpExecTimeEntry ) {
						def cmpExecTime = cmpExecTimeEntry.getAt(0).t
						// println " [XTR4] $baseExecTimeEntry -- $cmpExecTimeEntry [$baseExecTime < $cmpExecTime] "
						candidatedJobs << [ 't': cmpExecTime - baseExecTime, 'j': job ]
					}
				}
			}
			// println " [XTR5] $candidatedJobs "
			if(candidatedJobs) {
				def clnJob = candidatedJobs.sort{-it.t}.getAt(0).j.clone()
				println " [CLN] ${aClusterInfo.cluster}[${aClusterInfo.num}] CLONEJOB ${clnJob} -- ${new Date()} "
				aClusterInfo.cluster.multiSubmissionJobNumbers++
				aClusterInfo.cluster.save()
				def reason = " $aClusterInfo.cluster is ready for use. "
				// def advise = new Advise( job:clnJob, reason:reason ); advise.save()
				def waitAdviseAcception = new Thread() {
					while( !advise.used ) { sleep 1000 }
					assiagnCluster( clnJob, aClusterInfo.cluster )
				}
				waitAdviseAcception.start()
			} else {
				// println " [XTR6] $aClusterInfo Mark UnAvailable "
				// def dummyJob = new Job( name:'dummyJob', cluster: aClusterInfo.cluster, executionTime:0 )
				// advisorInfoService.renewJobMap( dummyJob )
			}
		}
	}

	def setBestClusterWA( Advisor advisor ) {
		def job = advisorInfoService.fetchJob( advisor )
		def jobObserver = new Thread() {
			while( !job.cluster ) {
				adviseLock.lock()
				try {
					useAvailableClusterByCPURanking( job )
					sleep 1000
				} finally { adviseLock.unlock() }
			}
			job.execute( disposeHandle, disposeSharedHandle )
			while ( !job.isFinished() ) { sleep 1000 }
			println " [BCK] $job *GIVEBACK* ${job.cluster} "
			advisorInfoService.renewJobMap( job )
		}
		jobObserver.start()
	}

	def useAvailableClusterByCPURanking( Job job ) {
		def jobListByAd = 
				advisorInfoService.fetchAdvisedJobList( job.function.advisor )
		def useCluster
		Cluster.list().sort{ -it.cpuInfo }.find { aCluster ->
			def limit = aCluster.calcTotalAvailableNodesNum()
			def active = advisorInfoService.fetchRunningJobListByCluster( aCluster ).size()
			if( running < limit ) {
				println " [TST] $job *${job.function.advisor}* ${aCluster} $running < $limit "
				useCluster = aCluster
				return true
			} else { return false }
		}
		if( useCluster ) {
			job.cluster = useCluster; advisorInfoService.renewJobMap( job )
			println " [USE] $job *CLUSTER* ${job.cluster} "
		}
	}

	def getClusterStillAvailableByRandom( Job job ) {
		def jobListByAd = 
				advisorInfoService.fetchAdvisedJobList( job.function.advisor )
		def bestClusterList = []
		Cluster.list().each { aCluster ->
			def limit = aCluster.calcTotalAvailableNodesNum()
			def running = jobListByAd.get( aCluster.name, [:] )
						  .get( 'running', [] ).size()
			if( running < limit ) { 
				println " $job *${job.function.advisor}* ${aCluster} $running < $limit "
				bestClusterList << aCluster
			} else {
				// println "$job -- $aCluster limit reached "
			}
		}
		if( bestClusterList ) {
			def clusterId = (int)(Math.random()*100)%bestClusterList.size()
			job.cluster = bestClusterList.get( clusterId )
			println " $job *CLUSTER* ${job.cluster} "
			advisorInfoService.renewJobMap( job )
		}
	}

*/
