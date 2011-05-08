
import org.springframework.beans.factory.InitializingBean

class ServiceReferenceService implements InitializingBean {

	boolean transactional = false;
	public static references = [:];

	def grailsApplication;
	def advisorInfoService;
	def executorService;
	def ngClusterHandleService;
	def clusterInfoProviderService;
	def helperService;
	def ngExecutionInitializeService;
	def schedulerCoreService;
	def clusterService;
	def jobInfoService;
	def planService;
	def executionAdviceService;
	def policyProcessorService;
	def awsService;
	def sgeService;
	def sshExecutorService;
	def sgeExecutionInitializeService;
	def sgeClusterHandleService;

	void afterPropertiesSet() {
		references["advisorInfoService"] = advisorInfoService;
		references["executorService"] = executorService;
		references["ngClusterHandleService"] = ngClusterHandleService;
		references["clusterInfoProviderService"] = clusterInfoProviderService;
		references["helperService"] = helperService;
		references["ngExecutionInitializeService"] = ngExecutionInitializeService;
		references["schedulerCoreService"] = schedulerCoreService;
		references["clusterService"] = clusterService;
		references["jobInfoService"] = jobInfoService;
		references["planService"] = planService;
		references["executionAdviceService"] = executionAdviceService;
		references["policyProcessorService"] = policyProcessorService;
		references["awsService"] = awsService;
		references["sgeService"] = sgeService;
		references["sshExecutorService"] = sshExecutorService;
		references["sgeExecutionInitializeService"] = sgeExecutionInitializeService;
		references["sgeClusterHandleService"] = sgeClusterHandleService;
		println toString();
	}

	String toString() {
		def message = "ServiceReferenceService has references: ";
		references.each { message += "\n\t [${it.key}] -- ${it.value}"; }
		return message;
	}
}
