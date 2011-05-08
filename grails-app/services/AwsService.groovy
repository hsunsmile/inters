
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

class AwsService {
	
	static def ec2;
	static def sgeService(){ ServiceReferenceService.references.sgeService; }

	static def init() {
		InputStream is;
		try {
			is = new FileInputStream("conf/AwsCredentials.properties");
		} catch (Exception e) { e.printStackTrace(); }
		def credentials = new PropertiesCredentials(is);
		ec2 = new AmazonEC2Client(credentials);
	}

	static def print_status() throws Exception {
		try {
			init();
			def availabilityZonesResult = ec2.describeAvailabilityZones();
			println("You have access to ${availabilityZonesResult.availabilityZones} Availability Zones.");
			def describeInstancesRequest = ec2.describeInstances();
			def instances = [];
			describeInstancesRequest.getReservations().each { reservation ->
				instances += reservation.getInstances();
			}
			def sep = "\n" + '---'*10 + "\n";
			println("You have \n${sep}${instances.join(sep)}\n Amazon EC2 instance(s) running.");
		} catch (AmazonServiceException ase) {
			println("Caught Exception: " + ase.getMessage());
		}
	}

	static def create_instances( params ) {
		init();
		def runRequest = new RunInstancesRequest(); 
		params.each { k,v -> runRequest."$k" = v; }
		def reservation = ec2.runInstances(runRequest).reservation;
		def newInstances;
		while( true ) {
			def _all = reservation.instances.findAll { instance -> instance.state.name == "running" }
			if( _all.size != params.maxCount ) {
				sleep 30000;
				println "Instances started now: $_all";
				reservation = ec2.describeInstances().reservations.find { _reservation ->
					_reservation.reservationId == reservation.reservationId
				}
			} else { newInstances = _all; break; }
		}
		println "started new instance: $newInstances";
		String instanceId = newInstances.instanceId;
		String ipAddress = newInstances.privateIpAddress;
		try {
			def inetAddress = InetAddress.getByName(ipAddress);
			def hostname = inetAddress.hostName;
			def nodeId = sgeService().addNode(
					[name:hostname, ipAddress:ipAddress, instanceId:instanceId, isEC2:true]);
			return nodeId;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	static def delete_instances( params ) {
		init();
		def instanceIds = [];
		params.hostnames.each { hostname ->
			ClusterNode.list().each { _node -> 
				if(_node.name =~ /$hostname/ && _node.isEC2) instanceIds << _node.instanceId; 
			}
			sgeService().deleteNode([hostname:hostname]);
		}
		instanceIds.unique();
		println "delete $instanceIds";
		try {
			def runRequest = new TerminateInstancesRequest();
			runRequest.instanceIds = instanceIds; // ["id1","id2"]
			ec2.terminateInstances(runRequest).terminatingInstances.each { state ->
				println "${state.instanceId} -- down --> ${state.currentState}";
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}

// new ClusterNode(name:"kuruwa12",instanceId:"i-ab3d73c0").save();
// new ClusterNode(name:"kuruwa13",instanceId:"i-c33b75a8").save();
// new ClusterNode(name:"kuruwa14",instanceId:"i-5737793c").save();
// new ClusterNode(name:"kuruwa16",instanceId:"i-91357bfa").save();
// AwsService.delete_instances([hostnames:["kuruwa12","kuruwa13","kuruwa14","kuruwa16"]]);

// zone = new Placement();
// zone.availabilityZone = "us-east-1c";
// def instance_spec = [ imageId:"ami-dddd33b4", instanceType:"m1.large", subnetId:"subnet-cff332a6", placement:zone, minCount:1, maxCount:1,  keyName:"inters-dev" ];
// AwsService.create_instances( instance_spec );
// AwsService.print_status();
