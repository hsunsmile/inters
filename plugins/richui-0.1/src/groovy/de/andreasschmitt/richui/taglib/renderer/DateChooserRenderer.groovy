package de.andreasschmitt.richui.taglib.renderer

import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.web.taglib.GroovyPageTagBody
import java.text.SimpleDateFormat

/*
*
* @author Andreas Schmitt
*/
class DateChooserRenderer extends AbstractRenderer {
	
	protected void renderTagContent(Map attrs, MarkupBuilder builder) throws RenderException {
		renderTagContent(attrs, null, builder)
	}
	
	protected void renderTagContent(Map attrs, GroovyPageTagBody body, MarkupBuilder builder) throws RenderException {
		String id = "c" + RenderUtils.getUniqueId()
		String inputId = "i" + RenderUtils.getUniqueId()

		builder.input("class": "${attrs?.'class'}", style: "${attrs?.style}", type:"text", name: "${inputId}", id: "${inputId}", value: "${attrs?.value}", "")
		builder.div("id": id, "class": "calendar yui-skin-sam", "")
			
		builder.script(type: "text/javascript"){
			builder.yieldUnescaped "	var cal = new Calendar();\n"
			builder.yieldUnescaped "	cal.setDisplayContainer(\"$id\");\n"
			builder.yieldUnescaped "	cal.setInputId(\"${inputId}\");\n"
			builder.yieldUnescaped "	cal.setStructId(\"${attrs?.id}\");\n"
			builder.yieldUnescaped "	cal.setFormat(\"${attrs?.format}\");\n"
			if(attrs?.locale){
				builder.yieldUnescaped "	cal.setLocale(\"${attrs?.locale}\");\n"
			}
			builder.yieldUnescaped "	cal.init();\n"
		}
		
		Date date = new SimpleDateFormat(attrs.format).parse(attrs.value)
		
		builder.input(type: "hidden", name: "${attrs?.name}", id: "${attrs?.id}", value: "struct")
		builder.input(type: "hidden", name: "${attrs?.name}_day", id: "${attrs?.id}_day", value: "${date.date}")
		builder.input(type: "hidden", name: "${attrs?.name}_month", id: "${attrs?.id}_month", value: "${date.month + 1}")
		builder.input(type: "hidden", name: "${attrs?.name}_year", id: "${attrs?.id}_year", value: "${date.year - 1900}")
	}
	
	protected void renderResourcesContent(Map attrs, MarkupBuilder builder, String resourcePath) throws RenderException {			
		builder.yieldUnescaped "<!-- DateChooser -->"
		builder.link(rel: "stylesheet", type: "text/css", href: "$resourcePath/css/datechooser.css")
		builder.link(rel: "stylesheet", type: "text/css", href: "$resourcePath/js/yui/calendar/assets/calendar.css")
		builder.link(rel: "stylesheet", type: "text/css", href: "$resourcePath/js/yui/calendar/assets/skins/sam/calendar.css")
		builder.script(type: "text/javascript", src: "$resourcePath/js/yui/yahoo-dom-event/yahoo-dom-event.js", "")
		builder.script(type: "text/javascript", src: "$resourcePath/js/datechooser/datechooser.js", "")
		builder.script(type: "text/javascript", src: "$resourcePath/js/yui/calendar/calendar-min.js", "")
	}
}