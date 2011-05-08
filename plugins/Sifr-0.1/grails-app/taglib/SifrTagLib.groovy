class SifrTagLib {

	def sifrHead = { attrs ->
		def applicationUri = grailsAttributes.getApplicationUri(request)
		out << """
		<link rel="stylesheet" href="${applicationUri}/plugins/Sifr-0.1/css/sIFR-screen.css" type="text/css" media="screen" />
		<link rel="stylesheet" href="${applicationUri}/plugins/Sifr-0.1/css/sIFR-print.css" type="text/css" media="print" />
		<script type="text/javascript" src="${applicationUri}/plugins/Sifr-0.1/js/sifr.js"></script>"""
		def debug = attrs.debug ? Boolean.valueOf(attrs.debug) : false
		if(debug) {
		out << """
		<script type="text/javascript" src="${applicationUri}/plugins/Sifr-0.1/js/sifr-debug.js"></script>"""
		}
		out << """
		<script type="text/javascript" src="${applicationUri}/plugins/Sifr-0.1/js/sifr-config.js"></script>"""
	}

}