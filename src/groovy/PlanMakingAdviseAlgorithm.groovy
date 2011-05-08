
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as AdviseLock
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PlanMakingAdviseAlgorithm implements AdviseAlgorithm {

	static AdviseLock adviseLock = new AdviseLock();
	static boolean clusterIsAvailable = true;

	boolean disposeHandle = true;
	boolean disposeSharedHandle = true;
	boolean clusterObserverIsStarted = false;
	boolean haveJobsToSubmit = true;

	private def advisorInst;
	private def needWaitForTest = true;
	private def cachedExecutor;

	def setBestCluster( Advisor advisor ) {
		this.advisorInst = advisor;
		def ready = true;
		synchronized ( clusterObserverIsStarted ) {
			if( clusterObserverIsStarted ) { sleep 10;  return false; }
			clusterObserverIsStarted = true;
			def threadId = Thread.currentThread().name;
			executorService.invokeWithSession("[$threadId][PlanMakingAdviseAlgorithm]") {
				log.info " ${advisorInst.name} start Plan making2... ";
				while( haveJobsToSubmit ) {
					if ( clusterIsAvailable ) { 
						// advisorInfoService.needAdvise( advisorInst ) && 
						adviseLock.lock();
						try {
							costLimitPlaning('costPrior');
							costLimitPlaning('performancePrior');
							costLimitPlaning('costPerformancePrior');
							break;
							// fetchAvailableClusterByPropMatch()
						} finally { adviseLock.unlock() }
					} else { sleep 500 }
				}
				log.info " [DBG] ${advisorInst.name} waiting for Plan selection... ";
				def planReadyDate = (new Date()).time/1000;
				def selectedPlan = planService.getSelectedPlan();
				while( selectedPlan == false ) {
					sleep 5000; 
					selectedPlan = planService.getSelectedPlan();
				}
				def selectionTime = (new Date()).time/1000 - planReadyDate;
				log.info " [DBG] plan ${selectedPlan} will be used. [${selectionTime}] ";
				planService.executePlan(selectedPlan, selectionTime);
				log.info " ${advisorInst.name} finalized.(${this}) No More Job to serve ";
			}
		}
		log.info " jobInfoService is : $jobInfoService ";
	}

	def fetchAllCluster() {
		def allCluster = [];
		Cluster.list().each { aCluster ->
			log.info "\t\t fetchAllCluster ... now test [$aCluster] ";
			def entry = [:]; entry[ 'cluster' ] = aCluster;
			def limit = aCluster.calcTotalAvailableNodesNum();
			def runningJobs = advisorInfoService.fetchRunningJobListByCluster( aCluster );
			def handleSize = limit - runningJobs.size(); 
			// entry[ 'num' ] = handleSize;
			entry[ 'num' ] = limit;
			log.info "\t\t [${aCluster}][lim:${limit}]" + 
				"[run:${runningJobs.size()}][num:$handleSize && num>0 is prefered]";
			def handleStatus = runningJobs.collect { jobId ->
				def job = Job.get( jobId );
				def est = job.exectionEstimateTime();
				est = (est.after(new Date()))? est : new Date() + 7;
				[ 'job':job, 'est':est ];
			}
			// def dummyJob = 
			// new Job( name:'waitingForJob', cluster: aCluster, executionTime:0 )
			// handleSize.times { handleStatus << [ 'job':null, 'est': new Date() ] }
			entry[ 'handleStatus' ] = handleStatus.sort { it.est };
			allCluster << entry;
		}
		return allCluster;
	}

	def fetchAvailableClusterByPropMatch() {
		def jobList = []
			while( advisorInfoService.needAdvise( advisorInst ) ) {
				jobList << advisorInfoService.fetchJob( advisorInst )
			}
		if( !jobList ) return;
		propertyMatchByJobCalcTime( jobList );
		jobList.each { advisorInfoService.addJob(it) }
		// log.info "**** ${advisorInst} *LEFTJOBS* $jobList "
	}

	def costLimitPlaning( priorKind ) {
		def allClusters = fetchAllCluster();
		def clusterInfos = planService.getClusterRanking(priorKind,allClusters);
		log.info "\t\t costLimitPlanning ... now test [$priorKind] ";
		def availableClusters = clusterInfos.findAll { it.num > 0 }
		if( !availableClusters ) {
			synchronized( clusterIsAvailable ) { clusterIsAvailable = false }
			return
		}
		planService.makePlan( priorKind, 'PlanMaking', availableClusters );
	}

	def multiSumbmit() {
		def clusterInfo = fetchAllCluster().sort { -((double)it.cluster.cpuInfo) }
		def availableClusters = clusterInfo.findAll { it.num > 0 }
		if( !availableClusters ) {
			synchronized( clusterIsAvailable ) { clusterIsAvailable = false }
			return;
		}
		clusterInfo.each { entry ->
			// log.info "\n [CLT] ${entry.cluster}[${entry.num}] -- ${new Date()} "
			// entry.handleStatus.each { log.info " [HDL] ${it} " }
		}
		def usedClusters = [];
		availableClusters.each { aClusterInfo ->
			def handleSize = aClusterInfo.num
				while( advisorInfoService.needAdvise( advisorInst ) && handleSize ) {
					def jobId = advisorInfoService.fetchJob( advisorInst );
					jobInfoService.updateJob( jobId, [disposeHandle:disposeHandle, 
							disposeSharedHandle:disposeSharedHandle] );
					planService.assignCluster( jobId, aClusterInfo.cluster );
					handleSize--;
				}
			if( handleSize == 0 ) { usedClusters << aClusterInfo }
		}

		availableClusters -= usedClusters;
		if( advisorInfoService.jobSubmitionFinished ) {
			adviseMultipleJobSubmition( availableClusters, clusterInfo )
		}
	}

	private def adviseMultipleJobSubmition( availableClusters, clusterInfo ) {
		if( !availableClusters.size() ) return;
		if( haveJobsToSubmit ) log.info " [TRY] Try to MultiSubmit Job.[${this}] ";
		availableClusters.each { aClusterInfo ->
			def theJob;
			clusterInfo.findAll { 
				it.cluster.name != aClusterInfo.cluster.name;
			}.find { uClusterInfo ->
				advisorInfoService.
					fetchRunningJobListByCluster( uClusterInfo.cluster ).
					find { jobId ->
						def job = Job.get( jobId );
						// if( job.clonedJobs || job.advise ) return false;
						theJob = job;
					}
				theJob;
			}
			// log.info " [FND] ${aClusterInfo.cluster}[${aClusterInfo.num}]" + 
			//	"FIND $theJob -- ${new Date()} ";
			if(theJob) {
				def theCluster = aClusterInfo.cluster; 
				theCluster.incMultiSubmissionJobNumbers();
				def reason = " $theCluster is ready for use. ";
				def postRemoveProc = { 
					def cluster = theCluster; 
					log.info " [DBG] advise is removed. ";
					cluster.decMultiSubmissionJobNumbers(); 
				}
				def advise = new Advise( 
						job:theJob, 
						cluster:theCluster, 
						method:'multiSubmission', 
						reason:reason, 
						postRemoveProc:postRemoveProc );
				// log.info " [DBG] make Advise ${advise} ";
				theJob.advise = advise; 
				advise.save();
				def waitAdviseAcception = new Thread() {
					log.info " [DBG] checking Advise ${advise} ";
					while( !advise.used && advise.isValid() ) { 
						if( theCluster.calcAvailableNodesNumNow() < 0 ) {
							advise.valid = false; 
							break;
						}
						sleep 10;
					}
					log.info " [DBG] processing Advise ${advise} ";
					if( !advise.isValid() ) {
						log.info " [DEL] $advise Unvalid -- ${new Date()} "; 
						advise.removeUnvalid(); 
						return;
					} else {
						def clnJob = theJob.clone();
						// log.info " [CLN] $theCluster[${aClusterInfo.num}]" +
						//	" CLONEJOB ${clnJob} -- ${new Date()} ";
						jobInfoService.updateJob( jobId, [disposeHandle:disposeHandle,
								disposeSharedHandle:disposeSharedHandle] );
						planService.assignCluster( clnJob, theCluster );
					}
				}
				cachedExecutor.execute( waitAdviseAcception );
			} else {
				if ( !advisorInfoService.needAdvise( advisorInst ) ) 
					haveJobsToSubmit = false;
				if( !haveJobsToSubmit ) log.info " [DBG] No job to serve. ";
			}
		}
	}

	private def fetchMaxJobExecutionTime( long jobId ) {
		def job = Job.get( jobId );
		def maxVal = estimateCalcTime( job, 50 ).
			findAll { e -> e.c.name == job.cluster.name }.sort{it.t}.reverse().get(0).t
			return maxVal
	}

	private def estimateJobExecutionTime( long jobId, int aTime ) {
		def job = Job.get( jobId );
		def maxVal = fetchMaxJobExecutionTime( job )
			if( job.executionStartTime ) {
				calcJobExecutionTime( job, aTime != maxVal )
			} else { sleep 10 }
		return maxVal
	}

	private calcJobExecutionTime( long jobId, boolean force = false ) {
		def job = Job.get( jobId );
		if( force ) log.info " [TST] $job FORCE RENEW RANK ";
		long executionTime = ( new Date().time - job.executionStartTime.time ) / 1000;
		if( executionTime > job.executionTime || force ) {
			doCalcJobExecutionTime( job, executionTime )
		}
	}

	private 
		doCalcJobExecutionTime( long jobId, long execTimeNow ) {
			def job = Job.get( jobId );
			if( jobInfoService.jobIsFinished(jobId) ) return
				def cTime = estimateCalcTime( jobId, 50 ).
				findAll { it.c.name == job.cluster.name && it.t > execTimeNow }
			if( cTime ) {
				cTime.sort{ it.t }; job.executionTime = cTime.get(0).r
					log.info " [CHG] $job *D* ${job.executionTime} " + 
					" < $execTimeNow -- ${new Date()} "
			} else { job.executionTime = execTimeNow }
			sleep 100000; //job.mySave()
		}

	private def estimateCalcTime( long jobId, int threshold, boolean allInfo = false ) {
		def job = Job.get( jobId );
		estimateCalcTime( job.function, threshold, allInfo )
	}

	private def estimateCalcTime( RemoteFunction function, int threshold, boolean allInfo = false ) {
		def freqency = []
			Cluster.list().each { aCluster ->
				def jobList = advisorInfoService.fetchFinishedJobListByCluster( aCluster )
					if( allInfo ) jobList += advisorInfoService.fetchRunningJobListByCluster( aCluster )
					jobList.each { aJobId ->
						def aJob = Job.get( aJobId );
						if( aJob.function.name == function.name ) {
							freqency << [ 'c': aCluster, 
									 'q':aJob.queuingTime , 'e':aJob.executionTime ]
						}
					}
				if( !freqency.find { it.c.name == aCluster.name } ) {
					freqency << [ 'c': aCluster, 'q':0 , 'e':1000000 ]
				}
			}
		// log.info " [FRQ1] $function -- $freqency "
		def result = getClusteredExecTime( freqency, threshold )
			return result
	}

	private def getClusteredExecTime( freqency, int threshold ) {
		def result = [];
		Cluster.list().each { aCluster -> 
			freqency.findAll { it.c.name == aCluster.name }.each {
				long estime = it.e-it.q;
				long rank = ( (int)(estime/threshold) + 1) * threshold;
				result << [ 'c':aCluster, 'r':rank, 't':estime ]
			}
		}
		return result;
	}
}

/*
   private
   def doMultipleJobSubmition( availableClusters, clusterInfo ) {
   def functionList = [:]
   RemoteFunction.list().each { func -> functionList[ func.name ] = estimateCalcTime( func, 50, true ) }
   availableClusters.each { aClusterInfo ->
// log.info " [XTR] ${aClusterInfo.cluster}[${aClusterInfo.num}] -- ${new Date()} "
// log.info " [XTR2] $functionList "
def candidatedJobs = []
clusterInfo.findAll { it.num == 0 }.each { uClusterInfo ->
advisorInfoService.fetchRunningJobListByCluster( uClusterInfo.cluster ).each { job ->
if( job.name == "dummyJob" ) return
if( job.clonedJobs || candidatedJobs.find { it.j.function.name == job.function.name } ) return
def executionTimeRank = functionList.get( job.function.name )
def baseExecTimeEntry = 
executionTimeRank.findAll{ it.c.name == aClusterInfo.cluster.name }.sort{-it.t}
// log.info " [XTR3] $executionTimeRank -- $baseExecTimeEntry "
def baseExecTime = baseExecTimeEntry.getAt(0).t
def cmpExecTimeEntry = executionTimeRank.findAll{ 
	it.c.name != aClusterInfo.cluster.name && it.t > baseExecTime }.sort{-it.t}
	// log.info " [XTR3.5] $executionTimeRank -- $cmpExecTimeEntry "
	if( cmpExecTimeEntry ) {
		def cmpExecTime = cmpExecTimeEntry.getAt(0).t
			// log.info " [XTR4] $baseExecTimeEntry -- $cmpExecTimeEntry [$baseExecTime < $cmpExecTime] "
			candidatedJobs << [ 't': cmpExecTime - baseExecTime, 'j': job ]
	}
}
}
// log.info " [XTR5] $candidatedJobs "
	if(candidatedJobs) {
		def clnJob = candidatedJobs.sort{-it.t}.getAt(0).j.clone()
			log.info " [CLN] ${aClusterInfo.cluster}[${aClusterInfo.num}] CLONEJOB ${clnJob} -- ${new Date()} "
			aClusterInfo.cluster.multiSubmissionJobNumbers++
			aClusterInfo.cluster.save()
			def reason = " $aClusterInfo.cluster is ready for use. "
			// def advise = new Advise( job:clnJob, reason:reason ); advise.save()
			def waitAdviseAcception = new Thread() {
				while( !advise.used ) { sleep 1000 }
				assignCluster( clnJob, aClusterInfo.cluster )
			}
		waitAdviseAcception.start()
	} else {
		// log.info " [XTR6] $aClusterInfo Mark UnAvailable "
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
				jobInfoService.updateJob( job.id, [disposeHandle:disposeHandle, 
						disposeSharedHandle:disposeSharedHandle] );
				jobInfoService.executeJob( job.id );
				while ( !jobInfoService.jobIsFinished(job.id) ) { sleep 1000 }
				log.info " [BCK] $job *GIVEBACK* ${job.cluster} "
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
					log.info " [TST] $job *${job.function.advisor}* ${aCluster} $running < $limit "
						useCluster = aCluster
						return true
				} else { return false }
		}
	if( useCluster ) {
		job.cluster = useCluster; advisorInfoService.renewJobMap( job )
			log.info " [USE] $job *CLUSTER* ${job.cluster} "
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
					log.info " $job *${job.function.advisor}* ${aCluster} $running < $limit "
						bestClusterList << aCluster
				} else {
					// log.info "$job -- $aCluster limit reached "
				}
		}
	if( bestClusterList ) {
		def clusterId = (int)(Math.random()*100)%bestClusterList.size()
			job.cluster = bestClusterList.get( clusterId )
			log.info " $job *CLUSTER* ${job.cluster} "
			advisorInfoService.renewJobMap( job )
	}
}

*/
