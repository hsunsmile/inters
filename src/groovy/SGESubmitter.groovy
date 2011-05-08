
class SGESubmitter {

	def submit(SchedulerCoreService scheduler, long clientId) {
		def opts = [ remoteCommand:"/home/xsun/inters-ec2/nlp2/nlp.sh", outputPath:":/inters-results",
			errorPath:":/inters-results", workingDirectory:"/home/xsun/inters-ec2/nlp2",
			jobName:"nlp-test" ];
		def sge_func = scheduler.addSGEFunction( function:"nlp-test", advisor:'SGEPlanMaking', opts:opts );
		def kuruwa = new Cluster( name:"kuruwa-gw.alab.nii.ac.jp", 
				numberOfNodes:100, numberOfCorePerNode:4, cpuInfo:2889.32 ).save();
		def nodeInfo = [ name:"kuruwa03", isEC2:false, numOfCores:4 ];
		new ClusterNode( nodeInfo ).save();
		1.upto(20) { seq ->
			def result = new String[1];
			def args = [ "news20.binary" ];
			def jobId = scheduler.addSGEJob( clientId, sge_func, args, result );
			println "submit job${jobId} *${seq}*";
		}
		scheduler.waitAll();
	}

}
