
import java.io.*;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock as updateLock 

class Job {

	def static final updateLock = new updateLock();

	RemoteFunction function;
	Cluster cluster;
	Client client;
	ExecutionInfo details = null;

	String name = "seqNum";
	String status = "created";
	String coreGroup = "System";
	String sgeId = "0";
	Date createdTime = new Date();
	Date executionStartTime;
	Date executionFinishTime;
	double executionTime = -1.0;
	double queuingTime = -1.0;
	double interactiveTime = -1.0;
	int executedTimes = 0;
	int jid = -1;
	int failedTimes = 0;
	int failLimit = 100;
	boolean isClone = false;
	boolean disposeHandle = false;
	boolean disposeSharedHandle = false;
	boolean finishedRunning = false;
	boolean startRunning = false;
	boolean isSGE = false;

	def jobInfoService;
	def serviceReferenceService;

	static constraints = {
		name()
		cluster(nullable:true)
		status()
		queuingTime()
		interactiveTime()
		executionTime(nullable:true)
		executionStartTime(nullable:true)
		executionFinishTime(nullable:true)
		createdTime(blank:false)
		executedTimes()
		details(nullable:true)
		function(nullable:false)
		jid()
		coreGroup(nullable:true)
		client(nullable:false)
		isClone()
	}

	static mapping = {
		function lazy:false 
		cluster  lazy:false
	}

	String toString() { " $name[$status][$cluster][ver:$version] " }
	// String toString() { name }

	def clone( params = null ) {
		def njob = jobInfoService.makeCloneJob( this.id, params );
		return njob;
	}

	def exectionEstimateTime() {
		return ( executionStartTime )? 
			new Date((long)(executionStartTime.time + executionTime*1000)) :
			new Date() - 1;
	}

	def executionTimeNow() {
		if( executionTime > 0 ) return executionTime - queuingTime;
		return ( executionStartTime )?
			(long)(((new Date()).time - executionStartTime.time) / 1000 ) : 0;
	}

	def queuingTimeNow() {
		return ( queuingTime > 0 )? 
			(long)queuingTime : (long)( ((new Date()).time - createdTime.time) / 1000 );
	}

	def getHandle() {
		def _exeService = serviceReferenceService.references.sgeExecutionInitializeService;
		return _exeService.getFunctionHandle(this);
		// TODO: whether is SGE or not
		def _handleService = (isSGE)? "sgeClusterHandleService" : "ngClusterHandleService";
		def handleService = serviceReferenceService.references[ _handleService ];
		while (!handleService) {
			sleep 1000;
			println "wait for handleService <-- [$serviceReferenceService]";
			handleService = serviceReferenceService.references[ _handleService ];
		}
		println "handleService now is: $handleService, isSGE? $isSGE ";
		def ahandle = handleService.getOneOfTheHandles( this.id );
	}

}
