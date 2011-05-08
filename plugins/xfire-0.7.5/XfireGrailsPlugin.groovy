import org.codehaus.groovy.grails.plugins.support.*
import org.grails.xfire.*
import org.grails.xfire.ServiceFactoryBean
import org.codehaus.groovy.grails.commons.GrailsClassUtils

class XfireGrailsPlugin {

    def author = "Chanwit Kaewkasi"
    def authorEmail = "chanwit@gmail.com"
    def title = "Add Web Service support for Grails services using XFire."
    def description = '''\
XFire plugin allows Grails applications to expose service 
classes as Web Services. It uses the SOAP implementation 
from XFire 1.2.6.
'''
  	def documentation = "http://grails.org/Xfire+plugin"
	def version = '0.7.5'
	def loadAfter = ['services']
  	def observe = ['services','hibernate']
	def dependsOn = [services: '1.0.2 > 1.1']    
	                 
    def watchedResources = ["file:./grails-app/services/**/*Service.groovy",
							"file:./plugins/*/grails-app/services/**/*Service.groovy"]
    	
	def doWithSpring = {
	
		"xfire.serviceRegistry"(org.codehaus.xfire.service.DefaultServiceRegistry) { bean->			
			bean.getBeanDefinition().setSingleton(true)
		}
		
		"xfire.transportManager"(org.codehaus.xfire.transport.DefaultTransportManager){ bean->
			bean.getBeanDefinition().setSingleton(true)
			bean.getBeanDefinition().setInitMethodName("initialize")
			bean.getBeanDefinition().setDestroyMethodName("dispose")
		}
				
		"xfire"(org.codehaus.xfire.DefaultXFire, 
		         ref("xfire.serviceRegistry"), 
				 ref("xfire.transportManager")) { bean ->			
			bean.getBeanDefinition().setSingleton(true)
		}
		
		"xfire.typeMappingRegistry"(org.codehaus.xfire.aegis.type.DefaultTypeMappingRegistry){ bean ->
			bean.getBeanDefinition().setSingleton(true)
			bean.getBeanDefinition().setInitMethodName("createDefaultMappings");
		}

		"xfire.aegisBindingProvider"(org.codehaus.xfire.aegis.AegisBindingProvider,
			ref("xfire.typeMappingRegistry")) { bean ->			
			bean.getBeanDefinition().setSingleton(true)
		}

		"xfire.serviceFactory"(org.codehaus.xfire.service.binding.ObjectServiceFactory,
			ref("xfire.transportManager"), ref("xfire.aegisBindingProvider")) { bean ->			
			bean.getBeanDefinition().setSingleton(true)
		}
		
		"xfire.servletController"(org.codehaus.xfire.transport.http.XFireServletController,
			ref("xfire")) { bean ->
			bean.getBeanDefinition().setSingleton(true)
		}
		
		"grails.xfire"(org.grails.xfire.ServiceFactoryBean, "grails.xfire") { bean ->
			bean.getBeanDefinition().setInitMethodName("initialize")
			transportManager = ref("xfire.transportManager")
			grailsApplication = ref("grailsApplication", true)
		}
		
		if(application.serviceClasses) {
			application.serviceClasses.each { service ->
				def serviceClass = service.getClazz()
				def exposeList = GrailsClassUtils.getStaticPropertyValue(serviceClass, 'expose')
				if(exposeList!=null && exposeList.contains('xfire')) {
					def sName = service.propertyName.replaceFirst("Service","XFire")
					// <bean name="myService" class="org.codehaus.xfire.spring.ServiceBean">
					"${sName}"(org.grails.xfire.ServiceBean){
						//   <property name="xfire" ref="xfire"/>
						xfire = ref("xfire")
						//   <property name="serviceBean" ref="myService"/>
						serviceBean = ref("${service.propertyName}")
						//   <property name="serviceClass" value="MyService"/>
						serviceClass = service.getClazz()
						//   <property name="serviceFactory" ref="grails.xfire"/>
						serviceFactory = ref("grails.xfire")
					}				
				}
			}		
		}
	}   
	
	def doWithApplicationContext = { applicationContext ->
		// TODO Implement post initialization spring config (optional)		
	}
	
	def doWithWebDescriptor = { xml ->	   
		def filters = xml.filter
		def filterMappings = xml.'filter-mapping'
		def servlets = xml.servlet
		def servletMappings = xml.'servlet-mapping'
		
		// define hibernate's OpenSessionInViewFilter
		def hibernateFilter = 'hibernateFilter'			                               
		filters[filters.size()-1] + {
			filter {
				'filter-name'(hibernateFilter)
				'filter-class'('org.grails.xfire.OpenSessionInViewFilter')
			}
		}		
		filterMappings[filterMappings.size()-1] + {
			'filter-mapping' {						
				'filter-name'(hibernateFilter)
				'url-pattern'("/services/*")						
			}		
		}
		
		def xfireServlet = 'XFireServlet'
		servlets[servlets.size()-1] + {
			servlet {
				'servlet-name'(xfireServlet)
				'servlet-class'('org.grails.xfire.XFireSpringServlet')
				'load-on-startup'(1)
			}
		}		
		servletMappings[servletMappings.size()-1] + {
			'servlet-mapping' {						
				'servlet-name'(xfireServlet)
				'url-pattern'("/services/*")						
			}		
		}		
		servletMappings[servletMappings.size()-1] + {
			'servlet-mapping' {						
				'servlet-name'(xfireServlet)
				'url-pattern'("/servlet/XFireServlet/*")						
			}		
		}				
	}	        
	
	def onChange = { event ->
		if(event.source) {
			def exposeList = GrailsClassUtils.getStaticPropertyValue(event.source, 'expose')
			if(exposeList!=null && exposeList.contains('xfire')) {													
				def service = application.getServiceClass(event.source.name)
				def sName = service.propertyName.replaceFirst("Service","XFire")			
				def beans = beans {
					// <bean name="myXFire" class="org.grails.xfire.ServiceBean">
					"${sName}"(org.grails.xfire.ServiceBean){
						//   <property name="xfire" ref="xfire"/>
						xfire = ref("xfire")
						//   <property name="serviceBean" ref="myService"/>
						serviceBean = ref("${service.propertyName}")
						//   <property name="serviceClass" value="MyService"/>
						serviceClass = event.source
						//   <property name="serviceFactory" ref="grails.xfire"/>
						serviceFactory = ref("grails.xfire")
					}
				}
				if(event.ctx) {
					// TODO handling change of name, and namespace
					event.ctx.registerBeanDefinition(sName, beans.getBeanDefinition(sName))
					def factory = event.ctx.getBean("grails.xfire")
					def newService = factory.create(event.source)
					def registry = event.ctx.getBean("xfire.serviceRegistry")					
					def services = registry.getServices()
					def oldService = services.find { 
						it.serviceInfo.serviceClass.name == event.source.name
					}
					if(oldService != null) {
						registry.unregister(oldService)
						registry.register(newService)
					}					
				}
			}
		}		
	}
    
	def onApplicationChange = { event ->
		// TODO Implement code that is executed when any class in a GrailsApplication changes
		// the event contain: event.source, event.application and event.applicationContext objects
	}
}
