class ClusterPlanDetails {

	static belongsTo = [ Cluster, Plan ]
	static hasMany = [ jobIds:Long ]

	Cluster cluster
	Plan plan
	int numberOfJobs = 0

	String toString() {
		return "${cluster?.name}($numberOfJobs/${cluster?.numberOfNodes * cluster?.numberOfCorePerNode}) "
	}

}
