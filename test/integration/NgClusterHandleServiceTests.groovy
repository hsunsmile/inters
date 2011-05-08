class NgClusterHandleServiceTests extends GroovyTestCase {

	NgClusterHandleService ngClusterHandleService

	private def handleName = 'handle1'
	private def clusterName = 'testCluster'
	private def clusterName2 = 'testCluster2'
	private def handleId = -1
	private def handleId2 = -1

    void testAddHandle() {
		handleId = ngClusterHandleService.addHandle( clusterName, handleName )
		handleId2 = ngClusterHandleService.addHandle( clusterName, handleName )
		assert handleId == handleId2
		handleId2 = ngClusterHandleService.addHandle( clusterName , handleName + '2' )
		def handleMap = ngClusterHandleService.handlesDivedByCluster
		def handleList = handleMap[ clusterName ]
    	assert handleList.get(handleId2) == handleName + '2'
		handleId2 = ngClusterHandleService.addHandle( clusterName2 , handleName )
		def handleId3 = ngClusterHandleService.addHandle( clusterName2, handleName )
		assert handleId2 == handleId3
		handleList = handleMap[ clusterName2 ]
    	assert handleList.get(handleId2) != handleName + '2'
		assert handleList.get(handleId2) == handleName
	}
	
	void testgetHandle() {
		if( handleId < 0) testAddHandle()
		assert ngClusterHandleService.getHandle( clusterName, handleId ) == handleName
		assert ngClusterHandleService.getHandle( clusterName2, handleId2 ) == handleName
    }

}
