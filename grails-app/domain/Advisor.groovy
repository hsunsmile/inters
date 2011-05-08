class Advisor {

	String name = "MyAdvisor"
	String typeDetails = "PerformancePriori"
	int priority = -1
	int adviseSuccessPointRate = 20
	int adviseFailPointRate = 20
	int rankingRenewInterval = 2
	double costLimitation = 100
	double deadline = 300

	static constraints = {
		name(blank:false, unique:true)
		rankingRenewInterval(blank:false)
		adviseSuccessPointRate(blank:false)
		adviseFailPointRate(blank:false)
		costLimitation()
		deadline()
		priority()
	}

	String toString() { name }
}
