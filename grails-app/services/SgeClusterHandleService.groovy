import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as HandleLock

class SgeClusterHandleService {
	
    boolean transactional = true
	private def handlesHolder = [:];
	private def clientRelatedHandleSet = [:]
	private def clientRelatedSGEClientSet = [:]
	private def clusterLock = [:]
	private Lock handleLock = new HandleLock()
	private int ngClientNum = 0

	def jobInfoService() { return ServiceReferenceService.references.jobInfoService; }
	def sgeExecutionInitializeService() { return ServiceReferenceService.references.sgeExecutionInitializeService; }

	synchronized private 
	def getClusterLock( String clusterName ) {
		def cLock = clusterLock.get( clusterName, null )
		if( !cLock ) {
			cLock = new HandleLock()
			clusterLock[ clusterName ] = cLock
		}
		return cLock
	}

	def getInstance() { return this; }

	synchronized private def getSGEClient( aJobId ) {
		def job = Job.get( aJobId );
		def key = job.client.configFilePath 
		def clientList = clientRelatedSGEClientSet.get(key, [])
		try {
			def ngClientLockPair
			if ( ngClientNum < Cluster.count() ) {
				def ngClient = sgeExecutionInitializeService().getSGESession( key )
				def aLock = new HandleLock()
				ngClientLockPair = [ 'client':ngClient, 'lock':aLock ]
				clientList << ngClientLockPair
				ngClientNum++
			}
			else {
				def radnum = (int)(Math.random()*1000) % ngClientNum
				ngClientLockPair = clientList[ radnum ]
			}
			return ngClientLockPair
		}
		catch( Exception e ) {
			log.error("$job#${job.status} *ACTIVATE* ERROR",e);
			throw e;
		}
	}

	private 
	def getHandleMap( aJobId ) {
		def job = Job.get( aJobId );
		def handlesDivedByCluster = 
				clientRelatedHandleSet.get( job.client.configFilePath, [:] )
		if(!handlesDivedByCluster) {
			clientRelatedHandleSet[ job.client.configFilePath ] = handlesDivedByCluster
		}
		def handleMap = handlesDivedByCluster.get( job.cluster.name, [:] )
		if( !handleMap ) handlesDivedByCluster[ job.cluster.name ] = handleMap
		return handleMap
	}

	def getOneOfTheHandles( long jobId, int num=0 ) throws Exception {
		try {
			def job = Job.get( jobId );
			return sgeExecutionInitializeService().getFunctionHandle(job)
		} catch ( Exception e ) {
			sleep 1000;
			if( num < 10 ) return getOneOfTheHandles( jobId, ++num );
		}
	}

	def createSGEHandle( long jobId ) throws Exception {
		def job = Job.get( jobId );
		def ngClientLockPair = getSGEClient( jobId );
		def cLock = getClusterLock( job.cluster.name );
		cLock.lock();
		try {
			return sgeExecutionInitializeService().getFunctionHandle( job )
		} catch ( Exception e ) { throw e 
		} finally { cLock.unlock() }
	}

	def disposeSGEHandle( long jobId ) throws Exception {
		def noUsedHandle;
		def handleMap = getHandleMap( jobId );
		while( !noUsedHandle && handleMap.size() ) {
			noUsedHandle = findSGEHandle( jobId );
		}
		def handle = jobInfoService().getHandle( jobId );
		log.info " ** ${jobId} *DISPOSE* ${handle} ";
		noUsedHandle?.dispose()
	}

	def findSGEHandle( 
			long jobId, 
			boolean disposeSharedHandle = false 
			) throws Exception {
		if( jobInfoService().hasHandle( jobId ) ) { return }
		def handleMap = getHandleMap( jobId );
		handleLock.lock();
		try {
			handleMap.each { entry ->
				def handle = entry.value;
				def function = entry.key;
				def functionName = jobInfoService().getFunctionName( jobId );
				if ( function =~ /functionName(.)*/ && handle ) {
					jobInfoService().changeHandle( jobId, handle );
					if( disposeSharedHandle ) handleMap.remove( function );
				}
			}
		} finally {
			handleLock.unlock();
		}
	}

	int addHandle( long jobId ) {
		def handleMap = getHandleMap( jobId );
		def index = 0;
		handleLock.lock();
		try {
			def functionName = jobInfoService().getFunctionName( jobId );
			handleMap.each { entry->
				def handle = entry.value
					def function = entry.key
					if ( function =~ /functionName(.)*/ ) {
						index++
					}
			}
			assert handleMap.get( functionName+index, null ) == null;
			handleMap[ functionName+index ] = jobInfoService().getHandle( jobId );
			//renewHandleMap( job, handleMap )
			//knownFunctions << [ (job.cluster.name) : job.function.name ]
		} finally {
			handleLock.unlock()
		}
		return index
	}

}
