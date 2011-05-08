
import groovy.time.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as AdviseLock

class PropertyMatchAdviseAlgorithm implements AdviseAlgorithm {

	def advisorInfoService
	boolean disposeHandle = false 
	boolean disposeSharedHandle = false
	static AdviseLock adviseLock = new AdviseLock()
	static boolean clusterIsAvailable = true
	boolean clusterObserverIsStarted = false
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
		while( true ) {
			if ( clusterIsAvailable ) { // advisorInfoService.needAdvise( advisorInst ) && 
				adviseLock.lock()
				try {
					multiSumbmit()
					// fetchAvailableClusterByPropMatch()
				} finally { adviseLock.unlock() }
 			} else { sleep 500 }
		}
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
			def dummyJob = new Job( name:'waitingForJob', cluster: aCluster, executionTime:0 )
			handleSize.times { handleStatus << [ 'job':dummyJob, 'est': new Date() ] }
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
		if( availableClusters.size() != clusterInfo.size() && 
				advisorInfoService.jobSubmitionFinished ) {
			def functionList = [:]
			RemoteFunction.list().each { func -> functionList[ func.name ] = estimateCalcTime( func, 50, true ) }
			availableClusters.each { aClusterInfo ->
				println " [XTR] ${aClusterInfo.cluster}[${aClusterInfo.num}] -- ${new Date()} "
				// println " [XTR2] $functionList "
				def candidatedJobs = []
				clusterInfo.findAll { it.num == 0 }.each { uClusterInfo ->
					advisorInfoService.fetchRunningJobListByCluster( uClusterInfo.cluster ).each { job ->
						if( job.name == "dummyJob" ) return
						if( job.clonedJobs || candidatedJobs.find { it.j.function.name == job.function.name } ) return
						def executionTimeRank = functionList.get( job.function.name )
						def baseExecTimeEntry = executionTimeRank.findAll{ it.c.name == aClusterInfo.cluster.name }.sort{-it.t}
						println " [XTR3] $executionTimeRank -- $baseExecTimeEntry "
						def baseExecTime = baseExecTimeEntry.getAt(0).t
						def cmpExecTimeEntry = executionTimeRank.findAll{ 
								it.c.name != aClusterInfo.cluster.name && it.t > baseExecTime }.sort{-it.t}
						println " [XTR3.5] $executionTimeRank -- $cmpExecTimeEntry "
						if( cmpExecTimeEntry ) {
							def cmpExecTime = cmpExecTimeEntry.getAt(0).t
							println " [XTR4] $baseExecTimeEntry -- $cmpExecTimeEntry [$baseExecTime < $cmpExecTime] "
							candidatedJobs << [ 't': cmpExecTime - baseExecTime, 'j': job ]
						}
					}
				}
				println " [XTR5] $candidatedJobs "
				if(candidatedJobs) {
					def clnJob = candidatedJobs.sort{-it.t}.getAt(0).j.clone()
					println " [CLN] ${aClusterInfo.cluster}[${aClusterInfo.num}] CLONEJOB ${clnJob} -- ${new Date()} "
					assiagnCluster( clnJob, aClusterInfo.cluster )
				} else {
					println " [XTR6] $aClusterInfo Mark UnAvailable "
					def dummyJob = new Job( name:'dummyJob', cluster: aClusterInfo.cluster, executionTime:0 )
					advisorInfoService.renewJobMap( dummyJob )
				}
			}
		}
	}


	def assiagnCluster( Job job, Cluster cluster ) {
		job.cluster = cluster
		advisorInfoService.renewJobMap( job )
		def aThread = new Thread() {
			job.execute( disposeHandle, disposeSharedHandle )
		}
		aThread.start()
		def jobObserver = new Thread() {
			int maxValue = -1
			while ( !job.isFinished() ) {
				maxValue = estimateJobExecutionTime( job, maxValue )
			}
			synchronized( clusterIsAvailable ) { clusterIsAvailable = true }
			advisorInfoService.renewJobMap( job )
			println " [FIN] ${job}[${job.cluster}] -- ${ new Date() }"
			estimateCalcTime( job, 50 ).each { e -> println " [FIN] $e" }
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
		} else { job.executionTime = execTimeNow + 1000000 }
		sleep 10; job.save()
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

	def propertyMatchByJobDoneNumber( jobList ) {
		def availableCluster = fetchAvailableCluster()
		if( !availableCluster ) { 
			synchronized( clusterIsAvailable ) { clusterIsAvailable = false }
			return
		}
		def functions = RemoteFunction.list().sort { it.name }
		def funcRank = fetchFuncRankInAvailableCluster( functions, availableCluster )
		println "**** ${advisorInst}"
		funcRank.each { println "${it.cluster} ${it.rank}" }
		for( entry in funcRank ) {
			def rank = new TreeMap()
			entry.rank.each { e ->
				int pos = e.value
				while( rank.containsKey(pos) ) pos--
				rank[ -pos ] = e.key
			}
			println " *SortRank* ${rank} -- ${entry.num} "
			def available = entry.num
			for( func in rank ) {
				def jobs = jobList.findAll { job -> job.function.name == func.value }
				println " ${func} ${jobs} -- ${entry.cluster} "
				if( available && jobs ) {
					def iteration = (jobs.size() < available)? jobs.size() : available
					if( !iteration ) println " **********HANDLEMORE* ${jobs} "
					iteration.times {
						def aJob = jobs.remove(0)
						jobList.remove( aJob )
						println " ${aJob} -- ${jobList} ${entry.cluster} "
						assiagnCluster( aJob, entry.cluster )
					}
					available -= iteration
				}
			}
		}
	}

	def propertyMatchByJobDoneTotalNum( jobList ) {
		// println "**** ${advisorInst}"
		def functions = RemoteFunction.list().sort { it.name }
		def jobDoneTotalRank = [:]
		fetchFuncRankInAvailableCluster(functions,fetchAllCluster()).each { 
			// println "${it.cluster} ${it.rank} ${it.num}"
			it.rank.each { func -> 
				if( !jobDoneTotalRank.containsKey(func.key) ) {
					jobDoneTotalRank[ func.key ] = 0.minus(func.value)
				} else {
					jobDoneTotalRank[ func.key ] -= func.value
				}
			}
		}
		def funcRank = new TreeMap()
		jobDoneTotalRank.each { e ->
			int pos = e.value
			// println " $e VAL: ${funcRank[-pos]} "
			while( funcRank.containsKey(-pos) ) pos--
			funcRank[ -pos ] = e.key
		}
		// println " *SortRank* ${funcRank} "
		funcRank.each { func ->
			def jobs = jobList.findAll { job -> job.function.name == func.value }
			if( jobs ) {
				def clusterEntrys = 
					fetchAvailableCluster().sort { it.cluster.cpuInfo }.reverse()
				for( element in clusterEntrys ) {
					def jobSize = jobs.size()
					def iteration = ( jobSize<element.num )? jobSize : element.num
					iteration.times {
						def aJob = jobs.remove(0)
						if( aJob ) {
							// println " ${aJob} ${element} -- ${jobs} "
							jobList.remove( aJob )
							assiagnCluster( aJob, element.cluster )
						}
					}
				}
			}
		}
	}

	private
	def fetchFuncRankInAvailableCluster( functions, availableCluster ) {
		def rank = []
		def jobListByAd = advisorInfoService.fetchAdvisedJobList()
		availableCluster.sort { it.cluster.cpuInfo }.reverse().each {
			def finJobs = jobListByAd.get( it.cluster.name, [:] ).get( 'finished', [] )
			def funcRank = [ 'cluster':it.cluster, 'num':it.num, 'rank':[:] ]
			functions.each { func ->
				funcRank.rank[ func.name ] = 
					finJobs.findAll { job -> job.function.name == func.name }.size()
			}
			rank << funcRank
		}
		return rank
	}

	private
	def fetchAvailableCluster() {
		def availableCluster = []
		Cluster.list().each { aCluster ->
			def limit = aCluster.calcTotalAvailableNodesNum()
			def runningJobs = advisorInfoService.fetchRunningJobListByCluster( aCluster )
			if( runningJobs.size() < limit ) {
				def entry = [:]
				entry[ 'cluster' ] = aCluster; entry[ 'num' ] = limit-runningJobs.size()
				availableCluster << entry
			}
		}
		return availableCluster
	}

	def getFunctionRankByCluster( Cluster cluster ) {
		def rank = new TreeMap()
		RemoteFunction.list().each { aFunc ->
			estimateCalcTime( aFunc, 50 ).each { e ->
				if( e.key?.name == cluster.name ) {
					long pos = 0
					if( e.value ) {
						def maxValEntry; def maxSize = 0
						e.value.each {
							if( it.value.size() >= maxSize ) {
								maxSize = it.value.size()
								maxValEntry = it
							}
						}
						// println " $aFunc $e *MAX* ${maxValEntry} "
						// pos = - maxValEntry.value.sum() / 
						//				maxValEntry.value.size()
						pos = - maxValEntry.max()
						while( rank.containsKey(pos) ) pos++
					} else {
						long rand = Math.random()*1000
						pos = ( rand % 2 )? -rand : rand
					}
					rank[ pos ] = aFunc.name 
				}
			}
		}
		println " [SLT] ${rank} "
		return rank
	}

	def propertyMatchByJobCalcTime( jobList ) {
		def clusterInfo = fetchAllCluster()
		if( !clusterInfo.find { it.num > 0 } ) {
			synchronized( clusterIsAvailable ) { clusterIsAvailable = false }
			return
		}
		clusterInfo = clusterInfo.sort { it.cluster.cpuInfo }.reverse()
		clusterInfo.each { entry ->
			println " [TBL] ${entry.cluster} -- ${new Date()} "
			entry.handleStatus.each { println " [TBL] ${it} " }
		}
		def jobByFunc = [], calcTimeByFunc = []
		RemoteFunction.list().each { func ->
			def jobs = jobList.findAll { it.function.name == func.name }
			jobByFunc << [ 'f':func.name, 'j': jobs ]
			def calct = estimateCalcTime( func, 50 )
			calcTimeByFunc << [ 'f':func.name, 't': calct ]
		}
		jobList.each { job ->
			
		}
		RemoteFunction.list().each { aFunc ->
			def clusterRank = 
				ranks.findAll { e -> e.func.name == aFunc.name && e.num >0 }
			if( !clusterRank ) { return }
			clusterRank.each { println " [CLR] $it -- ${new Date()} " }
			def jobs = jobList.findAll { job -> job.function.name == aFunc.name }
			clusterRank.sort { it.est }.each {
				if( jobs ) {
					def jobId = Math.random()*1000000 % jobs.size()
					def aJob = jobs.remove(jobId); jobList.remove( aJob )
					println " [SUB] ${aJob} -- ${it.cluster} : ${new Date()} "
					assiagnCluster( aJob, it.cluster )
				}
			}
		}
	}

	def getFunctionRankByCluster( clusterEtr ) {
		def rank = []
		RemoteFunction.list().each { aFunc ->
			estimateCalcTime( aFunc, 50 ).each { e ->
				if( e.key?.name == cluster.name ) {
					def esTime = Math.random()*100000
					if( e.value ) {
						def maxSize = 0
						e.value.each {
							if( it.value.size() >= maxSize ) {
								maxSize = it.value.size(); esTime = it.value.max()
							}
						}
					}
					clusterEtr.handleStatus.each {
						rank << [ 'cluster':clusterEtr.cluster, 'num': clusterEtr.num,
									'func':aFunc, 'est':esTime+it.time ]
					}
				}
			}
		}
		println " [SLT] ${rank} "
		return rank
	}

*/

