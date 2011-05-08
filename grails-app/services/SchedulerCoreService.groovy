
class SchedulerCoreService {

	boolean transactional = true;

	def advisorList = [];
	static def removedAdvisorList = [];

	def functionIsTested = [:];
	def tmpJobQueue = [];
	def origAdvisorStore = [:];

	def testJobManagerIsStarted = false;
	def testJobFinished = false;
	def testJobNumLimit = 1;

	def executorService() { return ServiceReferenceService.references.executorService; }
	def advisorInfoService() { return ServiceReferenceService.references.advisorInfoService; }
	def jobInfoService() { return ServiceReferenceService.references.jobInfoService; }
	def helperService() { return ServiceReferenceService.references.helperService; }
	def planService() { return ServiceReferenceService.references.planService; }
	def executionAdviceService() { return ServiceReferenceService.references.executionAdviceService; }

	Cluster[] getClusters() {
	    Cluster.list() as Cluster[];
	}

	def pause() {
		advisorInfoService().stop();
		executorService().stop();
		advisorList.each { removeAdvisor(it.advisor); }
		advisorList = [];
	}

	def resume() {
		advisorList = [];
		advisorInfoService().restart();
		executorService().resume();
		removedAdvisorList.each { 
			log.info "add..Advisor[$it]"; 
			addAdvisor(it); 
		}
	}

	def reschedule() {
		pause();
		resume();
	}

	def startSchedule( boolean markScheduleStart=true ) {
		if( !markScheduleStart ) { return }
		advisorInfoService().setJobSubmitionFinished( true );
	}

	def stopSchedule( boolean markScheduleStart=true ) {
		if( !markScheduleStart ) { return }
		log.info "  ******* Job Submission Can be started ******* ";
		advisorInfoService().setJobSubmitionFinished( false );
	}

	def waitJob( long jobId, boolean markScheduleStart=true ) {
		startSchedule( markScheduleStart );
		while( !jobInfoService().jobIsFinished(jobId) ) { sleep 10; }
	}

	def waitAllThread() {
		startSchedule();
		def jobnum = Job.count();
		def threadPool = [];
		jobnum.times { jobid ->
		   def waitThread = new Thread() {
			   waitJob( jobid, false );
	   	   }
		   threadPool << waitThread;
		   executorService().invokeWithSession("waitAllJobFinish") {
			   waitThread.start();
		   }
	   }
	   threadPool.each { athread -> athread.join() }
	}

	def waitAll() {
		startSchedule();
		log.info "[WAIT] for All Test jobs finishing... ";
		while( !testJobFinished ) { sleep 1000; }
		log.info "[WAIT] All Test jobs finished ";
		helperService().withSession("waitAll") {
			def jobnum = Job.count();
			log.info "[WAIT] $jobnum detected.";
			while( true ) {
				def finJobsSize = jobInfoService().getFinishedJobs().size();
				if( finJobsSize == jobnum ) break;
				log.info "[WAIT] $finJobsSize/$jobnum finished ";
				sleep 10000; 
			}
		}
		log.info "[WAIT] All jobs finished. ";
	}

	def waitAllTestJob() {
		// helperService().withSession("waitAllTestJob") { }
		advisorInfoService().setJobSubmitionFinished( true );
		def testJobNum = jobInfoService().getTestJobs().size();
		while( true ) {
			def finTestJobs = jobInfoService().getFinishedTestJobs();
			if( finTestJobs.size() != testJobNum ) {
				log.info "[WAIT] $finTestJobs finished";
				sleep 10000;
			} else { break; }
		}
	}

	def waitOr( long[] jobIds ) {
		def waitPool = [];
		jobIds.each { jobid ->
			def waitThread = new Thread() {
				waitJob( jobid, false );
			}
			waitPool << waitThread;
			executorService().invoke( jobIds.size() ){ waitThread.start(); }
		}
		def orFinished;
		while( !orFinished ) {
			orFinished = waitPool.find { athread -> !athread.isAlive() }
		}
	}

	def waitAnd( long[] jobIds ) {
		def waitPool = [];
		jobIds.each { jobid ->
			def waitThread = new Thread() {
				waitJob( jobid, false );
			}
			waitPool << waitThread;
			executorService().invoke( jobIds.size() ){ waitThread.start(); }
		}
		waitPool.each { athread ->
			athread.join()
		}
	}

	def addJob( long jobId ) throws Exception {
		def job = Job.get( jobId );
		if( !job ) return;
		try {
			log.info "reschedule Job: $job";
			job.executedTimes++;
			job.status = "rescheduled";
			job.save(flush:true);
			addAdvisor( job.function.advisor );
			advisorInfoService().addJob( jobId );
			executorService().invokeWithSession("WaitJobFinish:${job.id}")
			{
				try {
					jobInfoService().jobFinish(job.id);
				} catch ( Exception e ) {
					jobInfoService().updateJob( jobId, [status:"failed"] );
					log.info "${this.class} execution failed: $e";
				}
			}
			// log.info "  scheduled Job: $job"
			return jobId;
		} catch (Exception e) {
			log.error "${this.class}#reAddJob failed: $e";
			throw e;
		}
	}

	def addJob(long clientId, String jobName, Object... args) throws Exception {
		def function = RemoteFunction.list().find { it -> it.name == jobName }
		addJob(clientId, function, args) 
	}

	def addJobFromXMLRPC(String jobName, Object... args) throws Exception {
		def client = new Client();
		def function = RemoteFunction.list().find { it -> it.name == jobName }
		addJob(client.id, function, args) 
	}

	def addSGEJob(long clientId, RemoteFunction function, Object... args) {
		try {
			def testJobnum = functionIsTested[function.name];
			if( !testJobnum || testJobnum < testJobNumLimit ) {
				def rr = Advisor.findByName('RoundRobin');
				if(!rr) rr = new Advisor(name:'RoundRobin').save();
				def func = 
					new RemoteFunction(name:function.name, advisor:rr, isTest:true).save();
				_addSGETestJob(clientId, func, args);
				testJobnum = (testJobnum)? testJobnum+1 : 1;
				functionIsTested[function.name] = testJobnum;
				log.debug "${function.name} is tested."
			}
			tmpJobQueue << [c:clientId,f:function,o:args];
			// log.debug helperService().printHelper(tmpJobQueue,1,"\nJobQueue: ");
			return tmpJobQueue.size()
		} catch( Exception e ) {
			log.error "addJob Test exception", e;
		}
	}

	def addJob(long clientId, RemoteFunction function, Object... args) {
		try {
			def testJobnum = functionIsTested[function.name];
			if( !testJobnum || testJobnum < testJobNumLimit ) {
				def rr = Advisor.findByName('RoundRobin');
				if(!rr) rr = new Advisor(name:'RoundRobin').save();
				def func = 
					new RemoteFunction(name:function.name, advisor:rr, isTest:true).save();
				_addTestJob(clientId, func, args);
				testJobnum = (testJobnum)? testJobnum+1 : 1;
				functionIsTested[function.name] = testJobnum;
				log.debug "${function.name} is tested."
			}
			tmpJobQueue << [c:clientId,f:function,o:args];
			// log.debug helperService().printHelper(tmpJobQueue,1,"\nJobQueue: ");
			return tmpJobQueue.size()
		} catch( Exception e ) {
			log.error "addJob Test exception", e;
			// log.error "addJob Test exception $e";
		}
		//TODO: return job id;
	}

	def _addJob(long clientId, RemoteFunction function, Object... args) {
		log.debug "_schedule Job: $function $args";
		def jobId = jobInfoService().addNewJob( clientId, function, args );
		addAdvisor( function.advisor );
		advisorInfoService().addJob( jobId );
		log.debug "executorService() addJob: $jobId ";
		return jobId;
	}

	def _addSGEJob(long clientId, RemoteFunction function, Object... args) {
		println "_schedule SGEJob: $function $args";
		def jobId = jobInfoService().addSGEJob( clientId, function, args );
		addAdvisor( function.advisor );
		advisorInfoService().addJob( jobId );
		log.debug "executorService() addSGEJob: $jobId ";
		return jobId;
	}

	def addJobPlan(
			long clientId, 
			RemoteFunction function, 
			Object... args) throws Exception {
		try{
			// function duplication prob
			// if( !function.advisor ) addAdvisor( function )
			def jobId = jobInfoService().addNewJob( clientId, function, args );
			return jobId;
		} catch (Exception e) {
			log.error("${this.class}#addJob failed", e);
			throw e;
		}
	}

	def addJobWithCluster( 
			long clientId, 
			long clusterId, 
			String functionName, 
			Object... args ) {
		try{
			def wa = Advisor.list().find { it.name == 'WaitAny' }; 
			if(!wa) wa = new Advisor( name:'WaitAny' ).save();
			def function = RemoteFunction.list().find { it.name == functionName }
			if(!function) {
				function = new RemoteFunction( name:functionName, advisor:rr ).save();
			}
			def aCluster = Cluster.get( clusterId );
			if( !aCluster ) { 
				log.error " [FAL] no Cluster is managed with id $clusterId ";
			}
			log.debug """ test function 
				${function.name}@${function.advisor} with ${aCluster.name}""";
			def testJob = jobInfoService().addNewJobWithCluster( 
					clientId, function, clusterId, args );
			return testJob.ident();
		} catch (Exception e) {
			log.error(" ${this.class}#addTestJob failed", e);
			throw e;
		}
	}

	synchronized  // TODO: make it execute parallelly
	def _addSGETestJob(long clientId, RemoteFunction function, Object... args) {
		log.debug "_schedule TestJob: $function $args";
		Cluster.list().each { aCluster ->
			def numberOfran = 0;
			advisorInfoService().fetchFinishedJobListByCluster( aCluster ).each { aJob ->
				if( aJob.function.name == function.name ) { numberOfran++; }
			}
			if( numberOfran >= testJobNumLimit ) {
				log.debug "no need run testFunc" + 
					" $function in $aCluster [fin:$numberOfran]";
				return;
			}
			testJobFinished = false;
			def testJobId = jobInfoService().addNewSGETestJob(clientId, function, args);
			addAdvisor( function.advisor );
			log.debug "scheduled SGEjob:$testJobId ${function.advisor.name}";
			advisorInfoService().addJob( testJobId );
			log.debug "scheduled SGEjob:$testJobId";
			startTestJobManager( true );
		}
	}

	synchronized  // TODO: make it execute parallelly
	def _addTestJob(long clientId, RemoteFunction function, Object... args) {
		log.debug "_schedule TestJob: $function $args";
		Cluster.list().each { aCluster ->
			def numberOfran = 0;
			advisorInfoService().fetchFinishedJobListByCluster( aCluster ).each { aJob ->
				if( aJob.function.name == function.name ) { numberOfran++; }
			}
			if( numberOfran >= testJobNumLimit ) {
				log.debug "no need run testFunc" + 
					" $function in $aCluster [fin:$numberOfran]";
				return;
			}
			testJobFinished = false;
			def testJobId = jobInfoService().addNewTestJob(clientId, function, args);
			addAdvisor( function.advisor );
			advisorInfoService().addJob( testJobId );
			log.debug "scheduled testjob $testJobId ";
			startTestJobManager();
		}
	}

	def startTestJobManager( boolean isSGE=false ) {
		if( testJobManagerIsStarted ) { return }
		testJobManagerIsStarted = true;
		executorService().invokeWithSession("startTestJobManager") {
			println "testJobMangerThread started. isSGE? $isSGE";
			// CHECK
			while( !advisorInfoService().jobSubmitionIsFinished() ) {
				sleep 1000; 
				log.info "wait job submission finish." + 
					"${advisorInfoService().jobSubmitionIsFinished()}"
			}
			waitAllTestJob();
			def funcs = RemoteFunction.findAllWhere(isTest:true);
			funcs.each { it.name += "Nouse"; it.save(); }
			stopSchedule();
			tmpJobQueue.each { (isSGE)? _addSGEJob(it.c,it.f,it.o): _addJob(it.c,it.f,it.o) }
			tmpJobQueue = [];
			testJobFinished = true;
			startSchedule();
			log.info "test jobs all finished. [${testJobFinished}]";
			testJobManagerIsStarted = false;
		}
	}

	def addTestJob(long clientId, String functionName, Object... args) {
		try{
			def rr = Advisor.list().find { it.name == 'RoundRobin' }; 
			if(!rr) rr = new Advisor( name:'RoundRobin' ).save();
			def function = RemoteFunction.list().find { it.name == functionName }
			if(!function) 
				function = new RemoteFunction( name:functionName, advisor:rr ).save();
			if(!function.advisor) { function.advisor = rr; function.save(); }
			def testJobId = jobInfoService().addNewJob( clientId, function, args );
			def testJob = Job.get( testJobId );
			boolean firstTime = true;
			Cluster.list().each { aCluster ->
				def subJob;
				if(firstTime) {
					testJob.coreGroup = aCluster.name; 
					testJob.mySave();
					subJob = testJob; firstTime = false;
				} else {
					subJob = testJob.clone( coreGroup:aCluster.name );
					log.debug "CloneJob $subJob";
				}
				log.debug "test function" + 
					"${function.name}@${function.advisor} with ${aCluster.name}";
			}
			return testJob.ident();
		} catch (Exception e) {
			log.error("${this.class}#addTestJob failed", e);
			throw e;
		}
	}

	synchronized def addAdvisor( Advisor advisor ) {
		try{
			log.debug "check advisor ";
			if( hasAdvisor( advisor ) ) return;
			log.debug "New Advisor[${advisor.name}] Job Scheduling... ";
			def algorithm = 
				getClass().getClassLoader().
				loadClass("${advisor.name}AdviseAlgorithm").newInstance();
			algorithm.metaClass.advisorInfoService = advisorInfoService();
			algorithm.metaClass.jobInfoService = jobInfoService();
			algorithm.metaClass.executorService = executorService();
			algorithm.metaClass.helperService = helperService();
			algorithm.metaClass.planService = planService();
			algorithm.metaClass.executionAdviceService = executionAdviceService();
			algorithm.metaClass.log = log;
			advisorList << [ advisor:advisor, algorithm:algorithm ];
			advisorInfoService().makeAdvice( advisor, algorithm );
		} catch (Exception e) {
			log.error("${this.class} *ERROR*", e);
		}
	}

	def hasAdvisor( Advisor advisor ) {
		log.debug "check if has $advisor in $advisorList ";
		if( advisorList == [] ) return false;
		def result = advisorList.find {
			it.advisor.name == advisor.name && it.algorithm.haveJobsToSubmit 
		}
		return (result)? true:false;
	}

	def boolean removeAdvisor( Advisor advisor ) {
		try{
			log.info "deleteAdvisor " + advisor.name;
			// quartzScheduler.unscheduleJob("AdvisorTrigger_" + advisor.name, "System")
			// quartzScheduler.deleteJob(advisor.name, "SystemAdvisor");
			removedAdvisorList << advisor;
			return true;
		} catch (Exception e) {
			log.error("removeAdvisor",e);
			return false;
		}
	}

	def addSGEFunction( params ) {
		def advisorType = params.advisor;
		def functionName = params.function;
		def opts = params.opts;
		def advisor = Advisor.findByName( advisorType );
		if( !advisor ) advisor = new Advisor( name:advisorType ).save();
		def func = RemoteFunction.findByName( functionName );
		if( !func ) {
			func = new RemoteFunction();
			func.name = functionName;
			func.advisor = advisor;
			func.advisorType = advisorType;
			println "addFunction -1-";
			func.save(flush:true);
			jobInfoService().addFunctionOpts( func.name, opts );
			println "addFunction -2-";
		}
		if( func.advisor.name != advisorType ) {
			def oldAdvisor = func.advisor;
			func.advisor = advisor;
			func.save();
			log.debug "$func change advisor: $oldAdvisor --> $advisorType ";
		}
		return func;
	}

	def addFunction( params ) {
		def advisorType = params.advisor;
		def functionName = params.function;
		def advisor = Advisor.findByName( advisorType );
		if( !advisor ) advisor = new Advisor( name:advisorType ).save();
		def func = RemoteFunction.findByName( functionName );
		if( !func ) func = new RemoteFunction( name:functionName, advisor:advisor ).save();
		if( func.advisor.name != advisorType ) {
			def oldAdvisor = func.advisor;
			func.advisor = advisor;
			func.save();
			log.debug "$func change advisor: $oldAdvisor --> $advisorType ";
		}
		return func;
	}

	def String toString() {
		return "scheduler Core@${hashCode()}"
	}
}
