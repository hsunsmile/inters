import java.rmi.server.UID
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import groovy.xml.MarkupBuilder
import de.andreasschmitt.richui.taglib.renderer.*

class UserInterfaceTagLib {

	/**
	 * This code has been taken from a previous Grails release, where this component was included.
	 *
	 * A Rich Text Editor component that by default uses fckeditor with a basepath of /fckeditor.
	 * TODO: Add support for other rich text editing components like those from the Dojo framework
	 *
	 * Example:
	 *
	 * <g:richTextEditor name="editor" height="400" />
	 */
	def richTextEditor = { attrs ->
		out << withTag(name:'script',attributes:[type:'text/javascript']) {
			if(attrs.onComplete) {
				out.println "function FCKeditor_OnComplete( editorInstance ) {"
					out.println "${attrs.onComplete}(editorInstance);"					
				out.println "}"
			}
			out << """
			var oFCKeditor = new FCKeditor( '${attrs.name}' ) ;
			oFCKeditor.BasePath	 = \""""
			if(attrs.basepath) {
				out << createLinkTo(dir:attrs.basepath)
			}
			else {
			    out << createLinkTo(dir:"fckeditor/")
			}
			out.println '";'
			if(attrs.toolbar) {
				out << "oFCKeditor.ToolbarSet	 = '${attrs.toolbar}';" 	
			}
			// add width support
			if(attrs.width)			
				out.println "oFCKeditor.Width	= '${attrs.width}';"
			
			if(attrs.height)			
				out.println "oFCKeditor.Height	= '${attrs.height}';"
			
			// add skin support, values to choose: "default", "office2003", "silver"
			if(attrs.skin)
				out.println "oFCKeditor.Config['SkinPath'] = 'skins/${attrs.skin}/';"
			
			// check the browser compatibility when rendering the editor.  default value: true, values to choose: true, false, 
			if(attrs.checkBrowser)
				out.println "oFCKeditor.CheckBrowser = ${attrs.checkBrowser};"

			// show error messages on errors while rendering the editor.   default value: true, values to choose: true, false
			if(attrs.displayErrors)
				out.println "oFCKeditor.DisplayErrors = ${attrs.displayErrors};"

			// oFCKeditor.Config      AutoDetectLanguage:true/false, DefaultLanguage:'pt-BR' and so on
			if(attrs.config) {
				if (attrs.config instanceof Map) {
					attrs.config.each { k, v ->
						out.println "oFCKeditor.Config['$k'] = '$v';"
					}
				} else {
					throw new Exception("""The format of config is not correct, it should be like "[AutoDetectLanguage:false, DefaultLanguage:'pt-BR']"   """)
				}
			}

			
			if(attrs.value) {
							// replace " with '   original implementation: out << "oFCKeditor.Value	= \""
				out << "oFCKeditor.Value	= \'"			    
				out << attrs.value.replaceAll(/\r\n|\n|\r/, '\\\\n').replaceAll('"','\\\\"').replaceAll("'","\\\\'")
				out.println "\' ;"
			}
			
			out.println "oFCKeditor.Create();"	

		}
	}
}