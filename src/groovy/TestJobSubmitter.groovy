
class TestJobSubmitter {

	def _scheduler
	def _client

	def TestJobSubmitter(SchedulerCoreService scheduler, Client client ) {
		_scheduler = scheduler
		_client = client 
	}

	def submit() {
		//def gk = new Cluster( name:"gk.alab.ip.titech.ac.jp", 
		//		 numberOfNodes:4, cpuInfo:1089.32 ).save()
		// def gs = new Cluster( name:"gs.alab.ip.titech.ac.jp", 
		//			numberOfNodes:3, numberOfCorePerNode:2, cpuInfo:2112.32 ).save()
		def kuruwa = new Cluster( name:"kuruwa-gw.alab.nii.ac.jp", 
				numberOfNodes:2, numberOfCorePerNode:4, cpuInfo:2889.32 ).save()

		def submitThread = new Thread() {

			def filenamebase = "/home/xsun/working/testPro/useCG/cgTestFile/data"
			def classTypeList = [ 3 ]
			1.upto(2) { seq ->
				try {
					def result = new String[1]
						def jobId
						def classType = classTypeList.remove(0) 
						if( seq % 2 == 0 ) {
							def filename = filenamebase + [ 's','w','a','b' ].get( classType-1 )
								jobId = _scheduler?.addTestJob(_client, 
										'useNetCG/cg_main', filename, classType, seq, result)
						} else {
							jobId = _scheduler?.addTestJob(_client, 'useCPUEP/ep_main', classType, seq, result)
						}
						classTypeList << classType
						def jobThread = new Thread() {
							_scheduler?.waitJob(jobId)
								println " [RES] ${jobId} *${new Date()}* $result "
						}
					jobThread.start()
				}catch ( Exception e ) { println "+++"; e.printStackTrace() }
			}
			// _scheduler?.shutdown()
		}
		submitThread.start()
	}

}
