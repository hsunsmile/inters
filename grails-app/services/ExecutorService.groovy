
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

class ExecutorService {
	
    boolean transactional = true;
	boolean isStop = false;

	def sessionFactory;
	def cachedExecutors = [];
	def unProcThreadPool = [];
	def fixedExecutors = [:];
	def cachedExecutor = Executors.newCachedThreadPool();
	def scheduledExecutorPool = [];

	def helperService() { return ServiceReferenceService.references.helperService; }

	def getCachedExecutor( String message ) {
		def aExecutor = Executors.newCachedThreadPool();
		cachedExecutors << aExecutor;
		log.debug "make cachedExecutor[${aExecutor.hashCode()}] for $message ";
		return aExecutor;
	}

	def stop() {
		isStop = true;
		try { 
			cachedExecutors.each { it.shutdownNow(); }
			cachedExecutors = [];
			/*
			   def clusterNames = [];
			   fixedExecutors.each { key,val -> 
			   if( !(key instanceof Integer) ){
			   val.shutdownNow();
			   log.info "stop jobs in cluster[$key] ";
			   clusterNames << key;
			   }
			   }
			   clusterNames.each { fixedExecutors[it] = null; }
			 */
		}catch( Exception e ) {
			log.error("stop Executor err", e);
		}
		log.debug "fixedExecutors: $fixedExecutors ";
	}

	def resume() {
		isStop = false;
	}

	def storeThreads( thread, other ) {
		unProcThreadPool << [ thread, other ];
	}

	def invokeWithSession( String name="", Closure c ) {
		def aThread = new Thread() {
			def threadId = Thread.currentThread().name;
			try {
				log.info "[$threadId] $name --> start."; 
				helperService().withSession( name,c );
				log.info "[$threadId] $name --> stop."; 
			} catch( Exception e ) {
				log.error("[$threadId] invokeWithSession error: $e");
			} finally {
				// log.info "[$threadId] $name --> stop finally."; 
			}
		}
		if( isStop ) { storeThreads( aThread, null ); return; }
		cachedExecutor.execute( aThread );
	}

	def invoke( Closure c ) {
		// Thread.currentThread().getThreadGroup().list();
		def aThread = new Thread() { 
			helperService().withSession( "invokeCachedExecutor",c ) 
		}
		if( isStop ) { storeThreads( aThread, null ); return; }
		cachedExecutor.execute( aThread );
	}

	def invokeWithFixedDelay( long delay, Closure c ) {
		// Thread.currentThread().getThreadGroup().list();
		def scheduledExecutor = Executors.newScheduledThreadPool(1);
		def aThread = new Thread() {
			def cancel = helperService().withSession( "invokeScheduledExecutor",c );
			if( cancel ) println "invokeScheduledExecutor: $cancel";
			if( cancel ) scheduledExecutor.shutdownNow();
		}
		if( isStop ) { storeThreads( aThread, null ); return; }
		scheduledExecutorPool << scheduledExecutor;
		scheduledExecutor.scheduleAtFixedRate( aThread, delay, delay, TimeUnit.SECONDS );
	}

	def invoke( int num, Closure c ) {
		// Thread.currentThread().getThreadGroup().list();
		def executor = fixedExecutors.get( num, null );
		if( executor == null ) {
			executor = Executors.newFixedThreadPool( num );
			fixedExecutors[ num ] = executor;
			log.debug "made new executor: executor[$num] ";
		}
		def aThread = new Thread() { 
			helperService().withSession( "invokeFixedExecutor",c ) 
		}
		if( isStop ) { storeThreads( aThread, null ); return; }
		try {
			executor.execute( aThread );
		} catch( Exception e ) { log.error "invokeFixedExecutor error: $e"; }
	}

	def invoke( cluster, String jobId="", Closure c ) {
		// if( isStop ) { storeThreads( thread, [cluster, jobId] ); return; }
		// TODO: detect init value: numOfCores
		def num = 10000; // ClusterNode.list().each { num = it.numOfCores; }
		def executor = fixedExecutors.get( num, null );
		if( executor == null ) {
			executor = Executors.newFixedThreadPool( num );
			fixedExecutors[ num ] = executor;
			println "sge made new executor: executor[${cluster.name}[$num]]";
		}
		log.debug "Executor scheduled: $jobId";
		def aThread = new Thread() {
			helperService().withSession( "invokeFixedExecutor",c ) 
		}
		def result;
		// Thread.start {
		// 	Session session = null;
		// 	try {
		// 		session = SessionFactoryUtils.getSession(sessionFactory, false); 
		// 	} catch (java.lang.IllegalStateException ex) { 
		// 		session = SessionFactoryUtils.getSession(sessionFactory, true);  
		// 		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session)); 
		// 	}
		// 	session.setFlushMode(FlushMode.AUTO);
		// 	result = c();
		// 	try {
		// 		def sessionHolder = TransactionSynchronizationManager.unbindResource(sessionFactory);
		// 		SessionFactoryUtils.closeSession(sessionHolder.getSession());
		// 	} catch (Exception ex) { println "sge $ex"; }
		// }
		// if( isStop ) { storeThreads( aThread, null ); return; }
		// println "sge job$jobId execute1 $num jobs paralelly. ${new Date()}, ${ClusterNode.list()}";
		executor.execute( aThread );
		// println "sge job$jobId act:${executor.activeCount}, ${new Date()}";
	}

}
