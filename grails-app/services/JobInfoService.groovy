
import java.util.concurrent.locks.ReentrantLock as UpdateLock 
import org.ggf.drmaa.SessionFactory;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.JobInfo;

class JobInfoService {

	boolean transactional = true
	long jobId = 0;
	def sessionFactory;

    def final handleLock = new UpdateLock();
    def final cloneLock = new UpdateLock();
    def final argsLock = new UpdateLock();
    def final adJobsLock = new UpdateLock();
    def final finJobsLock = new UpdateLock();
    def final executionInfoLock = new UpdateLock();
    def final testJobsMapLock = new UpdateLock();
    def final funArgsMapLock = new UpdateLock();
	def final unFinishedJobsLock = new UpdateLock();

	def handlesMap = [:];
	def advisedJobsMap = [:];
	def clonedJobsMap = [:];
	def argsMap = [:];
	def finishedJobsMap = [:];
	def testJobsMap = [:];
	def executionInfoMap = [:];
	def funArgsMap = [:];
	def unFinishedJobs = [];

    // def final jobsLock = new UpdateLock();
	def jobsUpdateLocks = [:];

	def helperService() { return ServiceReferenceService.references.helperService; }
	def advisorInfoService() { return ServiceReferenceService.references.advisorInfoService; }
	def executorService() { return ServiceReferenceService.references.executorService; }
	def executionAdviceService() { return ServiceReferenceService.references.executionAdviceService; }
	def ngExecutionInitializeService() { return ServiceReferenceService.references.ngExecutionInitializeService; }
	def sgeExecutionInitializeService() { return ServiceReferenceService.references.sgeExecutionInitializeService; }

	def addNewJob( long clientId, RemoteFunction function, Object... args ) {
		jobId++;
		def aJob = new Job(
				name:function.name+jobId,
				jid:jobId, 
				client:Client.get(clientId),
				function:function,
				executedTimes:0,
				status:'scheduled',
				createdTime:new Date()).save();
		helperService().withLock( argsLock, "addArgs" ) {
			log.debug "$jobId addArgs: $args";
			argsMap[jobId] = args;
		}
		helperService().withLock( finJobsLock, "finishJob" ) {
			finishedJobsMap[jobId] = false;
		}
		println "added NEW job $jobId";
		return jobId;
	}

	def addSGEJob( long clientId, RemoteFunction function, Object... args ) {
		jobId++;
		def aJob = new Job(
				isSGE:true,
				name:function.name+jobId,
				jid:jobId, 
				client:Client.get(clientId),
				function:function,
				executedTimes:0,
				status:'scheduled',
				createdTime:new Date()).save();
		helperService().withLock( argsLock, "addArgs" ) {
			log.debug "$jobId addArgs: $args";
			argsMap[jobId] = args;
		}
		helperService().withLock( finJobsLock, "finishJob" ) {
			finishedJobsMap[jobId] = false;
		}
		println "added SGE job $jobId";
		return jobId;
	}

	def addFunctionOpts( String funcId, opts ) {
		helperService().withLock( funArgsMapLock, "addFunArgs-${funcId}-ToMap" ) {
			funArgsMap[funcId] = opts;
			println "add opts for func: $funcId, ${funArgsMap[funcId]}";
		}
	}

	def getFunctionOpts( String funcId ) {
		def results = [];
		helperService().withLock( funArgsMapLock, "getFunArgs-${funcId}-FromMap" ) {
			results = funArgsMap[funcId];
		}
		println "get opts for func: $funcId, ${results}";
		return results;
	}

	def addNewTestJob( long clientId, RemoteFunction function, Object... args ) {
		def jobId = addNewJob( clientId, function, args );
		helperService().withLock( testJobsMapLock, "addTestJob-${jobId}-ToMap" ) {
			if( !testJobsMap[function.name] ) testJobsMap[function.name] =[];
			testJobsMap[function.name] << jobId;
		}
		def message = "test job with id [$jobId] for $function is created.";
		message += helperService().printHelper(testJobsMap[function.name],10,"\n\t" );
		log.info message;
		return jobId;
	}

	def addNewSGETestJob( long clientId, RemoteFunction function, Object... args ) {
		def jobId = addSGEJob( clientId, function, args );
		helperService().withLock( testJobsMapLock, "addSGETestJob-${jobId}-ToMap" ) {
			if( !testJobsMap[function.name] ) testJobsMap[function.name] =[];
			testJobsMap[function.name] << jobId;
		}
		def message = "test job with id [$jobId] for $function is created.";
		message += helperService().printHelper(testJobsMap[function.name],10,"\n\t" );
		log.info message;
		return jobId;
	}

	def addNewJobWithCluster( long clientId,
			RemoteFunction function, long clusterId, Object... args ) {
		def jobId = addNewJob( clientId, function, args );
		changeJobCluster( jobId, clusterId );
		return jobId;
	}

	def isTestJobsForFunctionsEnough() {
		def result = true;
		def allFuncs = RemoteFunction.list().findAll{!it.isTest}.collect{ it.name };
		def testedFuncs = testJobsMap.keySet();
		def leftFuncs = allFuncs - testedFuncs;
		if(leftFuncs.size() != 0) {
			log.error "no test job for functions:$leftFuncs have been executed.";
			result = false;
		}
		return result;
	}

	def caculateExecutionCost( long jobId ) {
		while( !jobIsFinished(jobId) ) { sleep 10; }
		def job = getCurrentJob( jobId );
		def clusterName = job.cluster.name;
		def startTime = job.executionStartTime?.time;
		def endTime = job.executionFinishTime?.time;
		def execTime = -1;
		if( startTime && endTime ) {
			execTime = (endTime-startTime)/1000;
			def message = "store test job infomation ${jobId} --> ";
			message += "\n ${job.function.name}, $clusterName, $execTime";
			log.debug message;
		} else {
			log.error "job ${jobId} is not finished. wrong mark!"
		}
		return [ clusterName:clusterName, time:execTime ];
	}

	def getJobExecutionCosts() {
		isTestJobsForFunctionsEnough();
		def allClusters = Cluster.list().collect{ it.name };
		def testResult = [:];
		log.debug "test results: ${testJobsMap}";
		testJobsMap.keySet().each { functionName ->
			def jobIds = testJobsMap[functionName];
			def testedClusters = [];
			jobIds.each { jobId ->
				def res = caculateExecutionCost(jobId);
				if( !testResult[functionName] ) testResult[functionName] = [:];
				if( !testResult[functionName]["${res.clusterName}"] ) 
					testResult[functionName]["${res.clusterName}"] = [];
				testResult[functionName]["${res.clusterName}"] += [res.time];
				testedClusters << res.clusterName;
			}
			log.debug "tested cluster $testedClusters";
			def leftClusters = allClusters - testedClusters;
			if( leftClusters.size() != 0) {
				log.info "no test job[$functionName] for cluster:$leftClusters.";
			}
			def randomClusterId = (int)(Math.random()*100)%testedClusters.size();
			def selectedCluster = testedClusters[randomClusterId];
			def testedOne = Cluster.findByName(selectedCluster);
			leftClusters.each { clusterName ->
				def untestedOne = Cluster.findByName(clusterName);
				def estTime = testedOne.cpuInfo * theTime / untestedOne.cpuInfo;
				if( !testResult[functionName]["${clusterName}"] ) 
					testResult[functionName]["${clusterName}"] = [];
				testResult[functionName]["${clusterName}"] += [estTime];
			}
		}
		log.debug "testResult ${testResult}";
		return testResult;
	}

	def getUnfinishedJobs( inList=[] ) {
		def finishedJobs = getFinishedJobs( inList );
		def result = finishedJobsMap.keySet() - finishedJobs;
		log.debug "jobs unfinished: ${result}";
		return result;
	}

	def getFinishedJobs( inList=[] ) {
		def result = [];
		helperService().withLock( finJobsLock, "getfinishedJobs" ) {
			def jobIds = (inList)? inList : finishedJobsMap.keySet();
			jobIds.each { if( finishedJobsMap[it] ) result << it; }
		}
		log.debug "jobs finished: ${result}";
		return result;
	}

	def getTestJobs( String functionName="" ) {
		def result = [];
		def logMsg = "getTestJobs from $testJobsMap: ";
		helperService().withLock( testJobsMapLock, "getTestJobs" ) {
			if(functionName) {
				result += testJobsMap[functionName];
				logMsg += "\n\ttestJobs: $result";
			} else {
				testJobsMap.keySet().each { func ->
					result += testJobsMap[func];
					logMsg += "\n\ttestJobs: $result";
				}
			}
		}
		log.debug logMsg;
		return result;
	}

	def getFinishedTestJobs( String functionName="" ) {
		def testJobs = getTestJobs( functionName );
		def finTests = getFinishedJobs( testJobs );
		log.debug "\ttestJobs:$testJobs, \n\tfinTests:$finTests";
		return finTests;
	}

	def getJobsNeedAdvice( String AdvisorName ) {
		def results = [];
		results = getUnfinishedJobs().findAll { jobId ->
			log.debug "\t$jobId hasHandle: ${hasHandle(jobId)} ";
			log.debug "\t$jobId ${getAdvisorName(jobId)} ";
			!hasHandle(jobId) && (getAdvisorName(jobId) == AdvisorName)
		}
		log.debug "jobs needed to server by $AdvisorName advisor: ${results}";
		return results.asList();
	}

	def getQueuingJobsInCluster( clusterId, boolean isSGE=false ) {
		def futureJobs = getUnfinishedJobs();
		def futureJobsWithoutHandles = futureJobs.findAll { !hasHandle( it ) }
		def result = futureJobsWithoutHandles.findAll { getClusterId( it ) == clusterId }
		// println "jobs queuing in cluster[$clusterId] are $result";
		if( !isSGE ) { return result; }
		def _result;
		helperService().withLock( unFinishedJobsLock, "setQueuingJobs" ) {
			_result = futureJobs - unFinishedJobs;
			if( unFinishedJobs ) { 
				unFinishedJobs.each { jobId ->
					def job = getCurrentJob( jobId ); def sgeId = job.sgeId;
					if( sgeId.toInteger() > 0 ) {
						println "sge queuing job$jobId -- sge$sgeId";
						if(sgeExecutionInitializeService().getJobExecutionStatus(sgeId)>10) {
							println "sge queuing job$jobId << sge$sgeId";
							_result << jobId;
						}
					}
				}
			}
			if( unFinishedJobs.size() == 0 ) {
				unFinishedJobs = futureJobs;
			} else {
				unFinishedJobs += _result;
			}
		}
		return _result;
	}

	def getClusterName( long jobId ) {
		def job = getCurrentJob( jobId );
		return job.cluster.name;
	}

	def getClusterId( long jobId ) {
		def job = getCurrentJob( jobId );
		// TODO:check why job has no cluster
		return (job.cluster)? job.cluster.id:1;
	}

	def changeJobHandle( long jobId, handle ) {
		def job = getCurrentJob( jobId );
		if( !handle ) {
			log.error " $job null Handle ";
			// executionAdviceService().addAdviceForExecutionFault(jobId,"changeJobHandle[$jobId]");
			return;
		}
		helperService().withLock( handleLock, "changeJobHandle" ) {
			if( handlesMap[ jobId ] ) {
				def oldhandle = handlesMap[ jobId ];
				log.debug " $job [oldhdl:${oldhandle?.hashCode()}] " + 
					"--> [newhdl:${handle?.hashCode()}]"
			}
			handlesMap[ jobId ] = handle;
			log.debug " $job [newhdl:${handle?.hashCode()}]";
			log.debug " $job with handleMap ${handlesMap}";
		}
		return jobId;
	}

	def getHandle( long jobId ) {
		def handle;
		helperService().withLock( handleLock, "getHandle" ) {
			handle = handlesMap[ jobId ];
			if( handle ) log.debug " $jobId [gethdl:${handle.hashCode()}]"
		}
		return handle;
	}

	def removeHandle( long jobId ) {
		def handle = null;
		helperService().withLock( handleLock, "removeHandle!" ) {
			handle = handlesMap[ jobId ];
			handlesMap[ jobId ] = null;
		}
		println "remove grpc handle for job:$jobId ";
		return handle
	}

	def hasHandle( long jobId ) {
		def handle = null;
		helperService().withLock( handleLock, "hasHandle?" ) { handle = handlesMap[ jobId ]; }
		def job = getCurrentJob( jobId )
			return handle && job.status == "executing"
	}

	def getFunctionName( long jobId ) {
		def job = getCurrentJob( jobId );
		return job.function.name
	}

	def getAdvisorName( long jobId ) {
		def job = getCurrentJob( jobId );
		return job.function.advisor.name
	}

	def getClonedJobs( long jobId ) {
		def jobIds;
		helperService().withLock( cloneLock, "getClonedJobs" ) {
			jobIds = clonedJobsMap[ jobId ]
		}
		return jobIds
	}

	def storeJobExecutionInfo( long jobId, executionInfo ) {
		helperService().withLock( executionInfoLock, "storeJobExecutionInfo" ) {
			executionInfoMap[ jobId ] = executionInfo;
		}
	}

	def getJobExecutionInfo( long jobId ) {
		def info = null;
		helperService().withLock( executionInfoLock, "storeJobExecutionInfo" ) {
			info = executionInfoMap[ jobId ];
		}
		return info;
	}

	def finishJob( long jobId ) {
		def job = getCurrentJob( jobId );
		def params = [:];
		def execFT = new Date();
		params["executedTimes"] = job.executedTimes + 1;
		params["executionFinishTime"] = execFT;
		params["status"] = "finished";
		if(job.jid<0) params["jid"] = -job.jid;
		executionAdviceService().setAdviseStatus(jobId, ADVISE_STATUS.DISCARDED);
		params["executionTime"] = 
			( execFT.getTime() - job.createdTime.getTime() ) / 1000;
		updateJob( jobId, params );
		if( jobHasError( jobId ) ) { log.debug "${job} is failed. need re-invoke "; return; }
		if( job.status == "canceled" ) { log.debug "${job} is canceled "; return; }
		if( job.isSGE ) {
			// params["details"] = getJobExecutionInfo(jobId);
		}else {
			def details = new ExecutionInfo( job:job, cluster:job.cluster ).save();
			details.parseExecutionInfo( getJobExecutionInfo(jobId) );
			params["details"] = details;
		}
		updateJob( jobId, params );
		log.debug "${job} is finished ";
		advisorInfoService().renewJobMap( jobId );
		markJobIsFinished( jobId );
		getClonedJobs( jobId ).each { ajobId -> cancelJob( ajobId ); }
	}

	def boolean jobIsFinished( long jobId ) {
		def result = false;
		helperService().withLock( finJobsLock, "jobIsFinished" ) {
			result = finishedJobsMap[jobId];
		}
		return result;
	}

	def makeCloneJob( long jobId, params = null ) {
		def njobId;
		helperService().withLock( cloneLock, "makeCloneJob" ) {
			njobId = _makeCloneJob( jobId, params );
			if( !clonedJobsMap[ jobId ] ) {
				clonedJobsMap[ jobId ] = []
			}
			clonedJobsMap[ jobId ] << njobId;
		}
		return njobId
	}

	def private _makeCloneJob( long jobId, params = null ) {
		def njob = new Job();
		def job = getCurrentJob( jobId );
		njob.isClone = true;
		njob.isSGE = job.isSGE;
		njob.status = "cloned";
		njob.function = job.function;
		njob.client = job.client;
		njob.name = job.name + "C";
		njob.createdTime = new Date();
		njob.jid = -job.jid;
		njob.coreGroup = (params)? params.coreGroup : job.coreGroup ;
		njob.save(flush:true);
		return njob.id;
	}

	def getArgs( long jobId ) {
		def args = [[],[]];
		println "getArgs: for $jobId";
		def job = getCurrentJob( jobId );
		helperService().withLock( argsLock, "getArgs" ) {
			args = Arrays.asList(argsMap[jobId]);
			log.debug "$job useArgs: $args ";
		}
		return args;
	}

	def boolean jobNeedsHandle( long jobId ) {
		def job = getCurrentJob( jobId );
		return job.needHandle();
	}

	def executeJob( long jobId ) {
		// TODO: lock with jobId
		// def jobsLock = getJobsUpdateLock(jobId);
		// helperService().withLock( jobsLock, "executeJob" ) {
		def job = getCurrentJob( jobId ), _jobHasError = false;
		// checkJobExecutionTime( jobId );
		Thread.start {
			try {
				// while ( !job.cluster ) { job.refresh(); sleep 100; }
				println "sge job$jobId start execution1. ${new Date()}";
				job = updateJob( jobId, [status:"waitingHandle"] );
				def handle = addJobHandle( jobId );
				println "sge job$jobId start execution2. ${new Date()}";
				_execute( jobId, handle );
				// ( job.isSGE )? sgeExecutionInitializeService().execute( jobId, handle ) :
				//	ngExecutionInitializeService().grpcExecute( jobId, handle );
			} catch( Exception e ) {
				_jobHasError = true;
				executionAdviceService().addAdviceForExecutionFault(jobId);
				updateJob( jobId, [status:"failed"] );
				log.error("SGE $job Call failed $e");
			} finally {
				try {
					(_jobHasError)? cancelJob( jobId ) : finishJob( jobId );
				} catch ( Exception e ) {
					updateJob( jobId, [status:"failed"] );
					log.error("${job} execution failed $e");
				}
			}
			// }
		}
	}

	def _execute( long jobId, jt ) {
		println "sge job$jobId start execution3. ${new Date()}";
		try {
			def session = sgeExecutionInitializeService().getSGESession();
			def arguments = getArgs( jobId );
			println "sge job$jobId start execution4. ${new Date()}";
			def id = session.runJob(jt);
			updateJob( jobId, [ sgeId:id ] );
			def job_status = session.getJobProgramStatus(id);
			while( job_status != Session.RUNNING ) {
				job_status = session.getJobProgramStatus(id);
				if( job_status == Session.FAILED ) { throw new Exception("sge job$id failed."); }
				if( job_status == Session.DONE || job_status == Session.RUNNING ) break;
				Thread.sleep(100);
			}
			println "sge job$jobId start execution5. ${new Date()}";
			def execST = new Date();
			def job = getCurrentJob( jobId );
			updateJob( jobId, [ status:"executing", executionStartTime:execST,
					queuingTime:(execST.time-job.createdTime.time)/1000 ] );
			println "sge job$jobId start execution6. ${new Date()}";
			def info = session.wait(id, Session.TIMEOUT_WAIT_FOREVER);
			if (info.wasAborted()) {
				throw new Exception("sge job$id failed. Job never ran.");
			} else if (info.hasExited()) {
				println("Job ${info.jobId} finished regularly with exit status ${info.exitStatus}");
			} else if (info.hasSignaled()) {
				if (info.hasCoreDump()) { println("A core dump is available.") }
				throw new Exception("sge job$id finished due to signal ${info.terminatingSignal}.");
			} else {
				throw new Exception("sge job${info.jobId} finished with unclear conditions");
			}
			def rmap = info.resourceUsage;
			def _end_time = (rmap['end_time'])? Double.parseDouble(rmap['end_time']) : new Date().time;
			def _start_time = (rmap['start_time'])? Double.parseDouble(rmap['start_time']) : execST.time;
			def execution_time = (_end_time - _start_time).toLong();
			session.deleteJobTemplate(jt);
			storeJobExecutionInfo( jobId, rmap );
		} catch ( Exception e ) { 
			e.printStackTrace();
			// executionAdviceService().addAdviceForExecutionFault(jobId); 
		}
	}

	def getJobFinishTime( long jobId ) {
		def testJobs = getFinishedTestJobs( getFunctionName(jobId) );
		if( !testJobs ) { log.debug "no test jobs for ${jobId}."; return -1; }
		def costs = getJobExecutionCosts(), funcname = getFunctionName(jobId);
		def sum = 0, num =0, clusterName = getClusterName(jobId);
		costs[funcname].each {
			log.debug "$it, ${it.key}, ${it.value}";
			if(it.key == clusterName){ sum = it.value.sum(); num = it.value.size(); }
		}
		if( num == 0 ) num++;
		log.debug "${funcname} in $clusterName: ${sum} ${num} ${sum/num}";
		return (sum/num)*1.2
	}

	def checkJobExecutionTime( long jobId ) {
		executorService().invokeWithFixedDelay( 3 ) {
			long jobFinishEstimationTime = getJobFinishTime( jobId ).longValue();
			if( jobFinishEstimationTime <= 0 ) return;
			def msg = "\nStart job[${jobId}] execution check: loop[${jobFinishEstimationTime}s]";
			def job = getCurrentJob( jobId );
			def executionTimeNow = job.executionTimeNow();
			// println "job:$jobId ${job.cluster.name}
			// now:$executionTimeNow, estimate:$jobFinishEstimationTime";
			if( executionTimeNow >= jobFinishEstimationTime + 10 )
				executionAdviceService().addAdviceForClusterPerformanceDown( jobId );
			def jobFin = jobIsFinished( jobId );
			if( jobFin ) {
				msg += "\njob[${jobId}] is finished within: ${jobFinishEstimationTime}s";
				log.debug msg;
				executionAdviceService().deleteAdviceForTimeOver(jobId);
				return "cancel check execution run for job:$jobId ";
			}
		}
	}

	def getCurrentSGEJob( long jobId ) {
		def job = Job.get( jobId );
		def preVersion = job.version;
		job.refresh();
		def postVersion = job.version;
		if( preVersion != postVersion ) {
			log.info "Job:$jobId[version:$preVersion -> $postVersion]" + 
				" was updated by the other database session(s).";
		}
		return job;
	}

	def getCurrentJob( long jobId ) {
		def job = Job.get( jobId );
		def preVersion = job.version;
		job.refresh();
		def postVersion = job.version;
		if( preVersion != postVersion ) {
			log.info "Job:$jobId[version:$preVersion -> $postVersion]" + 
				" was updated by the other database session(s).";
		}
		return job;
	}

	def getJobsUpdateLock( long jobId ) {
		def result = jobsUpdateLocks[jobId];
		if( result == null ) { result = new UpdateLock(); jobsUpdateLocks[jobId] = result; }
		println "sge: $jobId --> $result ${new Date()}";
		result;
	}

	def updateJob( long aJobId, params ) {
		// def jobsLock = getJobsUpdateLock(aJobId);
		// helperService().withLock( jobsLock, "updateJob" ) {
		helperService().withSession( "job$jobId" ) {
			def job = getCurrentJob( aJobId );
			def logMsg = "\n+++++++ $aJobId ++++++\n";
			try {
				params.each { key, val ->
					logMsg += "\tjob$aJobId update ${key} --> ${val}  ${new Date()}\n";
					job."$key" = val;
				}
				job.save(flush:true);
				log.info logMsg + "------- $aJobId ------ ";
				return job;
			} catch(Exception e) { log.error("update ${job} err:",e); }
		}
	}

	private def addJobHandle( long jobId ) {
		def tryTimes = 0;
		def job = getCurrentJob( jobId );
		log.debug " ${job} addHandle TRY...";
		def ahandle;
		while( !hasHandle(jobId) ) {
			try {
				ahandle = job.getHandle();
				changeJobHandle(jobId, ahandle);
				break;
			} catch ( Exception e ) {
				log.error("${job} *GETHANDLE*",e);
				// executionAdviceService().addAdviceForExecutionFault(jobId,"addJobHandle");
			}
		}
		return ahandle;
	}

	def markJobIsFinished( long jobId ) {
		helperService().withLock( finJobsLock, "finishJob" ) {
			finishedJobsMap[jobId] = true;
		}
	}

	def markJobIsAdvised( long jobId ) {
		helperService().withLock( adJobsLock, "AdviceJob" ) {
			advisedJobsMap[jobId] = true;
		}
	}

	def unmarkAdvisedJob( long jobId ) {
		helperService().withLock( adJobsLock, "UnAdviceJob" ) {
			advisedJobsMap[jobId] = false;
		}
	}

	def jobIsAdvised( long jobId ) {
		def result = false;
		helperService().withLock( adJobsLock, "AdviceJob" ) {
			result = advisedJobsMap[jobId];
		}
		return result;
	}

	def jobHasError( long jobId ) {
		def job = getCurrentJob( jobId );
		return ( job.status == "failed"  || job.status == "canceled" )
	}

	def changeJobCluster( long jobId, long clusterId ) {
		def cluster = Cluster.get( clusterId );
		def job = getCurrentJob( jobId );
		if( !jobIsFinished( jobId ) ) {
			log.info "${jobId} changeJobCluster ${job.cluster} --> ${cluster} ";
		}
		job = updateJob( jobId, 
				[ cluster:cluster, jid:job.jid, name:job.name, status:"clusterChanged" ]);
	}

	def cancelJob( long jobId ) {
		try {
			def job = getCurrentJob( jobId );
			def handle = removeHandle( jobId );
			if(job.status == "canceled" || job.status == "finished") { return }
			def params = [:];
			def execFT = new Date();
			def theTime = ( job.executionStartTime )? job.executionStartTime : new Date();
			if( job.jid > 0 ) params["jid"] = -job.jid;
			params["executedTimes"] = job.executedTimes + 1;
			params["executionFinishTime"] = null;
			params["executionTime"] = -1.0;
			params["queuingTime"] = -1.0;
			params["status"] = "canceled";
			//TODO: executionAdviceService().setAdviseStatus(jobId, ADVISE_STATUS.DISCARDED);
			updateJob( jobId, params );
			log.info "Job[:$jobId] is cancelled. finished? ${jobIsFinished(jobId)}";
			Thread.start { if(!job.isSGE) { handle?.cancel(); } }
			// log.info "Job[:$jobId] is cancelled."
		} catch (Exception e) { 
			println "cancel job:$jobId error: $e";
		}
	}

	}
