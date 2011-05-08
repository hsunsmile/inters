class Cluster { 

	static hasMany = [nodes:ClusterNode];

	String name = "gk.alab.ip.titech.ac.jp"
	double cpuInfo = 2022.432
	double totalMemory = 4048
	int numberOfNodes = 32
	// TODO: detection needed 
	int numberOfCorePerNode = 2
	double networkDelayTime = 0.05
	double newestPrice = 0.0 
	int runningJobNumbers = 0
	int waitingJobNumbers = 0
	int finishedJobNumbers = 0
	int failedJobNumbers = 0
	int multiSubmissionJobNumbers = 0
	int dummyJobNumbers = 0

	static constraints = {
		name(unique:true, nullable:false)
		numberOfNodes()
		numberOfCorePerNode()
		newestPrice()
		cpuInfo()
		totalMemory()
	}

	private def beforeInsert = {
		println "[DBG] $name:$newestPrice:$numberOfNodes:$cpuInfo is created. "
	}

	int calcTotalAvailableNodesNum() {
		println "[DBG] $name:$multiSubmissionJobNumbers:$dummyJobNumbers is tested. "
		numberOfNodes * numberOfCorePerNode - multiSubmissionJobNumbers - dummyJobNumbers
	}
	
	int calcAvailableNodesNumNow() {
		numberOfNodes * numberOfCorePerNode - runningJobNumbers
	}

	def decMultiSubmissionJobNumbers() {
		Cluster.executeUpdate( "update Cluster clust set " + 
				"clust.multiSubmissionJobNumbers = clust.multiSubmissionJobNumbers - 1 " + 
				"where clust.name='$name'" )
	}

	def decRunningJobNumbers() {
		Cluster.executeUpdate( "update Cluster clust set " + 
				"clust.runningJobNumbers = clust.runningJobNumbers - 1 " + 
				"where clust.name='$name'" )
	}

	def decDummyJobNumbers() {
		Cluster.executeUpdate( "update Cluster clust set " + 
				"clust.dummyJobNumbers = clust.dummyJobNumbers - 1 " + 
				"where clust.name='$name'" )
	}

	def incMultiSubmissionJobNumbers() {
		Cluster.executeUpdate( "update Cluster clust set " + 
				"clust.multiSubmissionJobNumbers = clust.multiSubmissionJobNumbers + 1 " + 
				"where clust.name='$name'" )
	}

	def incRunningJobNumbers() {
		Cluster.executeUpdate( "update Cluster clust set " + 
				"clust.runningJobNumbers = clust.runningJobNumbers + 1 " + 
				"where clust.name='$name'" )
	}

	def incDummyJobNumbers() {
		Cluster.executeUpdate( "update Cluster clust set " + 
				"clust.dummyJobNumbers = clust.dummyJobNumbers + 1 " + 
				"where clust.name='$name'" )
	}

	def incFinishedJobNumbers() {
		Cluster.executeUpdate( "update Cluster clust set " + 
				"clust.finishedJobNumbers = clust.finishedJobNumbers + 1 " + 
				"where clust.name='$name'" )
	}

	public String toString() {
		name //+ ":" + IP + ":" + numberOfNodes + ":" + cpuInfo
	}

	def mySave() {
		try {
			save(flush:true);
		}catch ( Exception e ) {
			try { 
				refresh();
			} catch ( Exception e1 ) {
				try { 
					merge(flush:true);
				} catch ( Exception e2 ) {
					try { 
						if( !isAttached() ) attach();
					} catch ( Exception e3 ) {

					}
				}
			}
		}
	}

}
