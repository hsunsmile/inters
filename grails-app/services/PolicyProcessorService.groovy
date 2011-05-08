import condor.classad.*;
import java.util.*;
import java.lang.management.ManagementFactory;		
import java.lang.management.OperatingSystemMXBean;		
import org.codehaus.groovy.grails.commons.*

class PolicyProcessorService {
	
    boolean transactional = true; 
	def config = ConfigurationHolder.config.inters;

	private def policyFiles = [];
	private def policyFilePath;  // only one policy file can be used now.

	def clusterInfoProviderService() { return ServiceReferenceService.references.clusterInfoProviderService; }

	def initPolicyInfoProvider( policyFile ) {
		policyFilePath = policyFile;
	}

	def showClusterNames( aPolicy=null ) { return showConfigInfo( "name", aPolicy ); }

	def showPlanType( String planUsage ) { return showConfigInfo( planUsage ); }

    def showRemoteClusterInfo( clusterName, prop, aPolicy=null ) { 
        def cluster;
		def classAd = reloadConfig( aPolicy, clusterName );
        for( Iterator i = classAd.attributes(); i.hasNext() && (cluster == null); ) { 
            def attr = i.next().toString();
            if( attr =~ /cluster([0-9])*/ ) { 
                String[] clusterNameQuery = [ attr, "name" ];
                def name = ClassAd.eval( classAd, clusterNameQuery ).
					toString().replace('\"','');
                if( clusterName == name ) cluster = attr;
                log.debug "find cluster ${attr} ${name} ${clusterName} .. ${cluster} ";
            }
        }
        String[] clusterNameQuery = [ cluster, prop ];
        def result = ClassAd.eval( classAd, clusterNameQuery ).value; //toString();
        log.debug "$clusterName.$prop is : $result ";
        log.debug "$result is instanceof ${result.class} ";
        return result;
    }

	def getCustomizedSchedulingPlan( prop='cluster_ranking_data', aPolicy=null ) { 
		def customerPlans = [:];
		def classAd = reloadConfig( aPolicy );
		def allClusters = [];
		for( Iterator i = classAd.attributes(); i.hasNext(); ) {
			def attr = i.next().toString();
			if( attr =~ /cluster([_a-zA-Z0-9]*)/ ) { allClusters << attr; }
		}
		for( Iterator i = classAd.attributes(); i.hasNext(); ) {
			def attr = i.next().toString();
			def match = attr =~ /scheduling_([a-zA-Z0-9]*)/;
			if( match ) {
				String[] clusterRankingQuery = [ attr, prop ];
				def planName = match[0][1];
				def ranking = ClassAd.eval( classAd, clusterRankingQuery );
				def cluster_rank = [];
				log.debug "$ranking is instanceof ${ranking.class}";
				if (ranking instanceof ListExpr) {
					for( Iterator j = ranking.iterator(); j.hasNext(); ) {
						def cluster = j.next().toString();
						String[] clusterNameQuery = [ cluster, 'name' ];
						cluster_rank << ClassAd.eval( classAd, clusterNameQuery ).value; //toString();
					}
				} else {
					def tmp = new TreeMap();
					allClusters.each { cluster ->
						log.debug "attr: $attr"
							String[] clusterNameQuery = [ cluster, 'name' ];
						String[] clusterOrderQuery = [ cluster, ranking.toString().replace('\"','') ];
						String[] orderQuery = [ attr, prop.replace('\"','').replace('data','order') ];
						def clusterName = ClassAd.eval( classAd, clusterNameQuery ).value; //toString();
						def rank = ClassAd.eval( classAd, clusterOrderQuery ).value; //toString();
						def order = ClassAd.eval( classAd, orderQuery ).value; //toString();
						log.debug " $cluster , $order ";
						if( order =~ /desc/ ) rank = -rank;
						tmp[rank] = clusterName;
					}
					tmp.each { cluster_rank << it.value }
				}
				customerPlans[ planName ] = cluster_rank;
				// log.debug "${planName}.clusterRank is : $ranking ";
			}
		}
		log.debug customerPlans;
		return customerPlans;
	}

	private def showConfigInfo( attributes, aPolicy=null ) {
		def results = []; 
		def classAd = reloadConfig( aPolicy );
		for( Iterator i = classAd.attributes(); i.hasNext(); ) { 
			def attr = i.next().toString();
			if( attr =~ /cluster([0-9])*/ ) {
				log.debug "find cluster ${attr} ";
				String[] clusterNameQuery = [ attr, attributes ];
				results << ClassAd.eval( classAd, clusterNameQuery ).
					toString().replace('\"','');
			}
		}
		return results.unique();
	}

	private def formatInt( aString, defaultVal ) {
		try {
			return aString.toInteger();
		} catch( Exception e ) { 
			e.printStackTrace();
			return defaultVal;
		}
	}

	private def formatDouble( aString, defaultVal ) {
		try {
			return aString.toDouble();
		} catch( Exception e ) { 
			e.printStackTrace();
			return defaultVal;
		}
	}

	private def processPolicyFile( policyFile ) {
		if( !policyFile ) return
		println "processPolicyFile: use file --> $policyFile"
		BufferedReader br = new BufferedReader(new FileReader( policyFile ));
		def line = "", classAdString = "";
		while (( line = br.readLine()) != null) { classAdString += "$line\n"; };
		return classAdString;
	}

	private def createClassAd( classAdString ) {
		if( classAdString == "" ) { throw new Exception(" can not create classAd without config file. "); }
		ClassAdParser parser = new ClassAdParser( classAdString );
		return parser.parse();
	}

	private def reloadConfig( configFile ) {
		def policy = (configFile)? configFile : policyFilePath;
		def testString = processPolicyFile( policy );
		testString = replaceAllBuiltIns( testString );
		return createClassAd( testString );
	}

	private def reloadConfig( configFile, clusterName ) {
		def defaultPolicy = config.classad.config.file;
		def policy = (configFile)? configFile:defaultPolicy;
		def testString = processPolicyFile( policy );
		testString = replaceAllBuiltIns( testString );
		testString = replaceAllRemoteBuiltIns( testString, clusterName );
		return createClassAd( testString );
	}

	private def replaceLoadAvg( testString ) {
		OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();		
		def matcher = ( testString =~ /CLIENT_LOAD_AVERAGE/ );
		testString = matcher.replaceAll(osMXBean.getSystemLoadAverage().toString());
	}

	private def replaceThreadNum( testString ) {
		def matcher = ( testString =~ /CLIENT_NUM_OF_THREAD/ );
		testString = matcher.replaceAll(Thread.activeCount().toString());
	}

	private def replaceMemory( testString ) {
		def matcher = ( testString =~ /CLIENT_JVM_MEMORY_NOW/ );
		def memoryInMega = Runtime.getRuntime().totalMemory() / 1024 / 1024;
		testString = matcher.replaceAll(memoryInMega.toString());
	}

	private def replaceRemoteCPUInfo( testString, clusterName ) {
		clusterInfoProviderService().initMDSInfoProvider( clusterName );
		def matcher = ( testString =~ /RMOTE_NODE_CPUINFO/ );
		def remoteCPUInfo = clusterInfoProviderService().averageProcessor( clusterName );
		testString = matcher.replaceAll(remoteCPUInfo.toString());
	}

	private def replaceRemoteMemInfo( testString, clusterName ) {
		clusterInfoProviderService().initMDSInfoProvider( clusterName );
		def matcher = ( testString =~ /RMOTE_NODE_MEMINFO/ );
		def remoteMemInfo = clusterInfoProviderService().averageMemory( clusterName );
		testString = matcher.replaceAll(remoteMemInfo.toString());
	}

	private def replaceRemoteNumCore( testString, clusterName ) {
		clusterInfoProviderService().initMDSInfoProvider( clusterName );
		def matcher = ( testString =~ /RMOTE_NODE_NUMOFCORE/ );
		def remoteNodeInfo = clusterInfoProviderService().averageCores( clusterName );
		testString = matcher.replaceAll(remoteNodeInfo.toString());
	}

	private def replaceRemoteNodeNumber( testString, clusterName ) {
		clusterInfoProviderService().initMDSInfoProvider( clusterName );
		def matcher = ( testString =~ /RMOTE_NODE_NUMBER/ );
		def remoteCPUInfo = clusterInfoProviderService().totalProcessor(clusterName);
		def logMessage = "remoteCPUInfo for $clusterName is ${remoteCPUInfo}";
		log.debug logMessage;
		testString = matcher.replaceAll(remoteCPUInfo.toString());
	}

	private def replaceAllBuiltIns( testString ) {
		testString = replaceLoadAvg( testString );
		testString = replaceThreadNum( testString );
		testString = replaceMemory( testString );
		log.debug "classAd was changed to ${testString} ";
		return testString;
	}

	private def replaceAllRemoteBuiltIns( testString, clusterName ) {
		testString = replaceRemoteCPUInfo( testString, clusterName );
		testString = replaceRemoteMemInfo( testString, clusterName );
		testString = replaceRemoteNodeNumber( testString, clusterName );
		testString = replaceRemoteNumCore( testString, clusterName );
		log.debug "classAd was changed to ${testString} ";
		return testString;
	}

	def showCostLimitation( aPolicy=null ) {
		def classAd = reloadConfig( aPolicy );
		if( !classAd ) { return 1000000; }
		def limitation = ClassAd.eval(classAd,"costLimitation").toString();
		return formatDouble( limitation, 0 );
	}
	/*
	   def showNumOfJobReduce( aPolicy=null ) {
	   def classAd = reloadConfig( aPolicy );
	   def ratio = ClassAd.eval(classAd,"numOfJobsPerCallReduceRatio").toString();
	   return formatDouble( ratio, 0.5 );
	   }
	 */

}
