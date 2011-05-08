
class Submitter {

	def _scheduler
	def _client

	def Submitter(SchedulerCoreService scheduler, Client client ) {
		_scheduler = scheduler
		_client = client 
	}

	def submit() {

		def pm = new Advisor( name:'PropertyMatch' ).save(flush:true)
		def wa = new Advisor( name:'WaitAny' ).save(flush:true)
		def useCPU = new RemoteFunction( name:'useCPUEP/ep_main', advisor:wa ).save()
		def useNet = new RemoteFunction( name:'useNetCG/cg_main', advisor:wa ).save()
		def filenamebase = "/home/xsun/working/testPro/useCG/cgTestFile/data"
		def classTypeList = [ 3,3 ]

		def submitThread = new Thread() {
			1.upto(16) { seq ->
				try {
						def result = new String[1]
						def jobId
						def classType = classTypeList.remove(0) 
						if( seq % 2 < 0 ) {
							def filename = filenamebase + [ 's','w','a','b' ].get( classType-1 )
								jobId = _scheduler?.addJob(_client, 
										useNet, filename, classType, seq, result)
						} else {
							jobId = _scheduler?.addJob(_client, useCPU, classType, seq, result)
						}
						classTypeList << classType
						def jobThread = new Thread() {
							_scheduler?.waitJob(jobId)
								println " [RES] ${jobId} *${new Date()}* $result "
						}
						jobThread.start()
				}catch ( Exception e ) { e.printStackTrace() }
			}
			_scheduler?.shutdown()
		}
		submitThread.start()
	}

}
