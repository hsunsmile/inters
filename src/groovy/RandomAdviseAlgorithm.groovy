
class RandomAdviseAlgorithm implements AdviseAlgorithm {

	def advisorInfoService
	boolean disposeHandle = false
	boolean disposeSharedHandle = false

	def setBestCluster( Advisor advisor ) {
		def job = advisorInfoService.fetchJob( advisor )
		advisorInfoService.getRandomCluster( job )
		job.execute( disposeHandle, disposeSharedHandle )
	}

}

