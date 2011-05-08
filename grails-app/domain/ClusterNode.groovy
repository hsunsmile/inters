class ClusterNode {

	// static belongsTo = Cluster;

	String clusterName = "kuruwa-gw.alab.nii.ac.jp";
	String name = "";
	String ipAddress = "";
	String instanceId = "";
	String isEC2 = false;
	Integer numOfCores = 2;

    static constraints = {
    }
}
