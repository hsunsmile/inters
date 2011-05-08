
class TestPlanSubmitter {

	TestPlanSubmitter() {}

	def submit( schedulerCoreService, clientId ) {
		def useCPU = schedulerCoreService.
			addFunction( function:'useCPUEP/ep_main', advisor:'PlanMaking' );
		// def useNet = schedulerCoreService.
		//	addFunction( function:'useNetCG/cg_main', advisor:'PlanMaking' );

		def filenamebase = "/home/xsun/working/testPro/useCG/cgTestFile/data";
		def classTypeList = [ 2 ];
		def results = [];
		try {
			1.upto(24) { seq ->
				def result = new String[1];
				results << result;
				def jobId;
				def classType = classTypeList.remove(0);
				if( seq % 2 < 0 ) {
					def filename = filenamebase + [ 's','w','a','b' ].get( classType-1 );
					//jobId = schedulerCoreService?.addJob(_client, 
					//		useNet, filename, classType, seq, result);
				} else {
					jobId = schedulerCoreService.
						addJob(clientId, useCPU, classType, seq, result);
				}
				classTypeList << classType;
			}
			schedulerCoreService.waitAll();
			results.each { println "[${new Date()}] [FIN] result $it "; }
			// schedulerCoreService.shutdown();
		} catch ( Exception e ) {
			println "TestPlanSubmitter Error: "; e.printStackTrace() 
		}
	}
}

/*
   def jobThread = new Thread() {
   schedulerCoreService?.waitJob(jobId)
   println " [RES] ${jobId} *${new Date()}* $result "
   }
   jobThread.start()
 */

