package de.andreasschmitt.richui.taglib.renderer

import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.web.taglib.GroovyPageTagBody

/*
*
* @author Andreas Schmitt
*/
class AutoCompleteRenderer extends AbstractRenderer {
	
	protected void renderTagContent(Map attrs, MarkupBuilder builder) throws RenderException {
		renderTagContent(attrs, null, builder)
	}
	
	protected void renderTagContent(Map attrs, GroovyPageTagBody body, MarkupBuilder builder) throws RenderException {
		String resultId = "a" + RenderUtils.getUniqueId()
			
		builder.input("class": "${attrs?.'class'}", style: "${attrs?.style}", type: "text", id: "${attrs?.id}", name: "${attrs?.name}", value: "${attrs?.value}", "")
		builder.div("class": "searchcontainer yui-skin-sam", id: resultId, ""){}
			
		builder.script(type: "text/javascript"){
			builder.yieldUnescaped "	autoCompleteDataSource = new YAHOO.widget.DS_XHR(\"${attrs?.action}\", [\"result\", \"name\"]);\n"  
			if(attrs?.params){
				builder.yieldUnescaped "	autoCompleteDataSource.scriptQueryAppend = \"${attrs?.params}\";\n"
			}
			builder.yieldUnescaped "	autoCompleteDataSource.responseType = YAHOO.widget.DS_XHR.TYPE_XML;\n"
			builder.yieldUnescaped "	autoComplete = new YAHOO.widget.AutoComplete('${attrs?.id}','${resultId}', autoCompleteDataSource);\n"
			builder.yieldUnescaped "	autoComplete.queryDelay = ${attrs?.queryDelay};\n"
			builder.yieldUnescaped "	autoComplete.prehighlightClassName = \"yui-ac-prehighlight\";\n"
			builder.yieldUnescaped "	autoComplete.useShadow = ${attrs?.shadow};\n"
			builder.yieldUnescaped "	autoComplete.minQueryLength = ${attrs?.minQueryLength};\n"
					    
			if(attrs?.delimChar){
				builder.yieldUnescaped "	autoComplete.delimChar = \"${attrs?.delimChar}\";\n"
			}
		}
	}
	
	protected void renderResourcesContent(Map attrs, MarkupBuilder builder, String resourcePath) throws RenderException {			
		builder.yieldUnescaped "<!-- AutoComplete -->"
		
		if(attrs?.skin){
			if(attrs.skin == "default"){
				builder.link(rel: "stylesheet", type: "text/css", href: "$resourcePath/css/autocomplete.css")
			}
		}
		else {
			builder.link(rel: "stylesheet", type: "text/css", href: "$resourcePath/js/yui/autocomplete/assets/skins/sam/autocomplete.css")
		}
		
		builder.script(type: "text/javascript", src: "$resourcePath/js/yui/yahoo-dom-event/yahoo-dom-event.js", "")
		builder.script(type: "text/javascript", src: "$resourcePath/js/yui/connection/connection-min.js", "")
		builder.script(type: "text/javascript", src: "$resourcePath/js/yui/animation/animation-min.js", "")
		builder.script(type: "text/javascript", src: "$resourcePath/js/yui/autocomplete/autocomplete-min.js", "")
	}
}