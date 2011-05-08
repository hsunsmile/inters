
import grails.converters.*

class AdviseController {

	def scaffold = Advise
	def advisorInfoService
	def executionAdviceService() { return ServiceReferenceService.references.executionAdviceService; }

	def listAdvise = {
		def jsonResp = Advise.list().encodeAsJSON()
		log.info "jsonResp: $jsonResp"
		render jsonResp
	}
	
	def list = {
		redirect( controller:"job",action:"advise" )
	}

	def useAdvise = {
		def ids = params.ids, theId = params.advise.id;
		if( !(ids || theId) ) { println "[DBG] $ids --- $theId is null"; return }
		if( theId ) { if(!ids) ids = []; ids << theId }
		ids = ids.collect { it.toLong() }
		log.info "use advises: $ids";
		executionAdviceService().useAdvices(ids);
		render "$ids are used."
	}

	def useAdvise_old = {
		def ids = params.ids, theId = params.advise.id
			if( !(ids || theId) ) { println "[DBG] $ids --- $theId is null"; return }
		def advises = []
			Cluster.list().each { aCluster ->
				advisorInfoService.fetchRunningJobListByCluster( aCluster ).each { job ->
					if( job.advise ) advises << job.advise
				}
			}
		if( theId ) { if(!ids) ids = []; ids << theId }
		ids.each { aId ->
			def theAdvise = advises.find { anAdvise -> anAdvise.ident() == aId.toInteger() }
			if( theAdvise ) {
				theAdvise.used = true; theAdvise.status = "accepted"; theAdvise.mySave();
				println "[INF] $theAdvise is used"
			}
		}
		render "$ids $theId is used."
	}
}
