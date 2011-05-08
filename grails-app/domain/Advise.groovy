class Advise {

	static hasMany = [ jobIds:Long ];
	ADVISE_TYPE type = ADVISE_TYPE.EXECUTION_FAULT;
	ADVISE_STATUS status = ADVISE_STATUS.PROPOSED;

	def planService() { return ServiceReferenceService.references.planService; }

    long clusterId;

    boolean isSelected = false;
    boolean isAvailable = true;
    boolean isDiscarded = false;
    String planType = "NoTypeNow";
	String advisorName = "NoAdvisor";
	String planFor = "performance|faulty";

    String toString() {
        "[id:$id][jobs:$jobIds][clst:$clusterId][status:$status][type:$planType]"
    }

    def createAdviceFile( rootDir = "advices" ) {
        def ids =  planService().makePlanForFaultTolerantAdvice( id ); 
        println "\n\n\t createAdviceFile ids: $ids\n"
        ids.each { planId ->
            def plan = Plan.get( planId )
            def totalCost = plan.totalCost;
        	def calcTime = plan.calcTime;
            def details = plan.resourceslist
            def migrate_contents = details.collect {
            """
              # jobs 
              ${it.jobIds}
              # migrate to: 
              ${it.cluster.name}
            """
            }.join("\n")
            println "\t createAdviceFiles contents: $migrate_contents\n"
            def save_file = new File("${rootDir}/${id}_advice.txt")
            def advice_text = """
              Advice for job 
              Plan: ${planType}
              -----------------
              # advice number
              ${id}
              # migrate from:
              ${Cluster.get(clusterId).name}
              ${migrate_contents}
              # plan
              Time: ${calcTime}, Cost: ${totalCost}
              -----------------
              """
              println "\t createAdviceFiles text: $advice_text\n"
			  // TODO: whether to use save_file.append ?
              save_file.write( advice_text )
              if( !save_file.exists() ) save_file.createNewFile(); 
        }
    }
}

enum ADVISE_TYPE { EXECUTION_FAULT, CLUSTER_PERFORMANCE_DOWN, USER_CHANGE_CLUSTER }
enum ADVISE_STATUS { PROPOSED, SELECTED, DISCARDED }
