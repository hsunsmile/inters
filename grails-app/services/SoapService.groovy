import groovy.net.xmlrpc.*
import groovyx.net.ws.WSServer
import javax.jws.soap.*
import javax.jws.*
import javax.xml.ws.*
import javax.xml.bind.annotation.*
import java.net.ServerSocket

class SoapService {

	def server = new WSServer();
	def myService;
	def XMLRPCServer = new XMLRPCServer();

	def start( int port ) {
		// println "[DBG] start ${this} at $port... ";
		server.setNode("MyService", "http://localhost:$port/MyService");
		server.setNode("SchedulerCoreService", 
				"http://localhost:$port/SchedulerCoreService");
		// println "[DBG] ${this} starting.... ";
		try {
			server.start();
			println "[DBG] ${this} started ";
		}catch( Exception e ) {
			e.printStackTrace();
		}
	}

	def startCXF( int port ) {
		def endPoint = "http://localhost:$port/MyService";
		// println "[DBG] publish ${myService} at ${endPoint}:${port}... ";
		Endpoint.publish( endPoint, myService );
		println "[DBG] published ${myService} at ${endPoint}:${port}... ";
	}

	def startXMLRPCServer( int port ) {
		XMLRPCServer.echo = {
			println "[DBG] received $it ";
			return "${it}";
		}
		def callNum = 0;
		def add = { x,y ->
			println "[DBG] received $x,$y ";
			callNum++;
			sleep callNum*100;
			return "$x + $y is ${x+y} $callNum"; 
		} 
		def addInt = { x,y ->
			println "[DBG] received $x,$y ";
			callNum++;
			sleep callNum*100;
			return x+y;
		} 
		XMLRPCServer.add = add;
		XMLRPCServer.addInt = addInt;	
		// server.add = { x,y -> return (int)(x+y); }
		def serverSocket = new ServerSocket( port );
		// println "[DBG] publish ${XMLRPCServer} at ${port}... ";
		XMLRPCServer.startServer(serverSocket);
		println "[DBG] published ${XMLRPCServer} at ${port}... ";
	}
}

/*
   @XmlAccessorType(XmlAccessType.FIELD)
   class Book {
   String name;
   String author;
   }

   @WebService (targetNamespace="http://predic8.com/groovy-jax/")
   @SOAPBinding(parameterStyle=SOAPBinding.ParameterStyle.BARE)
   class BookService{
   @WebMethod
   public void add(Book book){
   println "Name of the book: ${book.name}";
   }
   }

*/
