import org.codehaus.groovy.grails.plugins.web.taglib.*

class DojoGrailsPlugin {
	def version = "0.4.3"
    def author = 'Graeme Rocher'
    def authorEmail = 'graemerocher at yahoo.co.uk'
    def title = 'Provides integration with the Dojo toolkit http://dojotoolkit.org, an Ajax framework.'
    def description = 'Provides integration with the Dojo toolkit http://dojotoolkit.org, an Ajax framework.'
	
	def doWithApplicationContext = { applicationContext ->
		JavascriptTagLib.PROVIDER_MAPPINGS.dojo = DojoProvider
	}
}
