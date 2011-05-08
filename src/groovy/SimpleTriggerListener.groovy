
import org.quartz.JobExecutionContext
import org.quartz.Trigger
import org.quartz.TriggerListener

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class SimpleTriggerListener implements TriggerListener {

	def logger = LogFactory.getLog(SimpleTriggerListener.class)	
	private String name

	public SimpleTriggerListener(String name) {
		this.name = name
	}

	public String getName() {
		return name
	}

	public void triggerFired(Trigger trigger, JobExecutionContext context) { }

	public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) { }

	public void triggerMisfired(Trigger trigger) { }

	public void triggerComplete(Trigger trigger, JobExecutionContext context, int triggerInstructionCode) {
		def jobtype = context.getJobDetail().getJobDataMap().getString("JOBTYPE")
		switch(jobtype) {
			case 'NgJob':
				def id = context.getJobDetail().getJobDataMap().getInt("ID")
				NgJobComplete(id)
				break
			case 'SystemAdvisor':
				def advisor = context.getJobDetail().getJobDataMap().get("ADVISOR")
				logger.info( advisor.advisorName + " Finished !! ")
				break
		}
	}

	private void NgJobComplete( id ) {
		def jobRecord = Job.get(id)
		if(jobRecord) {
			jobRecord.executionFinishTime = new Date() 
			jobRecord.status = "finished"
			jobRecord.executedTimes++;
			jobRecord.save()
		}
		logger.info( id + " Job Finished !!")
	
	}
}

