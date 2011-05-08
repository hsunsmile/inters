
var rockwell = { src: '/WEB_APP/plugins/Sifr-0.1/swf/rockwell-8.swf' };

sIFR.debugMode = false;
sIFR.delayCSS  = true;
// sIFR.domains = ['novemberborn.net'] // Don't check for domains in this demo
sIFR.activate(rockwell);
  
sIFR.replace(rockwell, {
	selector: 'h1',
	css: [
		'.sIFR-root { text-align: left; font-weight: bold; }',
		'a { text-decoration: none; }',
		'a:link { color: #000000; }',
		'a:hover { color: #CCCCCC; }'
	]
});