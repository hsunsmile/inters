
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as HandleLock

import org.apgrid.grpc.ng.NgGrpcClient
import org.apgrid.grpc.ng.NgGrpcFunctionHandle
import org.apgrid.grpc.ng.NgGrpcHandleAttr
import org.apgrid.grpc.ng.NgGrpcHandle
import org.gridforum.gridrpc.GrpcClientFactory
import org.gridforum.gridrpc.GrpcException

class NgExecutionInitializeService {
	
    boolean transactional = true;
	Lock handleLock = new HandleLock();
	String confFile;
	private def jobIdsServedByCluster = [:];

	def policyProcessorService() { return ServiceReferenceService.references.policyProcessorService; }
	def advisorInfoService() { return ServiceReferenceService.references.advisorInfoService; }
	def jobInfoService() { return ServiceReferenceService.references.jobInfoService; }
	def helperService() { return ServiceReferenceService.references.helperService; }
	def executionAdviceService() { return ServiceReferenceService.references.executionAdviceService; }

	synchronized def getNgGrpcClient( String configFile ) throws Exception 
	{
		confFile = configFile;
		def	client = (NgGrpcClient) GrpcClientFactory.
			getClient("org.apgrid.grpc.ng.NgGrpcClient");
		try {
			client.activate( configFile ); // active by property
			log.info " getNgGrpcClient: ngClient activated with $configFile ";
			return client
		}
		catch(Exception e) {
			def message = "Can not activate ngClient";
			log.error( message, e );
			throw new Exception( message )
		}
	}

	def getFunctionHandle( client, Job job ) throws Exception 
	{
		if( !job ) { throw new Exception("Job is null:${this}[getFunctionHandle]"); }
		log.debug " make handle for $job ";
		String cluster = job.cluster.name;
		String functionName = job.function.name;
		Properties prop = new Properties();
		prop.put(NgGrpcHandleAttr.KEY_HOSTNAME, cluster );
		def message = " create function handle error. ";
		try {
			def aHandle = client.getFunctionHandle(functionName, prop);
			return aHandle;
		}
		catch ( GrpcException e ) {
			message = " ${job} funchandle ERROR: in ${cluster} handles";
			log.error( message, e );
			throw new Exception( message );
		}
	}

	def getFunctionHandles( client, Job job ) throws Exception 
	{
		if( !client ) { throw new Exception("ninf-g client is null"); }
		if( !client && confFile ) client = getNgGrpcClient( confFile );
		String cluster = job.cluster.name;
		String functionName = job.function.name;
		Properties prop = new Properties();
		prop.put(NgGrpcHandleAttr.KEY_HOSTNAME, cluster );
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
		log.debug logMsg;
		// def message = " create function handle error. ";
		// while( numOfJobs >= 1 ) {
		try {
			def handles = client.getFunctionHandles( functionName, prop, 1 );
			println " ${job} got cluster:${job.cluster} handles: ${handles}";
			def res = [];
			for( int i=0; i<handles.size(); i++ ) { res << handles[i]; }
			return res;
		}
		catch ( GrpcException e ) {
			/*
			   message = " ${job}#${job.status}  *MKHandle* ERROR:" + 
			   " in ${cluster} alloc ${numOfJobs} handles $e ";
			   numOfJobs = (int) Math.round(numOfJobs * reduceRatio);
			 */	
			// log.error("GetFunctionHandlesError for job ${job.id}",e);
			log.error("GetFunctionHandlesError for job ${job.id}");
		}
		// }
		// throw new Exception( message );
	}

	def grpcExecute( long jobId, handle ) {
		def job = Job.get( jobId );
		job.cluster.incRunningJobNumbers();
		try {
			def needFurtherExecution = true;
			def failedTimes = 0, failLimit = 3;
			while( failedTimes < failLimit && needFurtherExecution ) {
				try {
					Properties sessionAttr = new Properties();
					sessionAttr.put( "hostname", job.cluster.name );
					def arguments = jobInfoService().getArgs( jobId );
					log.debug "${job}" + 
						" $handle ngCall ${job.cluster.name} .. startWith $arguments ";
					def execST = new Date();
					jobInfoService().updateJob( jobId, [ status:"executing", 
							executionStartTime:execST,
							queuingTime:(execST.time-job.createdTime.time)/1000 ] );
					def executionInfo = handle.callWith( sessionAttr, arguments );
					jobInfoService().storeJobExecutionInfo( jobId, executionInfo );
					log.debug "${job} ngCall ${job.cluster.name} .. fin ";
					needFurtherExecution = false;
				} catch ( Exception e ) {
					failedTimes++; // log.error("$job gridRPC call failed.",e);
					log.error("$job gridRPC call failed. $e");
					sleep 500*failedTimes;
				}
			}
			if(needFurtherExecution) throw new Exception("$jobId needFurtherExecution");
		} finally {
			job.cluster.decRunningJobNumbers();
			log.debug "${job} ngCall exit. ";
			try {
				handle.dispose();
			} catch ( Exception e ) {
				log.error("${job} disposeHandle err:",e);
			}
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


	/*
	   def String getFunctionHandleStatusString( handle ) {
	   def status = handle.getLocalStatus()
	   def result

	   switch( status ) {
	   case NgGrpcHandle.CLIENTSTATE_NONE:
	   result = 'CLIENTSTATE_NONE'
	   break
	   case NgGrpcHandle.CLIENTSTATE_IDLE:
	   result = 'CLIENTSTATE_IDLE'
	   break
	   case NgGrpcHandle.CLIENTSTATE_INIT:
	   result = 'CLIENTSTATE_INIT'
	   break
	   case NgGrpcHandle.CLIENTSTATE_INVOKE_SESSION:
result = 'CLIENTSTATE_INVOKE_SESSION'
break
case NgGrpcHandle.CLIENTSTATE_TRANSARG:
result = 'CLIENTSTATE_TRANSARG'
break
case NgGrpcHandle.CLIENTSTATE_WAIT:
result = 'CLIENTSTATE_WAIT'
break
case NgGrpcHandle.CLIENTSTATE_COMPLETE_CALCULATING:
result = 'CLIENTSTATE_COMPLETE_CALCULATING'
break
case NgGrpcHandle.CLIENTSTATE_TRANSRES:
result = 'CLIENTSTATE_TRANSRES'
break
case NgGrpcHandle.CLIENTSTATE_PULLBACK:
result = 'CLIENTSTATE_PULLBACK'
break
case NgGrpcHandle.CLIENTSTATE_SUSPEND:
result = 'CLIENTSTATE_SUSPEND'
break
case NgGrpcHandle.CLIENTSTATE_RESUME:
result = 'CLIENTSTATE_RESUME'
break
case NgGrpcHandle.CLIENTSTATE_DISPOSE:
result = 'CLIENTSTATE_DISPOSE'
break
case NgGrpcHandle.CLIENTSTATE_RESET:
result = 'CLIENTSTATE_RESET'
break
case NgGrpcHandle.CLIENTSTATE_CANCEL:
result = 'CLIENTSTATE_CANCEL'
break
case NgGrpcHandle.CLIENTSTATE_INVOKE_CALLBACK:
result = 'CLIENTSTATE_INVOKE_CALLBACK'
break
default:
result = 'CLIENTSTATE_NONE'
}

return result
}
*/

}

