package de.andreasschmitt.richui.taglib.renderer

import java.rmi.server.UID
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Formatter
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.apache.commons.codec.digest.DigestUtils

/*
*
* @author Andreas Schmitt
*/
class RenderUtils {

	public static String getUniqueId() {
		return DigestUtils.md5Hex(new UID().toString())
    }
	
	public static String getResourcePath(String pluginName){
		String pluginVersion = PluginManagerHolder?.pluginManager?.getGrailsPlugin(pluginName)?.version
		String appName = getApplicationName()
		
		"/$appName/plugins/richui-$pluginVersion"
	}
	
	public static String getApplicationName(){
		String appName = ApplicationHolder?.application?.metadata["app.name"]
		return appName
	}

}