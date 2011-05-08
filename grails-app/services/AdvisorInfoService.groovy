
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as MyLock
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

class AdvisorInfoService {

	boolean transactional = true;
	boolean jobSubmitionFinished = false;
	
	private def jobMap = [:];
	private def advisorExecution = [:];
	private def jobMapLock = new MyLock();
	private boolean isStoped = false;

	def executorService() { return ServiceReferenceService.references.executorService; }
	def jobInfoService() { return ServiceReferenceService.references.jobInfoService; }
	def helperService() { return ServiceReferenceService.references.helperService; }

	def getCachedExecutor(String message="") {
		return executorService().getCachedExecutor(message);
	}

	synchronized def stop() { isStoped = true; }

	public attachObj( obj ) {
		if( obj.isAttached() ) {
			log.debug "$obj is attached ";
		}
		if( !obj.isAttached() ) {
			obj.attach();
			log.debug "try to attach $obj result:[${obj.isAttached()}]";
		}
		if( !obj.isAttached() ) { 
			// obj = obj.merge(); 
			// log.info " try to merge $obj result:[${obj.isAttached()}]";
		}
		if( !obj.isAttached() ) { log.info "can not attach $obj " }
		return obj;
	}

	def restart() {
		def tmp = [];
		helperService().withLock( jobMapLock, "[restart]" ) {
			Cluster.list().each { aCluster ->
				def jobs = jobMap.get( aCluster.name, [:] );
				tmp += jobs.get( 'running', [] );
				tmp += jobs.get( 'clone', [] );
			}
		}
		def jobIds = tmp.findAll{ jobId ->
			!jobInfoService().hasHandle(jobId)
		}
		log.info "jobs: $jobIds";
		log.info "rescheduleJobs: ${jobMap["UnknownCluster"]}";
		jobIds.each {
			// jobInfoService().changeHandle( job, null );
			jobId -> addJob( jobId );
		}
		log.info "rescheduleJobs: ${jobMap["UnknownCluster"]}";
		isStoped = false;
		setJobSubmitionFinished( true );
	}

	def fetchAdvisedJobList( Advisor advisor = null ) {
		return jobMap
	}

	def fetchFinishedJobListByCluster( Cluster cluster ) {
		def jobIds = [];
		helperService().withLock( jobMapLock, "[$cluster][fetchFinishedJobListByCluster]" ) {
			def result = [];
			def tmp = jobMap.get( cluster.name, [:] );
			result += tmp.get('finished', []).clone();
			result += tmp.get('running', []).clone();
			result += tmp.get('clone', []).clone();
			result.each {
				log.debug "[fetchFinishedJobListByCluster] jobMap is ${jobMap} ";
				if(jobInfoService().jobIsFinished(it)) { jobIds << it; }
			}
		}
		return jobIds;
	}

	def fetchRunningJobListByCluster( Cluster cluster ) {
		def jobIds = [];
		helperService().withLock( jobMapLock, "[$cluster][fetchRunningJobListByCluster]" ) {
			log.debug "[fetchRunningJobListByCluster] jobMap is ${jobMap} ";
			def result = [];
			result += jobMap.get( cluster.name, [:] ).get( 'running', [] ).clone();
			result += jobMap.get( cluster.name, [:] ).get( 'clone', [] ).clone();
			result.each {
				if(!jobInfoService().jobIsFinished(it)) { jobIds << it; }
			}
		}
		return jobIds;
	}

	def setJobSubmitionFinished( boolean yesNo=true ) { 
		jobSubmitionFinished = yesNo;
		println "setJobSubmitionFinished: $jobSubmitionFinished";
	}
	def jobSubmitionIsFinished() { jobSubmitionFinished }

	def updateJobMap() {
		Cluster.list().each { aCluster ->
			helperService().withLock( jobMapLock, "[$aCluster][updateJobMap]" ) {
				def jobList = jobMap.get( aCluster.name, [:] );
				def finishedJobIds = jobList.get( 'finished', [] );
				def runningJobIds = jobList.get( 'running',[] );
				def clonedJobIds = jobList.get( 'clone',[] );
				runningJobIds.each { 
					if( jobInfoService().jobIsFinished(it) ) finishedJobIds << it;
				}
				runningJobIds -= finishedJobIds.unique();
				def cloneFinished = [];
				clonedJobIds.each { 
					if( jobInfoService().jobIsFinished(it) ) cloneFinished << it;
				}
				clonedJobIds -= cloneFinished;
				jobMap[ aCluster.name ] = [ 'running':runningJobIds, 
					'finished':finishedJobIds, 'clone':clonedJobIds ];
			}
		}
	}

	def changeJobCluster( long jobId, long newClusterId ) {
		helperService().withLock( jobMapLock, "changeJobCluster" ) {
			def job = Job.get( jobId );
			def oldCluster = job.cluster;
			def newCluster = Cluster.get( newClusterId );
			if( oldCluster.name == newCluster.name ) return;
			def jobList = jobMap.get( oldCluster.name, [:] );
			def runningJobIds = jobList.get( 'running', [] );
			runningJobIds.each { 
				if(it == jobId){
					log.debug " remove $job from ${oldCluster.name} running list";
					runningJobIds.remove(jobId);
				}
			}
			def clonedJobIds = jobList.get( 'clone',[] );
			clonedJobIds.each { 
				if(it == jobId){
					log.debug " remove $job from ${oldCluster.name} clone list";
					clonedJobIds.remove(jobId);
				}
			}
			jobInfoService().changeJobCluster( jobId, newClusterId );
		}
	}

	def renewJobMap( long jobId ) {
		helperService().withLock( jobMapLock, "renewJobMap" ) {
			def job = Job.get( jobId );
			def jobListByAd = fetchAdvisedJobList();
			def useCluster = job.cluster;
			def jobList = jobListByAd.get( useCluster.name, [:] );
			def finishedJobIds = jobList.get( 'finished', [] );
			def runningJobIds = jobList.get( 'running',[] );
			def clonedJobIds = jobList.get( 'clone',[] );
			( job.isClone )? clonedJobIds << jobId : runningJobIds << jobId ;
			runningJobIds.each {
				if( jobInfoService().jobIsFinished(it) ) finishedJobIds << it;
			}
			runningJobIds -= finishedJobIds.unique();
			def cloneFinished = [];
			clonedJobIds.each {
				if( jobInfoService().jobIsFinished(it) ) cloneFinished << it
			}
			clonedJobIds -= cloneFinished; 
			jobListByAd[ useCluster.name ] = [ 
				'running':runningJobIds, 
				'finished':finishedJobIds, 
				'clone':clonedJobIds 
					]
		}
	}

	def addJob( long jobId ) {
		helperService().withLock( jobMapLock, "addJob" ) {
			// TODO: need or not ?
			// if( jobSubmitionFinished ) setJobSubmitionFinished( false );
			def jobListByAd = jobMap.get( "UnknownCluster", [:] );
			def advisorname = jobInfoService().getAdvisorName( jobId );
			def jobList = jobListByAd.get( advisorname, [] );
			jobList << jobId;
			jobListByAd[ advisorname ] = jobList;
			jobMap[ "UnknownCluster" ] = jobListByAd;
			log.debug "added Job ${jobId}";
		}
	}

	synchronized def needAdvise( Advisor advisor ) {
		try {
			def num = jobMap.
				get( "UnknownCluster", [:] ).
				get( advisor.name, [] ).
				size();
			if( num > 0 ) log.debug " [${advisor}] should advice numJobs --> $num ";
			return num;
		} catch( Exception e ) {
			log.error("needAdvise",e);
		}
	}

	long fetchJob( Advisor advisor ) {
		while( isStoped ) { sleep 1000; }
		def jobId = 0;
		helperService().withLock( jobMapLock, "fetchJob" ) {
			log.debug " $advisor fetchJobs.... ";
			def jobListByAd = jobMap.get( "UnknownCluster", [:] );
			def jobList = jobListByAd.get( advisor.name, [] );
			if( !jobList ) return null;
			jobList.each { log.debug " $advisor have job $it "; }
			// def result = jobList.find { it.status == 'waitAdvise'; }
			def result = jobList.find { jobInfoService().getHandle(it) == null; }
			// result = attachObj( result );
			if( result ) {
				jobInfoService().updateJob( result, [status:'gettingCluster'] );
				jobList.remove( result );
				jobListByAd[ advisor.name ] = jobList;
				log.debug " $advisor fetched $result ";
				jobId = result;
			}
		}
		return jobId;
	}

	def getRandomCluster( long jobId ) {
		def job = Job.get( jobId );
		def clusterId = (int)(Math.random()*100)%Cluster.count() + 1;
		job.cluster = Cluster.get( clusterId );
		renewJobMap( jobId );
	}

	def getTestJobs() {
		def testJobs = [];
		Cluster.list().each { cluster ->
			testJobs += fetchFinishedJobListByCluster( cluster );
		}
		return testJobs;
	}

	def aggregateJobs( advisorInst ) {
		while( !jobSubmitionFinished ) { sleep 1000; }
		def advisedJobs = [];
		Cluster.list().each { cluster ->
			advisedJobs += fetchFinishedJobListByCluster( cluster );
		}
		def fullJobList = [];
		while( true ) {
			def jobnum = Job.count();
			if( fullJobList.size() != jobnum - advisedJobs.size() ) { 
				def result = fetchJob( advisorInst );
				log.debug "${advisedJobs.size()}/[${jobnum}] got $result ";
				fullJobList << result;
				// log.debug "detected ${fullJobList.size()} jobs ";
			} else { break; }
		}
		log.info "finally feteched ${fullJobList.size()} jobs ";
		return fullJobList;
	}

	def makeAdvice( advisor, algorithm ) {
		if(advisorExecution[advisor.name]) {
			log.info "$advisor with $algorithm already registered ";
			return;
		}
		executorService().invokeWithSession("makeAdvice") {
			advisorExecution[advisor.name] = true;
			log.info " $advisor with $algorithm registered";
			while( !jobSubmitionFinished ) { sleep 1000; }
			def limit = 100, tryNum = 0;
			while ( tryNum < limit ) {
				try {
					def num = needAdvise( advisor );
					if( num > 0 ) {
						log.info "setBestCluster -- start ";
						def furtherNeeds = algorithm.setBestCluster( advisor );
						log.info "setBestCluster -- end ";
						if( !furtherNeeds ) {
							tryNum += limit; 
							log.debug "advisor[$algorithm] noNeeds now.. ";
						}
					} else {
						sleep 1000; tryNum++;
					}
				} catch( Exception e ) {
					log.error("advisor[$algorithm] err",e);
				}
			}
			log.debug "advisor[$algorithm] is down. ";
			advisorExecution[advisor.name] = false;
		}
	}

}

