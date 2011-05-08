
class PlanService {

	boolean transactional = true;
	def grailsApplication;

	def jobInfoService() { return ServiceReferenceService.references.jobInfoService; }
	def advisorInfoService() { return ServiceReferenceService.references.advisorInfoService; }
	def executorService() { return ServiceReferenceService.references.executorService; }
	def policyProcessorService() { return ServiceReferenceService.references.policyProcessorService; }
	def helperService() { return ServiceReferenceService.references.helperService; }

	def deletePlan( planIds ) {
		log.info "delete plans with ids: ${planIds}";
		planIds.each { 
			def plan = Plan.get(it); 
			try {
				plan.delete();
			} catch( Exception e ) { 
				println "Can not delete plan $plan. $e"; 
			}
		}
	}
	
	def makeSGEPlan( priorKind, advisorName, clusters, jobs=null ) {
		def assignJobList = [];
		def fullJobList = (jobs)? jobs:jobInfoService().getJobsNeedAdvice( advisorName );
		fullJobList.each { assignJobList << it; }
		log.info "makeSGEPlan: make plans for sge jobs. ${assignJobList}"
		def planContents = [];
		while ( assignJobList.size() ) {
			clusters.each { clusterInfo ->
				clusterInfo.num.times {
					def aJob = ( assignJobList.size() >0 )? assignJobList.remove(0) : null;
					if( aJob ) planContents << [ cluster:clusterInfo.cluster.name, job:aJob ];
				}
			}
		}
		log.info "makeSGEPlan: planContents for sge jobs. ${planContents}"
		def plans = [];
		clusters.each { clusterInfo ->
			def jobsList = [];
			planContents.each { if(it.cluster == clusterInfo.cluster.name) jobsList << it.job }
			plans << [ cluster:clusterInfo.cluster, jobs:jobsList, advisorName:advisorName ];
		}
		log.info "makeSGEPlan: planContents for sge jobs. ${plans}"
		return makeSGEPlanDetails( priorKind, plans );
	}

	//TODO: add queuingTime calculation
	def makePlan( priorKind, advisorName, clusters, jobs=null ) {
		def assignJobList = [];
		def fullJobList = (jobs)? jobs:jobInfoService().getJobsNeedAdvice( advisorName );
		fullJobList.each { assignJobList << it; }
		// log.info "make[$priorKind]Plan with ${clusters}\n[jobs:${assignJobList}]";
		log.info "make[$advisorName][$priorKind]Plan";
		def planContents = [];
		while ( assignJobList.size() ) {
			clusters.each { clusterInfo ->
				clusterInfo.num.times {
					// def aJobId = ( assignJobList.size() >0 )? assignJobList.remove(0) : null;
					// log.info " \t get Jobid: $aJobId for ${clusterInfo.cluster.name}";
					// def aJob = fullJobList.find{ it.jid == aJobId };
					def aJob = ( assignJobList.size() >0 )? assignJobList.remove(0) : null;
					if( aJob ) planContents << [ cluster:clusterInfo.cluster.name, job:aJob ];
				}
			}
		}
		log.info "plan is: ${helperService().printHelper(planContents,2,"\n\tPlan is:")}";
		def plans = [];
		clusters.each { clusterInfo ->
			def jobsList = [];
			planContents.each { if(it.cluster == clusterInfo.cluster.name) jobsList << it.job }
			plans << [ cluster:clusterInfo.cluster, jobs:jobsList, advisorName:advisorName ];
		}
		log.info "plan is: ${helperService().printHelper(plans,2,"\n\tPlanDetail is:")}";
		return makePlanDetails( priorKind, plans );
	}

	def makePlanDetails( priorKind, plans ) {
		def aPlan = new Plan( type:priorKind ), details = []; 
		double allCost = 0; long longestTime = 0;
		plans.each {
			def totalCalcCost = calculateCost( it.jobs, it.cluster );
			log.debug "\t ${it.cluster.name} totalcost is: " + 
				"$totalCalcCost -- ${ new Date() } ";
			int numberOfCores = 
				it.cluster.numberOfNodes * it.cluster.numberOfCorePerNode;
			int numberOfUsingCores = 
				(it.jobs.size() > numberOfCores )? numberOfCores : it.jobs.size();
			long calcTime = 
				totalCalcCost / (it.cluster.cpuInfo * numberOfUsingCores);
			log.debug "\t ${totalCalcCost} " + 
				"${it.cluster.cpuInfo} ${numberOfUsingCores} -- ${ new Date() } ";
			double calcCost = 
				calcTime * it.cluster.newestPrice * numberOfUsingCores;
			log.debug "\t ${it.cluster.name} " + 
				"calcCost is: $calcCost -- ${ new Date() } ";
			if ( calcTime >= longestTime ) longestTime = calcTime;
			allCost += calcCost;
			def detail = new ClusterPlanDetails( cluster: it.cluster, plan: aPlan, numberOfJobs:it.jobs.size());
			aPlan.planForAdvice = it.advisorName;
			detail.jobIds = it.jobs;
			details << detail;
		}
		aPlan.totalCost = allCost;
		aPlan.calcTime = longestTime;
		policyProcessorService().initPolicyInfoProvider( grailsApplication.config.classad.config.file );
		// TODO: use this def costLimit = policyProcessorService().showCostLimitation();
		def costLimit = 100000000;
		if( allCost <= costLimit ) {
			aPlan.save();
			details.each { aDetail -> aPlan.addToResourceslist( aDetail ).save(); }
			log.info "[RES] $priorKind -- " + 
				"time:$longestTime price:$allCost <= costLimit:$costLimit "
		} else {
			aPlan.save(); 
			details.each { aDetail -> aPlan.addToResourceslist( aDetail ).save(); }
			log.info "[WRN] $priorKind -- " + 
				"time:$longestTime price:$allCost > costLimit:$costLimit "
		}
		log.info "[RES] $details ";
		return aPlan.id;
	}

	def makeSGEPlanDetails( priorKind, plans ) {
		def aPlan = new Plan( type:priorKind ), details = []; 
		double allCost = 0; long longestTime = 0;
		plans.each {
			def totalCalcCost = calculateCost( it.jobs, it.cluster );
			log.debug "\t ${it.cluster.name} totalcost is: " + 
				"$totalCalcCost -- ${ new Date() } ";
			int numberOfCores = 
				it.cluster.numberOfNodes * it.cluster.numberOfCorePerNode;
			int numberOfUsingCores = 
				(it.jobs.size() > numberOfCores )? numberOfCores : it.jobs.size();
			long calcTime = 
				totalCalcCost / (it.cluster.cpuInfo * numberOfUsingCores);
			log.debug "\t ${totalCalcCost} " + 
				"${it.cluster.cpuInfo} ${numberOfUsingCores} -- ${ new Date() } ";
			double calcCost = 
				calcTime * it.cluster.newestPrice * numberOfUsingCores;
			log.debug "\t ${it.cluster.name} " + 
				"calcCost is: $calcCost -- ${ new Date() } ";
			if ( calcTime >= longestTime ) longestTime = calcTime;
			allCost += calcCost;
			def detail = new ClusterPlanDetails( cluster: it.cluster, plan: aPlan, numberOfJobs:it.jobs.size());
			aPlan.planForAdvice = it.advisorName;
			detail.jobIds = it.jobs;
			details << detail;
		}
		aPlan.totalCost = allCost;
		aPlan.calcTime = longestTime;
		policyProcessorService().initPolicyInfoProvider( grailsApplication.config.classad.config.file );
		// TODO: use this: def costLimit = policyProcessorService().showCostLimitation();
		def costLimit = 10000000;
		if( allCost <= costLimit ) {
			aPlan.save();
			details.each { aDetail -> aPlan.addToResourceslist( aDetail ).save(); }
			log.info "[RES] $priorKind -- " + 
				"time:$longestTime price:$allCost <= costLimit:$costLimit "
		} else {
			aPlan.save(); 
			details.each { aDetail -> aPlan.addToResourceslist( aDetail ).save(); }
			log.info "[WRN] $priorKind -- " + 
				"time:$longestTime price:$allCost > costLimit:$costLimit "
		}
		log.info "[RES] $details ";
		return aPlan.id;
	}

	def calculateCost( jobsList, cluster ) {
		def jobsByFunc = [];
		def costs = jobInfoService().getJobExecutionCosts();
		RemoteFunction.findAllWhere(isTest:false).each { func -> 
			def sum = 0, num =0;
			costs[func.name].each {
				log.debug "$it, ${it.key}, ${it.value}";
				if(it.key == cluster.name){ sum = it.value.sum(); num = it.value.size(); }
			}
			if( num == 0 ) num++;
			log.debug "${func.name} in $cluster: ${sum} ${num} ${sum/num}";
			def nums = jobsList.findAll{jobInfoService().getFunctionName(it) == func.name}.size();
			jobsByFunc << [ sum/num, cluster.cpuInfo, nums ];
		}
		log.debug helperService().printHelper(jobsByFunc,5,"\n\tJobsByFunc: ");
		double totalCalcCost = 0; 
		jobsByFunc.each { assert it.size() == 3; totalCalcCost += it[0] * it[1] * it[2] }
		return totalCalcCost;
	}

	def fetchAllCluster( clusterList=null ) {
		def allCluster = [];
		def useClusters = clusterList? clusterList : Cluster.list();
		useClusters.each { aCluster ->
			log.info "\t\t fetchAllCluster ... now test [$aCluster] ";
			def entry = [:]; entry[ 'cluster' ] = aCluster;
			def limit = aCluster.calcTotalAvailableNodesNum();
			//TODO:use jobInfoService()
			def runningJobs = advisorInfoService().fetchRunningJobListByCluster( aCluster );
			def handleSize = limit - runningJobs.size(); 
			// entry[ 'num' ] = handleSize;
			entry[ 'num' ] = limit;
			log.info "\t\t [${aCluster}][lim:${limit}]" + 
				"[run:${runningJobs.size()}][num:$handleSize && num>0 is prefered]";
			def handleStatus = runningJobs.collect { jobId ->
				def job = Job.get( jobId );
				def est = job.exectionEstimateTime();
				est = (est.after(new Date()))? est : new Date() + 7;
				[ 'job':job, 'est':est ];
			}
			// def dummyJob = 
			// new Job( name:'waitingForJob', cluster: aCluster, executionTime:0 )
			// handleSize.times { handleStatus << [ 'job':null, 'est': new Date() ] }
			entry[ 'handleStatus' ] = handleStatus.sort { it.est };
			allCluster << entry;
		}
		return allCluster;
	}

	def getBestPlanTypes() {
		getSupportedPlanTypes();
	}

	def getSupportedPlanTypes() {
		policyProcessorService().getCustomizedSchedulingPlan().each { planType ->
			def planName = planType.key, clusterOrder = planType.value;
		}
	}

	def getCustomizedPlanTypes() {
		policyProcessorService().getCustomizedSchedulingPlan().collect { it.key }
	}

	def makePlanForFaultTolerantAdvice( long adviseId ) {
		def planIds = [];
		def theAdvise = Advise.get( adviseId );
		def toClusters = [];
		Cluster.list().each { cluster ->
			if( cluster.id != theAdvise.clusterId ) toClusters << cluster
		}
		def toClustersInfo = fetchAllCluster(toClusters);
		// theAdvise.toClusterIds.each { toClusters << Cluster.get(it); }
		def moveJobs = theAdvise.jobIds;
		// def definedPlanTypes = policyProcessorService().showPlanType("planTypeForFaultTolerant");
		def definedPlanTypes = false;
		// def planTypes = (definedPlanTypes)? definedPlanTypes:getBestPlanTypes();
		def planTypes = [ "performancePrior" ];
		def logMsg = "makePlanForFaultTolerantAdvice[id:$adviseId] $planTypes";
		planTypes.each { planType ->
			logMsg += "\n\tforPlanType:$planType";
			def _toClusters = getClusterRanking( planType, toClustersInfo );
			planIds << makePlan( planType, theAdvise.advisorName, _toClusters, moveJobs );
			logMsg += "\n\tuse clusters:$toClusters \n\tPlan.ids:$planIds";
		}
		log.info logMsg;
		return planIds;
	}

	def makePlanForClusterPerformanceDownAdvice( long adviseId ) {
		def planIds = [];
		def theAdvise = Advise.get( adviseId );
		def toClusters = [];
		Cluster.list().each { cluster ->
			if( cluster.id != theAdvise.clusterId ) toClusters << cluster
		}
		def toClustersInfo = fetchAllCluster(toClusters);
		// theAdvise.toClusterIds.each { toClusters << Cluster.get(it); }
		def moveJobs = theAdvise.jobIds;
		def definedPlanTypes = policyProcessorService().showPlanType("planTypeForPerformanceDownClusters");
		def planTypes = (definedPlanTypes)? definedPlanTypes:getBestPlanTypes();
		def logMsg = "makePlanForClusterPerformanceDownAdvice[id:$adviseId] $planTypes";
		planTypes.each { planType ->
			logMsg += "\n\tforPlanType:$planType";
			def _toClusters = getClusterRanking( planType, toClustersInfo );
			planIds << makePlan( planType, theAdvise.advisorName, _toClusters, moveJobs );
			logMsg += "\n\tuse clusters:$toClusters \n\tPlan.ids:$planIds";
		}
		log.info logMsg;
		return planIds;
	}

	def executePlanById( long planId, boolean migrate = true ) {
		def thePlan = Plan.get( planId );
		if( thePlan )  executePlan( thePlan.resourceslist, 0, "PlanMaking", migrate );
		if( !thePlan ) log.error "executePlan id:$planId not exist";
	}

	def executePlan( resources, selectionTime, planType = "PlanMaking", boolean migrate = false ) {
		def fullJobList = jobInfoService().getJobsNeedAdvice( planType );
		def message = "execute plan $resources with jobs $fullJobList ";
		if( fullJobList.size() == 0 ) {
			message += "\n can not find queuing jobs to execute. migrate? $migrate";
			log.info message;
			if(!migrate) return
		}
		fullJobList = resources.collect { details -> details.jobIds }.flatten();
		message += "\nexecute plan $resources with jobs $fullJobList ";
		def index=0, totaljobsNum=fullJobList.size();
		resources.each { details -> 
			message += "\nexecute plan: $details";
			details.numberOfJobs.times {
				message += "\n\t for the $it times assign. [idx:$index]";
				// log.info message;
				def aJobId = (index<totaljobsNum)? fullJobList.getAt(index):null;
				message += "\n got job: $aJobId ";
				index++; 
				message += "\nassign $aJobId[at:$index] to ${details.cluster}";
				// log.info message;
				if( aJobId ) {
					(migrate)?  migrateToCluster( aJobId, details.cluster, selectionTime ) :
						assignCluster( aJobId, details.cluster, selectionTime );
				}
			}
		}
		// log.info message;
	}

	def assignCluster( long jobId, Cluster cluster, selectionTime=0 ) {
		jobInfoService().updateJob( jobId, [cluster:cluster, interactiveTime:selectionTime] );
		def job = Job.get( jobId );
		advisorInfoService().renewJobMap( jobId );
		log.debug ( job.cluster.name != cluster.name )?
			"assign ${job} to ${cluster} failed. " : "assign ${job} to ${cluster} ok. ";
		jobInfoService().executeJob( jobId );
	}

	def migrateToCluster( long jobId, Cluster cluster, selectionTime=0 ) {
		try {
			jobInfoService().cancelJob( jobId );
		} catch( Exception e ) {
			println "migrateToCluster $jobId to $cluster error: $e";
		}
		assignCluster( jobId, cluster, selectionTime );
	}

	def getSelectedPlan() {
		def thePlan = Plan.list().find { it.refresh(); it.useThisPlan && !it.planIsOld }
		if( !thePlan ) {
			log.info "Plan need to be selected\n" + 
				"${helperService().printHelper(Plan.list(),1,"\nPlan is: ")}";
			return false;
		}
		log.info "${thePlan} \n${thePlan.useThisPlan} ${thePlan.planIsOld}";
		def resourceslist = thePlan.resourceslist;
		log.debug helperService().printHelper(resourceslist,2,"\n\tDetail is: ");
		try {
			def theOldPlan = Plan.list().find { it.refresh(); it.useThisPlan && it.planIsOld }
			theOldPlan?.delete();
			thePlan.planIsOld = true; 
			thePlan.save(flush:true); 
			Plan.list().each { it.refresh(); if( it.id != thePlan.id ) { it.delete(); } }
		} catch( Exception e ) {
			log.error("plan selection failed",e);
		}
		log.debug helperService().printHelper(resourceslist,2,"\n\tDetail is: ");
		return resourceslist;
	}

	def getClusterRanking( String priorKind, allClusters ) {
		def clusterInfos;
		switch( priorKind ) {
			case 'costPrior':
				clusterInfos = costFirstPlan( allClusters );
				break;
			case 'performancePrior':
				clusterInfos = performanceFirstPlan( allClusters );
				break;
			case 'costPerformancePrior':
				clusterInfos = costPerformanceFirstPlan( allClusters );
				break;
			default:
				clusterInfos = useCustomerPlan( priorKind, allClusters );
		}
		return clusterInfos;
	}

	def useCustomerPlan( planType, allClusters ) {
		def result = []
		policyProcessorService().getCustomizedSchedulingPlan().each { userPlan ->
			if( userPlan.key == planType ) {
				userPlan.value.each { clusterName ->
					result << allClusters.find { it.cluster.name == clusterName }
				}
			}
		}
		println "useCustomerPlan: $planType --> ${result}";
		return result;
	}

	def costFirstPlan( allClusters ) {
		def result = allClusters.sort { ((double)it.cluster.newestPrice) }
		// log.info "costFirstPlan clusterList: $result";
		return result;
	}

	//TODO: calculate by real data
	def performanceFirstPlan( allClusters ) {
		def result = allClusters.sort { -((double)it.cluster.cpuInfo *
				it.cluster.numberOfNodes * 
				it.cluster.numberOfCorePerNode) }
		// log.info "performanceFirstPlan clusterList: $result";
		return result;
	}

	def costPerformanceFirstPlan( allClusters ) {
		def result = allClusters.sort { -((double)it.cluster.cpuInfo / 
				it.cluster.newestPrice) }
		// log.info "costPerformanceFirstPlan clusterList: $result";
		return result;
	}

}
