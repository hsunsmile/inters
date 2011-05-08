
class ClientController {

		def schedulerCoreService
		def executorService
		def scaffold = Client

		def execute = { 
			def client = Client.get( params.id );
			if(client) { 
				try {
					flash.message = "Client ${client} Executed!";
				} catch (ClassNotFoundException e) {
					flash.message = " Exception $e Ocurred !";
					log.error(e);
				} catch (InstantiationException e) { 
					flash.message = " Exception $e Ocurred !";
					log.error(e);
				} catch (IllegalAccessException e) { 
					flash.message = " Exception $e Ocurred !";
					log.error(e);
				}
				redirect(controller:'job', action:'monitor');
			} else {
				flash.message = "Client not found with id ${params.id}";
				redirect(action:create);
			}
			println "[${new Date()}] after redirect.... ";
			def submitter = grailsApplication.classLoader.
				loadClass( client.submitterName ).newInstance();
			executorService.invokeWithSession("ClientExecution[${params.id}]") {
				println "[${new Date()}] -1- ${submitter}";
				submitter.submit(schedulerCoreService, client.id)
				println "[${new Date()}] -2- ${submitter}";
			}
		}

		def guideFlow = {
			introduce {
				on("createClient").to "createClinet"
				on("addCluster").to "addCluster"
			}
			createClinet {
				redirect(controller:"client", action:"list")
			}
			addCluster {
				redirect(controller:"cluster", action:"list")
			}
		}

}
