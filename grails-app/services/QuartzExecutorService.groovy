
import org.quartz.JobDetail
import org.quartz.TriggerUtils
import org.quartz.Trigger
import org.quartz.TriggerListener
import javax.jws.*

class QuartzExecutorService {

	boolean transactional = true;
	def quartzScheduler;

	/*
	   def jobDetail = new JobDetail( njobName, testJob.coreGroup, NgJob.class );
	   def jobDataMap = jobDetail.getJobDataMap();
	   jobDataMap.put( "JOBTYPE", "NgJob" );
	   jobDataMap.put( "JOB", testJob );
	   jobDataMap.put("ADVISORINFOSERVICE", advisorInfoService );
	   jobDataMap.put("SCHEDULERCORE", this );
	   def trigger = TriggerUtils.makeImmediateTrigger(0,0);
	   trigger.setName("NgJobCommonTrigger" + "$njobName#${aCluster.ident()}");
	   quartzScheduler.scheduleJob(jobDetail,trigger);
	 */
	def invokeJobImmediately( String jobType, long jobId ) {
		def jobDetail = new JobDetail("$jobType$jobId", jobType, "$jobType".class);
		def jobDataMap = jobDetail.getJobDataMap();
		jobDetailsMap.each { key, val -> jobDataMap.put("$key", val); }
		def trigger = TriggerUtils.makeImmediateTrigger(0,0);
		trigger.setName("$jobTypeTrigger" + "$jobType$jobId");
		quartzScheduler.scheduleJob(jobDetail, trigger);
	}

	def deleteJob() {
		def jobNames = quartzScheduler.getJobNames( "System" );
		if( jobNames.find { it == job.name } ) {
			log.info "  DELETE OLD JOB !! ${job}";
			quartzScheduler.deleteJob( job.name, "System" );
		}
	}

	def waitJob() {
		while( true ) {
			try {
				def njob = quartzScheduler.getJobDetail(job.name, job.coreGroup)?.
					getJobDataMap()?.get("JOB");
				if( njob ) job = njob;
				log.debug " [WAIT] for job $jobId -- $job ";
				def clonedJobs = jobInfoService.getClonedJobs( job.id );
				if( job.status == "finished" || 
						clonedJobs.find { it.status == "finished" } ) { break }
			} catch ( Exception e ) { log.error " [WAIT] $e " }
			sleep 20000;
		}
	}
}
