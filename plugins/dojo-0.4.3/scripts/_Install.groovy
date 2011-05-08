
//
// This script is executed by Grails after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'Ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
// Ant.mkdir(dir:"/Developer/grails-dev/plugins/dojo/grails-app/jobs")
//

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  
checkVersion()
configureProxy()

dojoVersion = "0.4.3"

Ant.sequential {
	mkdir(dir:"${grailsHome}/downloads")

    event("StatusUpdate", ["Downloading Dojo ${dojoVersion}"])

	get(dest:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax.zip",
		src:"http://download.dojotoolkit.org/release-${dojoVersion}/dojo-${dojoVersion}-ajax.zip",
		verbose:true,
		usetimestamp:true)
	unzip(dest:"${grailsHome}/downloads",
		  src:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax.zip")	
	
	mkdir(dir:"${basedir}/web-app/js/dojo")
	mkdir(dir:"${basedir}/web-app/js/dojo/src")
	
	copy(file:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax/dojo.js", 
		 tofile:"${basedir}/web-app/js/dojo/dojo.js")		
	copy(file:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax/iframe_history.html", 
		 tofile:"${basedir}/web-app/js/dojo/iframe_history.html")		
		
	copy(todir:"${basedir}/web-app/js/dojo/src") {
		fileset(dir:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax/src", includes:"**/**")
	}		 
}            
event("StatusFinal", ["Dojo ${dojoVersion} installed successfully"])
