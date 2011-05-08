class NgJobTests extends GroovyTestCase {

    void testGetCluster() {
		def cluster = new Cluster(IP:"192.168.2.20", name:"gk.alab.ip.titech.ac.jp").save()
		def ngJob = new NgJob()
		try{
			ngJob.execute()
			assert ngJob.useCluster.toString() == "192.168.2.20"
		}catch(Exception e) {}
    }
}
