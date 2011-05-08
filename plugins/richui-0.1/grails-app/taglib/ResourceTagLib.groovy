import de.andreasschmitt.richui.taglib.renderer.*

/*
*
* @author Andreas Schmitt
*/
class ResourceTagLib {
	
	static namespace = "resource"
	
	Renderer autoCompleteRenderer
	Renderer dateChooserRenderer
	Renderer mapRenderer
	Renderer ratingRenderer
	Renderer tabViewRenderer
	Renderer tagCloudRenderer
	Renderer timelineRenderer
	Renderer tooltipRenderer
	Renderer treeViewRenderer
	
	def autoComplete = { attrs ->	
		//Render output
		try {
			out << autoCompleteRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}	
	}
	
	def dateChooser = {	attrs ->
		//Render output
		try {
			out << dateChooserRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}
	}
	
	def richTextEditor = { attrs ->        
		def builder = new groovy.xml.MarkupBuilder(out)
		String resourcePath = RenderUtils.getResourcePath("richui")
        
		builder.yieldUnescaped "<!-- RichTextEditor -->"
		builder.script(type: "text/javascript", src: "$resourcePath/js/fckeditor/fckeditor.js", "")
	}
	
	def googlemaps = { attrs ->
		//Render output
		try {
			out << mapRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}
	}
	
	def rating = { attrs ->
		//Render output
		try {
			out << ratingRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}
	}
	
	def tabView = { attrs ->
		//Render output
		try {
			out << tabViewRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}
	}
	
	def tagCloud = { attrs ->
		//Render output
		try {
			out << tagCloudRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}		
	}
	
	def timeline = { attrs ->
		//Render output
		try {
			out << timelineRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}		
	}
	
	def tooltip = { attrs ->
		//Render output
		try {
			out << tooltipRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}		
	}
	
	def treeView = { attrs ->	
		//Render output
		try {
			out << treeViewRenderer.renderResources(attrs)
		}
		catch(RenderException e){
			log.error(e)
		}
	}

}