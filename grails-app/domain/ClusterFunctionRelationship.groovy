
class ClusterFunctionRelationship { 

	// def ngClusterHandleService
	// private static schedulerCoreService
	// static transients = ['ngClusterHandleService']
	static belongsTo = Client
	static optionals = [ 'newHandleDetectInterval' ]

	def newHandleDetectInterval = 20 

	Cluster cluster
	RemoteFunction function
	Client client

	static constraints = {
		cluster()
		function()
	}

	private def beforeInsert = {
		println " ${this.class} -- $cluster and $function are linked "
	}

	String toString() {
		cluster?.name + " --  " + function?.name
	}

}

/*
	static ClusterFunctionRelationship link( cluster, function ) {
		def r = ClusterFunctionRelationship.findByClusterAndFunction( cluster, function )
		if(!r) {
			r = new ClusterFunctionRelationship( cluster:cluster, function:function )
			cluster?.addToFunctionRelations(r)
			function?.addToClusterRelations(r)
			r.save()
		}
		return r
	}

	static void unlink( cluster, function ) {
		def r = ClusterFunctionRelationship.findByClusterAndFunction( cluster, function )
		if(r) {
			cluster?.removeFromFunctionRelations(r)
			function?.removeFromClusterRelations(r)
			r.delete()
		}
	}
*/

