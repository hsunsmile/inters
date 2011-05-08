package de.andreasschmitt.richui.taglib.renderer

import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.web.taglib.GroovyPageTagBody

/*
*
* @author Andreas Schmitt
*/
class TimelineRenderer extends AbstractRenderer {
	
	protected void renderTagContent(Map attrs, MarkupBuilder builder) throws RenderException {
		renderTagContent(attrs, null, builder)
	}
	
	protected void renderTagContent(Map attrs, GroovyPageTagBody body, MarkupBuilder builder) throws RenderException {
		String id = "t" + RenderUtils.getUniqueId()
			
		builder.div("class": attrs?.'class', style: attrs?.style, "id": id, ""){}
					
		builder.script(type: "text/javascript"){
			builder.yieldUnescaped "	var tl;\n"
			builder.yieldUnescaped "	function initTimeline() {\n"
			builder.yieldUnescaped "		alert('called!!'); \n"
			builder.yieldUnescaped "		var eventSource = new Timeline.DefaultEventSource();\n"
			builder.yieldUnescaped "		var bandInfos = [\n"
			builder.yieldUnescaped "		Timeline.createBandInfo({\n"
			builder.yieldUnescaped "			eventSource:    eventSource,\n"
				
			builder.yieldUnescaped "			date:           '$attrs.startDate',\n"
				
			builder.yieldUnescaped "			width:          '70%', \n"
			builder.yieldUnescaped "			intervalUnit:   Timeline.DateTime.MONTH, \n"
			builder.yieldUnescaped "			intervalPixels: 100\n"
			builder.yieldUnescaped "		}),\n"

			builder.yieldUnescaped "		Timeline.createBandInfo({\n"
			builder.yieldUnescaped "		    showEventText:  false,\n"
			builder.yieldUnescaped "			trackHeight:    0.5,\n"
			builder.yieldUnescaped "			trackGap:       0.2,\n"
			builder.yieldUnescaped "			eventSource:    eventSource,\n"
				
			builder.yieldUnescaped "			date:           '$attrs.startDate',\n"
				
			builder.yieldUnescaped "			width:          '30%',\n" 
			builder.yieldUnescaped "			intervalUnit:   Timeline.DateTime.YEAR, \n"
			builder.yieldUnescaped "			intervalPixels: 200\n"
			builder.yieldUnescaped "		})\n"
			builder.yieldUnescaped "		];\n"

			builder.yieldUnescaped "		bandInfos[1].syncWith = 0;\n"
			builder.yieldUnescaped "		bandInfos[1].highlight = true;\n"

			builder.yieldUnescaped "		tl = Timeline.create(document.getElementById('$id'), bandInfos);\n"
				
			if(attrs?.datasource) {
				builder.yieldUnescaped "		tl.loadXML('$attrs.datasource', function(xml, url) { eventSource.loadXML(xml, url); });\n"
			}
			builder.yieldUnescaped "}\n"

			builder.yieldUnescaped "var resizeTimerID = null;\n"
			builder.yieldUnescaped "function onResize() {\n"
			builder.yieldUnescaped "	if (resizeTimerID == null) {\n"
			builder.yieldUnescaped "		resizeTimerID = window.setTimeout(function() {\n"
			builder.yieldUnescaped "		resizeTimerID = null;\n"
			builder.yieldUnescaped "		tl.layout();\n"
			builder.yieldUnescaped "	}, 500);\n"
			builder.yieldUnescaped "}\n"
			builder.yieldUnescaped "}\n"
		}
	}
	
	protected void renderResourcesContent(Map attrs, MarkupBuilder builder, String resourcePath) throws RenderException {
		builder.script(type: "text/javascript", src: "http://simile.mit.edu/timeline/api/timeline-api.js", "")
	}

}
