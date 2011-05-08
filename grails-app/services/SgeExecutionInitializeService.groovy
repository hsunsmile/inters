
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as HandleLock
import org.ggf.drmaa.SessionFactory;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.JobInfo;

class SgeExecutionInitializeService {
	
    boolean transactional = true;
	Lock handleLock = new HandleLock();
	def sessions = [];
	private def jobIdsServedByCluster = [:];

	def policyProcessorService() { return ServiceReferenceService.references.policyProcessorService; }
	def advisorInfoService() { return ServiceReferenceService.references.advisorInfoService; }
	def jobInfoService() { return ServiceReferenceService.references.jobInfoService; }
	def helperService() { return ServiceReferenceService.references.helperService; }
	def executionAdviceService() { return ServiceReferenceService.references.executionAdviceService; }

	synchronized 
	def getSGESession( String configFile='' ) throws Exception 
	{
		def factory = SessionFactory.factory;
		try {
			def session;
			if (sessions.size > 0) {
				session = sessions[0];
			} else {
				session = factory.session;
				session.init("");
				sessions << session;
				log.info "getSGESession: create new session $configFile ";
			}
			return session;
		} catch(Exception e) {
			def message = "Can not activate ngClient";
			log.error( message, e );
			throw new Exception( message )
		}
	}

	def getFunctionHandle( Job job ) throws Exception 
	{
		def session = getSGESession();
		if( !job ) { throw new Exception("Job is null:${this}[getFunctionHandle]"); }
		log.info "make handle for $job ";
		def message = "create function handle error. ";
		try {
			def jt = session.createJobTemplate();
			jobInfoService().getFunctionOpts(job.function.name).each { k,v -> 
				// println "Jobtemplate: jt.$k = $v";
				jt."$k" = v;
				if( k == "jobName" ) jt."$k" = "$v${job.id}";
			}
			def _args = jobInfoService().getArgs( job.id )[0];
			println "Jobtemplate: $_args, ${_args.class}"
			jt.args = _args;
			return jt;
		} catch ( Exception e ) {
			message = "Job-${job.id} getFunchandle ERROR: $e";
			log.error( message, e );
			throw new Exception( message );
		}
	}

	def getFunctionHandles( session, Job job ) throws Exception 
	{
		if( !session ) session = getSGESession();
		String cluster = job.cluster.name;
		String functionName = job.function.name;
		policyProcessorService().initPolicyInfoProvider("conf/sample.ad");

		def logMsg = "";
		def reduceRatio = policyProcessorService().
			showRemoteClusterInfo( cluster, "onFailReduceRatio" );
		def jobIds = advisorInfoService().fetchRunningJobListByCluster( job.cluster );
		def jobsServed = jobIds.findAll {
			jobInfoService().hasHandle(it) || jobInfoService().jobIsFinished(it)
		}
		def jobsNeedServe = jobIds.findAll {
			!jobInfoService().hasHandle(it) && !jobInfoService().jobIsFinished(it)
		}
		def jobsRunning = jobIds.findAll {
			jobInfoService().hasHandle(it) && !jobInfoService().jobIsFinished(it)
		}
		def clusterLimit = job.cluster.numberOfNodes * job.cluster.numberOfCorePerNode;
		if( jobsNeedServe.size() == 0 ) { logMsg += " no more job to Serve: ${job}\n"; }

		int numOfJobsRunning = jobsRunning.size();
		int numOfJobsConfig = policyProcessorService().
			showRemoteClusterInfo( cluster, "jobPerCall" );
		int numOfJobs = [ jobsNeedServe.size(), numOfJobsRunning, clusterLimit ].min();

		// log.info "[DBG] MIN of: " + [numOfJobsRunning, numOfJobsConfig, clusterLimit];
		logMsg += "\n\t${cluster}.jobsNeedServe: ${jobsNeedServe.size()}";
		logMsg += helperService().printHelper(
				jobsNeedServe, 5, "\n\t${cluster} needServe --> ");
		logMsg += "\n\t${cluster}.jobsServed: ${jobsServed.size()}";
		logMsg += helperService().printHelper( 
				jobsServed, 5, "\n\t${cluster} served --> ");
		logMsg += "\n\t${cluster}.numOfJobsPerCall is ${numOfJobs}";
		log.info logMsg;
		try {
			def jt = session.createJobTemplate();
			jobInfoService().getFunctionOpts(job.function.name).each { k,v -> 
				// println "Jobtemplate: invoke jt.$k($v);";
				jt.invokeMethod("$k","$v");
			}
			def res = [ jt ];
			return res;
		} catch ( Exception e ) {
			log.error("GetFunctionHandles",e);
		}
	}

	def getJobExecutionStatus( String sgeJobId ) {
		def status = (-1);
		if( sgeJobId.toInteger() == 0 ) { return status; }
		def session = getSGESession();
		try {
			def job_status = session.getJobProgramStatus(sgeJobId);
			if( job_status == Session.FAILED ) { status = (-2); }
			if( job_status == Session.DONE ) { status = 1; }
			if( job_status == Session.RUNNING ) { status = 2; }
			if( job_status == Session.QUEUED_ACTIVE ) { status = 30; }
		} catch (Exception e) { println "Can not get sge $sgeJobId status"; }
		return status;
	}

	def execute( long jobId, jt ) {
		println "sge job$jobId start execution3. ${new Date()}";
		def session = getSGESession();
		def job = Job.get( jobId );
		job.cluster.incRunningJobNumbers();
		try {
			def needFurtherExecution = true;
			def failedTimes = 0, failLimit = 10;
			while( failedTimes < failLimit && needFurtherExecution ) {
				try {
					def arguments = jobInfoService().getArgs( jobId );
					// log.info "${job} call ${job.cluster.name} .. startWith $arguments";
					println "sge job$jobId start execution4. ${new Date()}";
					def id = session.runJob(jt);
					jobInfoService().updateJob( jobId, [ sgeId:id ] );
					def job_status = session.getJobProgramStatus(id);
					while( job_status != Session.RUNNING ) {
						job_status = session.getJobProgramStatus(id);
						println "wait job$jobId -- sge$id to run ${job_status} != ${Session.RUNNING} ${new Date()}";
						if( job_status == Session.FAILED ) { throw new Exception("sge job$id failed."); }
						if( job_status == Session.DONE || job_status == Session.RUNNING ) break;
						Thread.sleep(10000);
					}
					println "sge job$jobId start execution5. ${new Date()}";
					def execST = new Date();
					jobInfoService().updateJob( jobId, [ status:"executing", 
							executionStartTime:execST,
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
					println "sge info: $rmap";
					def _end_time = (rmap['end_time'])? Double.parseDouble(rmap['end_time']) : new Date().time;
					def _start_time = (rmap['start_time'])? Double.parseDouble(rmap['start_time']) : execST.time;
					def execution_time = (_end_time - _start_time).toLong();
					println("SGE Job Execution time: ${execution_time}");
					session.deleteJobTemplate(jt);
					jobInfoService().storeJobExecutionInfo( jobId, rmap );
					log.info "${job} call ${job.cluster.name} .. fin ";
					needFurtherExecution = false;
				} catch ( Exception e ) {
					failedTimes++;
					log.error("$job SGE call failed.",e);
					executionAdviceService().addAdviceForExecutionFault(jobId);
					sleep 500*failedTimes;
				}
			}
		} finally {
			job.cluster.decRunningJobNumbers();
			log.info "${job} ngCall exit. ";
		}
	}

	def boolean isAvailableHandle( handle ) {
		if ( !handle ) return false
			if ( handle.isIdle() ) {
				def status = handle.getLocalStatus()
					return true
			}
			else {
				def status = handle.getLocalStatus()
					return false
			}
	}

}

