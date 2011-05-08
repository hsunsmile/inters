
class RemoteFunction { 

	String name
	String advisorType
	Advisor advisor
	boolean isTest = false;
	def testResult = [];
	// Map opts;

	def beforeInsert = {
		// println "RemoteFunc $name@${hashCode()} *CREATED*" + 
		//	" with ${advisor}#${advisor?.hashCode()}"
	}

	def afterInsert = {
		println "RemoteFunction: $name && $advisor";
	}

	static constraints = {
		name(nullable:false)
		advisorType(nullable:true)	
		advisor(nullable:true)	
	}

	static mapping = { advisor lazy:false }

	def baseCalculationTime() {
		def cluster = Cluster.get(1);
		getExecutionTime( cluster.name );
	}

	def baseCPUInfo() {
		def cluster = Cluster.get(1);
		cluster.cpuInfo;
	}

	double getExecutionTime( String cluster ) { }

	String toString() {
		"$name${testResult}";
	}
}

/*
	def clusters() {
		clusterRelations.collect { it.cluster }
	}

	def advisors() {
		advisorRelations.collect { it.advisor }
	}
*/
