import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock as HandleLock
import org.apgrid.grpc.ng.NgGrpcClient
import org.apgrid.grpc.ng.NgGrpcFunctionHandle
import org.apgrid.grpc.ng.NgGrpcHandleAttr
import org.gridforum.gridrpc.GrpcClientFactory
import org.gridforum.gridrpc.GrpcException

class NgClusterHandleService {
	
    boolean transactional = true
	private def handlesHolder = [:];
	private def clientRelatedHandleSet = [:]
	private def clientRelatedNgClientSet = [:]
	private def clusterLock = [:]
	private Lock handleLock = new HandleLock()
	private int ngClientNum = 0

	def jobInfoService() { return ServiceReferenceService.references.jobInfoService; }
	def ngExecutionInitializeService() { return ServiceReferenceService.references.ngExecutionInitializeService; }

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

	synchronized private def getNgClient( aJobId ) {
		def job = Job.get( aJobId );
		def key = job.client.configFilePath 
		def clientList = clientRelatedNgClientSet.get(key, [])
		try {
			def ngClientLockPair
			if ( ngClientNum < Cluster.count() ) {
				def ngClient = ngExecutionInitializeService().getNgGrpcClient( key )
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
		def job = Job.get( jobId );
		def ngClientLockPair = getNgClient( jobId );
		def cLock = getClusterLock( job.cluster.name );
		log.info " getting handle for job[$jobId] ";
		try {
			boolean isNew = true;
			def ngClient = ngClientLockPair.client;
			def aHandle = ngExecutionInitializeService().
				getFunctionHandles(ngClient,job)?.getAt(0);
			if( !aHandle ) { throw new Exception("there is no handle for job:$jobId. try again."); }
			log.info " got handle " + 
				"${aHandle} for ${job.id} [isNew:$isNew] ";
			return aHandle;
		} catch ( Exception e ) { 
			log.error("make handle[try:$num] for ${job.id} err: $e");
			sleep 1000;
			if( num < 10 ) return getOneOfTheHandles( jobId, ++num );
		} finally { 
			// cLock.unlock(); 
		}
	}

	def createNgHandle( long jobId ) throws Exception {
		def job = Job.get( jobId );
		def ngClientLockPair = getNgClient( jobId );
		def cLock = getClusterLock( job.cluster.name );
		cLock.lock();
		try {
			def ngClient = ngClientLockPair.client
				def handle = ngExecutionInitializeService().
						getFunctionHandle( ngClient, job )
						return handle
				}
			catch ( Exception e ) { throw e }
			finally { cLock.unlock() }
		}

	def disposeNgHandle( long jobId ) throws Exception {
		def noUsedHandle;
		def handleMap = getHandleMap( jobId );
		while( !noUsedHandle && handleMap.size() ) {
			noUsedHandle = findNgHandle( jobId );
		}
		def handle = jobInfoService().getHandle( jobId );
		log.info " ** ${jobId} *DISPOSE* ${handle} ";
		noUsedHandle?.dispose()
	}

	def findNgHandle( 
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
