package org.codehaus.groovy.grails.plugins.web.taglib;
/**
 * An implementation for the Dojo javascript library
 *
 * @author Graeme Rocher
 */
class DojoProvider implements JavascriptProvider {
	 def doRemoteFunction(taglib,attrs, out) {
		if(attrs.onLoading) {
			out << "${attrs.onLoading};"
		}		
		 out << 'dojo.io.bind({url:\''

		 out << taglib.createLink(attrs) 
		attrs.remove('params')
		 out << '\',load:function(type,data,evt) {'
	    if(attrs.onLoaded) {
			out << "${attrs.onLoaded};"
		}		
		 if(attrs.update) {			
			out << 'dojo.html.textContent( dojo.byId(\''
			out << (attrs.update instanceof Map ? attrs.update.success : attrs.update)
			out << '\'),data);'		
		 }
		if(attrs.onSuccess) {
			out << ";${attrs.onSuccess};"
		}
		if(attrs.onComplete) {
			out << ";${attrs.onComplete};"
		}		
		out << '}'
		out << ',error:function(type,error) { '
		if(attrs.update instanceof Map) {
			if(attrs.update.failure) {
				out << "dojo.html.textContent( dojo.byId('${attrs.update.failure}'),error.message);"									
			}
		}
		if(attrs.onFailure) {
			out << ";${attrs.onFailure};"
		}	
		if(attrs.onComplete) {
			out << ";${attrs.onComplete};"
		}				
	     out << '}'
	     attrs.options?.each {k,v ->
	     	out << ",$k:$v"
	     }
		 out << '});' 
		attrs.remove('options')
	 }
	 
	 def prepareAjaxForm(attrs) {
		if(attrs.options) {
		    attrs.options.formNode = "dojo.byId('${attrs.name}')"
		}
		else {
			attrs.options = [formNode:"dojo.byId('${attrs.name}')"]
		}
	 }
}
