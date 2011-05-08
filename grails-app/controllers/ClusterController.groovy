            
class ClusterController {
	
	def schedulerCoreService
	def clusterService
	def scaffold = Cluster    

	def save = { 
		def cluster = new Cluster(params) 
		if(!cluster.hasErrors() && cluster.save()) { 
			flash.message = "Cluster ${cluster.id} created" 
			schedulerCoreService.addCluster(cluster)
			redirect(action:show,id:cluster.id)
		} 
		else { 
			render(view:'create',model:[cluster:cluster]) 
		} 
	} 

	def delete = { 
		def cluster = Cluster.get( params.id ) 
		if(cluster) { 
			cluster.delete() 
			flash.message = "Cluster ${params.id} deleted" 
			clusterService.removeCluster( params.id )
			redirect(action:list) 
		} 
		else { 
			flash.message = "Cluster not found with id ${params.id}" 
			redirect(action:list) 
		} 
	} 
}
