            
class ClusterPlanDetailsController {
    
    def index = { redirect(action:list,params:params) }

    // the delete, save and update actions only accept POST requests
    static def allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        if(!params.max) params.max = 10
        [ clusterPlanDetailsList: ClusterPlanDetails.list( params ) ]
    }

    def show = {
        [ clusterPlanDetails : ClusterPlanDetails.get( params.id ) ]
    }

    def delete = {
        def clusterPlanDetails = ClusterPlanDetails.get( params.id )
        if(clusterPlanDetails) {
            clusterPlanDetails.delete()
            flash.message = "ClusterPlanDetails ${params.id} deleted"
            redirect(action:list)
        }
        else {
            flash.message = "ClusterPlanDetails not found with id ${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def clusterPlanDetails = ClusterPlanDetails.get( params.id )

        if(!clusterPlanDetails) {
            flash.message = "ClusterPlanDetails not found with id ${params.id}"
            redirect(action:list)
        }
        else {
            return [ clusterPlanDetails : clusterPlanDetails ]
        }
    }

    def update = {
        def clusterPlanDetails = ClusterPlanDetails.get( params.id )
        if(clusterPlanDetails) {
            clusterPlanDetails.properties = params
            if(!clusterPlanDetails.hasErrors() && clusterPlanDetails.save()) {
                flash.message = "ClusterPlanDetails ${params.id} updated"
                redirect(action:show,id:clusterPlanDetails.id)
            }
            else {
                render(view:'edit',model:[clusterPlanDetails:clusterPlanDetails])
            }
        }
        else {
            flash.message = "ClusterPlanDetails not found with id ${params.id}"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def clusterPlanDetails = new ClusterPlanDetails()
        clusterPlanDetails.properties = params
        return ['clusterPlanDetails':clusterPlanDetails]
    }

    def save = {
        def clusterPlanDetails = new ClusterPlanDetails(params)
        if(!clusterPlanDetails.hasErrors() && clusterPlanDetails.save()) {
            flash.message = "ClusterPlanDetails ${clusterPlanDetails.id} created"
            redirect(action:show,id:clusterPlanDetails.id)
        }
        else {
            render(view:'create',model:[clusterPlanDetails:clusterPlanDetails])
        }
    }
}
