class Plan { 

	static hasMany = [ resourceslist:ClusterPlanDetails ]
	
	String type = "PlanType";
	String planForAdvice = "KindOfAdvice";
	String advisorName = "KindOfAdvisor";
	double totalCost = (-1.0);
	long calcTime;
	boolean useThisPlan = false;
	boolean planIsOld = false;

	def Date estimatedFinishTime() {
		new Date((new Date()).time + calcTime*1000);
	}

	String toString() {
		def result = "[$type][$id][$version][use:$useThisPlan][old:$planIsOld]";
		result += "\n\t -fin:${estimatedFinishTime()}, cost:${totalCost}";
		resourceslist?.each { result += "\n\t -$it "; }
		return result;
	}
}
