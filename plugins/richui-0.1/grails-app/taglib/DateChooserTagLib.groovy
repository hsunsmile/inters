import de.andreasschmitt.richui.taglib.renderer.*
import java.text.SimpleDateFormat

/*
*
* @author Andreas Schmitt
*/
class DateChooserTagLib {
	
	static namespace = "richui"
	
	Renderer dateChooserRenderer
	
	def dateChooser = {attrs ->			
		if(attrs.name){
			
			if(attrs?.format && (attrs.format == "dd.MM.yyyy" || attrs.format == "MM/dd/yyyy")){

			}
			else {
				attrs.format = "dd.MM.yyyy"
			}
			
			try {
				String date
				if(attrs.value == null || (attrs.value instanceof String && attrs.value == "")){
					log.debug("using new date")
					date = new SimpleDateFormat(attrs.format).format(new Date())
				}
				else {
					log.debug("using value")
					date = new SimpleDateFormat(attrs.format).format(attrs.value)
				}
				attrs.remove("value")
				attrs.value = date
			}
			catch(Exception e){
				log.error("Error parsing date", e)
				attrs.remove("value")
				attrs.value = new SimpleDateFormat(attrs.format).format(new Date())
			}			
			
			if(!attrs.id){
				attrs.id = attrs.name
			}
			
			if(!attrs?.'class'){
				attrs.'class' = ""
			}
			
			if(!attrs?.style){
				attrs.style = ""
			}
		
			//Render output
			try {
				out << dateChooserRenderer.renderTag(attrs)
			}
			catch(RenderException e){
				log.error(e)
			}
		}
	}
}