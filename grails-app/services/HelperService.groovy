import java.util.concurrent.locks.ReentrantLock as UpdateLock 
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.apache.log4j.NDC;


class HelperService {

	def transactional = true;
	def sessionFactory;

	def sessionMap = [ ids:[], holder:null ];
    def final sessionLock = new UpdateLock();

	def pushNDC( String message ) {
		def prefix = "\n[   ]";
		NDC.push("$prefix$message");
	}

	def popNDC() { NDC.pop(); }

	def printHelper( list, numInLine=10, prefix="\n\t" ) {
		def printQ = [], logMsg = "";
		list.each { item ->
			if( printQ.size() < numInLine ) {
				printQ << item;
			} else {
				logMsg += "$prefix$printQ"; 
				printQ = [ item ];
			}
		}
		if( printQ ) logMsg += "$prefix$printQ";
		return logMsg;
	}

	def getCurrentSessionHolder() {
		def session = sessionFactory.getCurrentSession()
		def sessionHolder = 
			TransactionSynchronizationManager.getResource(sessionFactory);
		log.debug " Executor \t$session, \n\t$sessionHolder ";
		if( sessionHolder == null ) {
			sessionHolder = 
				TransactionSynchronizationManager.getResource(sessionFactory);
			log.debug " Executor new-session ";
		}
		return sessionHolder;
	}

	def withLock( UpdateLock lock, String name, Closure c ) {
        lock.lock();
		// def session = sessionFactory.getCurrentSession();
		def threadId = Thread.currentThread().name;
		def result;
        try {
			// log.debug "[Session:${session.hashCode()}][Thread:$threadId] ";
            log.debug "$name --> withLock:${lock.hashCode()} start";
			result = c();
		} catch( Exception e ) {
            log.error("withLock Executor err",e);
		} finally {
           	lock.unlock();
			log.debug "$name --> withLock:${lock.hashCode()} stop";
        }
		return result;
	}

	def getSessionHolderShare() {
		if(sessionFactory == null) {
			throw new IllegalStateException("No sessionFactory property provided");
		}
		def holder = sessionMap.holder;
		def threadId = Thread.currentThread().name;
		if( !holder ) {
			def session = SessionFactoryUtils.getSession(sessionFactory, true);
			session.setFlushMode(FlushMode.AUTO);
			holder = new SessionHolder(session);
			withLock(sessionLock, "getSessionHolderShare") { sessionMap.holder = holder; }
			log.debug "new sessionHolder${holder.hashCode()} for Thread:$threadId\n";
		}
		return holder;
	}

	def getSessionHolder() {
		def threadId = Thread.currentThread().name;
		def session = SessionFactoryUtils.getSession(sessionFactory, true);
		session.setFlushMode(FlushMode.AUTO);
		def holder = new SessionHolder(session);
		log.debug "new sessionHolder${holder.hashCode()} for Thread:$threadId\n";
		return holder;
	}

	def hasBackgroundSession() {
		if(sessionFactory == null) {
			throw new IllegalStateException("No sessionFactory property provided");
		}
		def threadId = Thread.currentThread().name;
		final def inStorage = 
			TransactionSynchronizationManager.getResource(sessionFactory);
		if(inStorage == null) { return WITHSESSION.NEEDNEW; }
		def sholderCode = inStorage.hashCode();
		log.debug "${threadId} already bind ${sholderCode}";
		if( inStorage == sessionMap.holder ) {
			log.debug "${threadId} &&&& ${sholderCode}";
		}
		return WITHSESSION.CALLOK;
	}

	def bindSession( withHolder=null ) {
		if(sessionFactory == null) {
			throw new IllegalStateException("No sessionFactory property provided");
		}
		def holder = (withHolder)? withHolder:getSessionHolder();
		TransactionSynchronizationManager.bindResource(sessionFactory, holder);
		def threadId = Thread.currentThread().name;
		withLock(sessionLock,"bindSession") {
			sessionMap.ids << threadId;
			def logMsg = "HSession:${holder.hashCode()} <<<< ${threadId}\n";
			logMsg += "sessionHolder${holder.hashCode()} shared:";
			logMsg += printHelper(sessionMap.ids, 2, "\n\tThread:");
			log.debug logMsg;
		}
	}

	def unbindSession() {
		if(sessionFactory == null) {
			throw new IllegalStateException("No sessionFactory property provided");
		}
		def threadId = Thread.currentThread().name;
		try {
			final def sessionHolder = 
				TransactionSynchronizationManager.unbindResource(sessionFactory);
			def sholderCode = sessionHolder.hashCode();
			if(!FlushMode.MANUAL.
					equals(sessionHolder.getSession().getFlushMode())) {
				sessionHolder.getSession().flush();
			}
			SessionFactoryUtils.closeSession(sessionHolder.getSession());
		} catch(Exception e) {
			log.error("[$threadId] unbindSession err", e);
		}
	}

	def withSessionHelper( String name="", holder, Closure c ) {
		def result
		def threadId = Thread.currentThread().name;
		log.debug "$threadId ++++ $holder";
		bindSession(holder);
		try {
			result = c();
		} catch( Exception e ) {
			log.error("[$threadId] invokeWithSessionHelper err", e);
		} finally {
			unbindSession();
		}
		log.info "helper_helper[${name}] return with: $result ";
		return result
	}

	def withSession( String name="", Closure c ) {
		def result;
		def status = hasBackgroundSession();
		def threadId = Thread.currentThread().name;
		try {
			switch( status ) {
				case WITHSESSION.NEEDNEW:
					def holder = getSessionHolder();
					result = withSessionHelper(name,holder,c);
					break;
				case WITHSESSION.NEEDTHREAD:
					break;
				case WITHSESSION.CALLOK:
					result = c();
					break;
			}
		} catch( Exception e ) { log.error("withSession",e); }
		return result;
	}
}

public enum WITHSESSION { NEEDNEW, NEEDTHREAD, CALLOK }
