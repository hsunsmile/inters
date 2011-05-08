import java.util.concurrent.locks.ReentrantLock as UpdateLock 

class ExecutionAdviceService {

	boolean transactional = true;
    def final adviseMapLock = new UpdateLock();
    def final advisePlanMapLock = new UpdateLock();
	def final adviseLock = new UpdateLock();

	def adviseMap = [:];
	def advisePlanMap = [:];

	def planService() { ServiceReferenceService.references.planService; }
	def jobInfoService() { ServiceReferenceService.references.jobInfoService; }
	def helperService() { ServiceReferenceService.references.helperService; }
	def awsService() { ServiceReferenceService.references.awsService; }

	def makeAdvice( long jobId, long clusterId, String reason, String method ) {
		executorService.invokeWithSession("[makeAdvice|$jobId|$reason]") {
			def advise = new Advise( reason:reason, method:method );
			// advise.method = "Failed $failedTimes times";
			advise.addToJobIds( jobId );
			advise.addToToClusterIds( clusterId );
			advise.save();
			while( !adviseIsSelected( advise.id ) ) { sleep 1000 }
		}
	}

	def relateAdviceAndPlan( long adviseId, planIds ) {
		helperService().withLock( advisePlanMapLock, 
                "relate Advise[:${adviseId}] <--> Plans:[${planIds}]" ) {
			def ids = advisePlanMap[adviseId];
			if(ids) planService().deletePlan( ids );
			advisePlanMap[adviseId] = planIds;
		}
	}

	def getPlanIdsForAdvise( adviseId ) {
		if( adviseId instanceof String ) adviseId = adviseId.toLong();
		def result;
		helperService().withLock( advisePlanMapLock, "get Advise[:${adviseId}] Plans" ) {
			result = advisePlanMap[adviseId]
		}
		result
	}

	def getCurrentAdvise( long adviseId ) {
		def advise = Advise.get( adviseId );
		def preVersion = advise.version;
		advise.refresh();
		def postVersion = advise.version;
		if( preVersion != postVersion ) {
			log.info "Advise:$adviseId[version:$preVersion -> $postVersion]" + 
				" was updated by the other database session(s).";
		}
		return advise;
	}

	def updateAdvise( long adviseId, params ) {
		helperService().withLock( adviseLock, "updateAdvise" ) {
			def advise = getCurrentAdvise( adviseId );
			def threadId = Thread.currentThread().name;
			helperService().pushNDC("[$threadId][updateAdvise:$adviseId:$advise.version]");
			def logMsg = "\n+++++++ " + advise +" ++++++\n";
			try{
				params.each { key, val ->
					logMsg += "\tadvise[$adviseId].update ${key} --> ${val}\n";
					advise."$key" = val;
				}
				advise.save(flush:true);
                advise.createAdviceFile();
				log.debug logMsg + "------- ${advise} ------ ";
				helperService().popNDC();
				return advise;
			}catch(Exception e) {
				log.error("update ${advise} err: $e", e);
			}
		}
	}

	def getJobIdsAdvisedBy( long adviseId ) {
		def advise = getCurrentAdvise( adviseId );
		return advise.jobIds;
    }

	def deleteAdviceForTimeOver( long jobId, String reason="" ) {
    jobInfoService().unmarkAdvisedJob( jobId );
		log.info "delete advice for timeover job:${jobId}";
		if (!reason) reason = "Job[:${jobId}] execution finished.";
		helperService().withLock( adviseLock, "DeleteAdviceForExecutionTimeExeceed" ) {
			def job = Job.get( jobId );
			def logMsg = "DelAdviceForExecutionTimeExeceed[job:$jobId][reason:$reason]";
			Advise.list().each { advise ->
				def nAdvise = getCurrentAdvise( advise.id );
				if( nAdvise.type == ADVISE_TYPE.CLUSTER_PERFORMANCE_DOWN && 
						nAdvise.clusterId == job.cluster.id ) {
					def hasJob = nAdvise.jobIds.find { it == jobId };
					def jobList = nAdvise.jobIds - jobId;
					if( jobList ) {
						updateAdvise( advise.id, [ jobIds:jobList ] );
					} else {
						def sucess = nAdvise.delete();
						logMsg += "\n\tdelete[$sucess] advise for job[$jobId] --> ad:$nAdvise";
					}
				}
			}
			log.info logMsg;
		}
	}

	def makeJobMigrationAdvise( long jobId, long destClusterId ) {
    if( jobInfoService().jobIsAdvised(jobId) ) return;
		log.info "add advice for timeover job:${jobId}";
		if (!reason) reason = "Job[:${jobId}] Execution Time Execeeded.";
		helperService().withLock( adviseLock, "AdviceForExecutionTimeExeceed" ) {
			def job = Job.get( jobId );
			def logMsg = "addAdviceForExecutionTimeExeceed[job:$jobId][reason:$reason] ";
			log.info logMsg;
			def advises = [];
			Advise.list().each { advise ->
				def nAdvise = getCurrentAdvise( advise.id );
				if( nAdvise.type == ADVISE_TYPE.USER_CHANGE_CLUSTER && 
						nAdvise.clusterId == destClusterId ) advises << advise.id;
			}
			logMsg += "\n\tadvises for job[$jobId] --> $advises";
			if(!advises) {
				def advise = new Advise();
				advise.type = ADVISE_TYPE.USER_CHANGE_CLUSTER;
				advise.advisorName = jobInfoService().getAdvisorName(jobId);
				advise.planFor = "jobMigration";
				advise.clusterId = destClusterId;
				advise.addToJobIds( jobId );
				advise.save();
                advise.createAdviceFile();
				addJobAdvise(jobId,advise.id);
				log.info "add advise[$advise] for job[$jobId] \n" + logMsg;
			} else {
        advises.each { adviseId ->
          def adviseJobIds = getJobIdsAdvisedBy( adviseId );
          def hasTheJob = adviseJobIds.find { it == jobId };
          if(!hasTheJob) {
            def jobIds = ( adviseJobIds )? adviseJobIds << jobId : [ jobId ];
            updateAdvise( adviseId, [ jobIds:jobIds ] );
            logMsg += "\n\t${getJobIdsAdvisedBy(adviseId)}";
          }
        }
        // useClusterPerformanceDownAdvices( advises );
      }
      jobInfoService().markJobIsAdvised( jobId );
      log.info logMsg;
    }
  }

  def addAdviceForClusterPerformanceDown( long jobId, String reason="" ) {
    log.info "add advice for timeover job:${jobId}";
    if (!reason) reason = "Job[:${jobId}] Execution Time Execeeded.";
    helperService().withLock( adviseLock, "AdviceForExecutionTimeExeceed" ) {
      def job = Job.get( jobId );
      def logMsg = "addAdviceForExecutionTimeExeceed[job:$jobId][reason:$reason] ";
      log.info logMsg;
      def advises = [];
      Advise.list().each { advise ->
        def nAdvise = getCurrentAdvise( advise.id );
        if( nAdvise.type == ADVISE_TYPE.CLUSTER_PERFORMANCE_DOWN && 
            nAdvise.clusterId == job.cluster.id ) advises << advise.id;
      }
      logMsg += "\n\tadvises for job[$jobId] --> $advises";
      if(!advises) {
        def advise = new Advise();
        advise.type = ADVISE_TYPE.CLUSTER_PERFORMANCE_DOWN;
        advise.clusterId = job.cluster.id;
		advise.advisorName = jobInfoService().getAdvisorName(jobId);
		advise.planFor = "clusterPerfornamceDown";
        advise.addToJobIds( jobId );
        advise.save();
        advise.createAdviceFile();
        addJobAdvise(jobId,advise.id);
        log.info "add advise[$advise] for job[$jobId] \n" + logMsg;
      } else {
        advises.each { adviseId ->
          def adviseJobIds = getJobIdsAdvisedBy( adviseId );
          def jobIds = ( adviseJobIds )? adviseJobIds << jobId : [ jobId ];
          updateAdvise( adviseId, [ jobIds:jobIds ] );
          logMsg += "\n\t${getJobIdsAdvisedBy(adviseId)}";
        }
        // useClusterPerformanceDownAdvices( advises );
      }
      log.info logMsg;
    }
  }

  def addAdviceForFutureJobsInPerformanceDownCluster( clusterId ) {
    def futureJobs = jobInfoService().getQueuingJobsInCluster( clusterId );
  }

  def addAdviceForExecutionFault( long jobId, String reason="" ) {
    if (!reason) reason = "Job[:${jobId}] Execution Fault";
    helperService().withLock( adviseLock, "AdviceForExecutionFault" ) {
      def job = Job.get( jobId ), logMsg = "addAdviceForExecutionFault[job:$jobId][reason:$reason] ";
      log.info logMsg;
      def advises = [];
      Advise.list().each { advise ->
        def nAdvise = getCurrentAdvise( advise.id );
        if( nAdvise.type == ADVISE_TYPE.EXECUTION_FAULT && 
            nAdvise.clusterId == job.cluster.id ) advises << advise.id;
      }
      logMsg += "\n\tadvises for job[$jobId] --> $advises";
      if(!advises) {
        def advise = new Advise();
        advise.type = ADVISE_TYPE.EXECUTION_FAULT;	
		advise.advisorName = jobInfoService().getAdvisorName(jobId);
		advise.planFor = "executionFault";
        advise.clusterId = job.cluster.id;
        advise.addToJobIds( jobId );
        advise.save();
        advise.createAdviceFile();
        addJobAdvise(jobId,advise.id);
        log.info "add advise[$advise] for job[$jobId] \n" + logMsg;
      } else {
        advises.each { adviseId ->
          def adviseJobIds = getJobIdsAdvisedBy( adviseId );
          def jobIds = ( adviseJobIds )? adviseJobIds << jobId : [ jobId ];
          updateAdvise( adviseId, [ jobIds:jobIds ] );
          logMsg += "\n\t${getJobIdsAdvisedBy(adviseId)}";
        }
        // useExecutionFaultAdvices( advises );
      }
      log.info logMsg;
    }
  }

  def addJobAdvise( long jobId, long adviseId ) {
    helperService().withLock( adviseMapLock, "addAdvise" ) {
      if(!adviseMap[jobId]) adviseMap[jobId] = [];
      adviseMap[jobId] << adviseId;
      log.info "advises for job[$jobId] are => [${adviseMap[jobId]}]";
    }
  }

  def setAdviseStatus( long jobId, ADVISE_STATUS status ) {
    // helperService().withLock( adviseLock, "AdviceForExecutionFault" ) { }
    helperService().withLock( adviseMapLock, "setAdviseStatus[:$status]" ) {
      if( !adviseMap[jobId] ) { 
        log.info "no advise for job[$jobId] v:[${adviseMap[jobId]}]"; 
        return; 
      }
      adviseMap[jobId].each { adviseId -> 
        def adv = getCurrentAdvise(adviseId);
        def jobsLeft = adv.jobIds - jobId
          if( jobsLeft ) updateAdvise( adviseId, [ status:status ] );
      }
    }
  }

  def getAdvise( adviseIds ) { adviseIds.collect { Advise.get(it) } }

  def useAdvices( adviseIdList ) {
    useExecutionFaultAdvices( adviseIdList );
    useClusterPerformanceDownAdvices( adviseIdList );
  }

  def useExecutionFaultAdvices( adviseIdList ) {
    def adviseList = (adviseIdList)? getAdvise(adviseIdList.unique()):Advise.list();
    def advises = adviseList.findAll{ it.type == ADVISE_TYPE.EXECUTION_FAULT }
    log.info "[ExecutionFault] advise => $advises]";
    advises.each { advise -> 
      def planIds =  planService().makePlanForFaultTolerantAdvice( advise.id ); 
      relateAdviceAndPlan( advise.id, planIds );
      log.info "[ExecutionFault] advise[:$advise.id] => plan[:$planIds] are made for execution";
    }
    executeAdviseWithPlans( adviseIdList );
  }

  def makeClusterPerformanceDownAdvices( adviseIdList ) {
    def adviseList = (adviseIdList)? getAdvise(adviseIdList.unique()):Advise.list();
    def advises = adviseList.findAll{ it.type == ADVISE_TYPE.CLUSTER_PERFORMANCE_DOWN }
    advises.each { advise -> 
      def planIds =  planService().makePlanForClusterPerformanceDownAdvice( advise.id ); 
      relateAdviceAndPlan( advise.id, planIds );
      log.info "advise[:$advise.id] => plan[:$planIds] are made for execution";
    }
    // executeAdviseWithPlans( adviseIdList );
  }

  def useClusterPerformanceDownAdvices( adviseIdList, boolean migrate = true ) {
    def adviseList = (adviseIdList)? getAdvise(adviseIdList.unique()):Advise.list();
    def advises = adviseList.findAll{ it.type == ADVISE_TYPE.CLUSTER_PERFORMANCE_DOWN }
    log.info "[PerformanceDown] advise => $advises]";
    advises.each { advise -> 
      def planIds = planService().makePlanForClusterPerformanceDownAdvice( advise.id ); 
      relateAdviceAndPlan( advise.id, planIds );
      log.info "[PerformanceDown:$migrate] advise[:$advise.id] => plan[:$planIds] are made for execution";
    }
    // executeAdviseWithPlans( adviseIdList, migrate );
  }

  def executeAdviseWithPlans( adviseIdList, boolean migrate = false ) {
    adviseIdList.each { adviseId ->
      def ids = getPlanIdsForAdvise( adviseId );
      log.info "[migrate:$migrate] execute advises:$adviseIdList, with plans:$ids";
      ids.each { planId -> planService().executePlanById( planId, migrate ); }
    }
  }

  def adviseIsSelected( long adviseId ) {
    def advise = Advise.get( adviseId );
    return advise.isSelected && advise.isAvailable;
  }
}
