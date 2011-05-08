package sge

class JobWatcherJob {

	def timeout = 30000l;
	def serviceReferenceService;
	def jobInfoService() { serviceReferenceService.references.jobInfoService; }
	def awsService() { serviceReferenceService.references.awsService; }
	def sgeService() { serviceReferenceService.references.sgeService; }

    def execute() {
		// TODO: do not delete finished jobs !
		def finjobs = jobInfoService().getFinishedTestJobs();
		if( finjobs ) {
			println "sge finTestJobs: $finjobs ${new Date()}";
			def jobs = jobInfoService().getQueuingJobsInCluster(1,true);
			println "sge queuing jobs: $jobs ${new Date()}";
			if( jobs ) {
				def _finjob = jobInfoService().getCurrentJob(finjobs.pop());
				println "sge _finJob: $_finjob";
				def executionTime = _finjob.executionFinishTime.time - _finjob.executionStartTime.time;
				executionTime = executionTime/1000;
				def totalTime = executionTime * jobs.size();
				def numOfNodes = totalTime/(3600*4) + 1;
				println "sge queuing jobs:$jobs -- t:$totalTime -- n:$numOfNodes";
				numOfNodes.times {
					def num = sgeService().getClusterNodes().size() + 4;
					if( num < 10 ) {
						def nodeName = "kuruwa${sprintf("%02d",num)}";
						println "add sge node: $nodeName";
						// TODO: auto detect numOfcores
						sgeService().addNode([hostname:nodeName, isEC2:false, numOfCores:4]);
					}
				}
			}
		}
	}
}
