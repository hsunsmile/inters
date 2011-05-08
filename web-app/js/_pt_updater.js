
function ajaxPeriodicUpdate( url, freq ) {
		var ajax = new Ajax.PeriodicalUpdater( {success: '', failure: 'failure'}, url, 
						{
method: 'get',
			parameters: '',
			encoding: 'UTF-8',
			frequency: freq,
			decay: 1,
			onCreate: processCreate,
			onLoading: processLoading,
			onSuccess: processSuccess,
			onFailure: processFailure,
			onComplete: processComplete
						});
}
function processLoading(transport) {
		//$('success').innerHTML = 'Loading response ...';
}
function processCreate(transport) {
		alert('create');
}
function processSuccess(transport, json ) {
	var result = transport.responseText;
	var advises = eval( "(" + result + ")" );
	var frame = $('success');
	var limit = 3;
	var tableStyle = "margin-top:6px; margin-bottm:6px;";
	var tdLeftBar = "width:3px; background:#999999;";
	var tdRightStyle = "float:left; width:200px; background:#CCCCFF; margin-right:30px; height:70px;";
	var adviseNum = "font-family:fantasy; font-size:50px; text-align:center; float:left; width:70px; height:70px;";
	adviseNum += "border-left:6px inset #ffffff; border-top:1px dashed; border-bottom:1px dashed; "
	// var message = "";
	var message = "<table align='center' style='"+ tableStyle +"'><tr><td>";
	var elementAdded = false;
	advises.each( function(advise) {
		if( $("advise"+advise.id) ) {
			if( advise.status != "proposed" ) {
				$("status"+advise.id).innerHTML = advise.status; 
				Element.setStyle($("advise"+advise.id), {color:'#999999'});
			}
			return;
		}
		if( (advise.id-1)%limit == 0 ) { 
			// if( (advise.id-1)/limit > 0 || advise.id == advises.length ) { message += "</tr></table>"; }
			// message += "<table align='center' style='"+ tableStyle +"'><tr>"; 
			if( (advise.id-1)/limit > 0 || advise.id == advises.length ) { message += "<div style='clear:both'></div>"; }
			message += "<div align='center' style='"+ tableStyle +"'>";
		}
		message += "<div id=advise" + advise.id + " style='"+ adviseNum +"' >" + advise.id;
		message	+= "<div id=status" + advise.id + " style='font-size:12px;'>" + advise.status + "</div></div>";
		message	+= "</div><div style='"+ tdRightStyle +"'>";
		message += "<div style='padding-left:6px; text-align:left;'>" + advise.jobName + "</div>";
		message	+= "<div style='padding-left:6px; text-align:left;'>" + advise.clusterName + "</div>";
		message	+= "<div style='padding-left:6px; text-align:left;'>" + advise.method + "</div>";
		message += "</div>";
		elementAdded = true;
	});
	message += "</td></tr></table>";
	if( elementAdded ) new Insertion.After( frame, message );
}

function processFailure(transport) {
	//	alert('failure');
}
function processComplete(transport) {
		alert('complete');
}
function showResponses(transport) {
		alert('responseHeaders: ' + transport.getAllResponseHeaders());
		alert('response header content-length: ' + transport.getResponseHeader('content-length'));
		alert('status: ' + transport.status);
		alert('statusText: ' + transport.statusText);
		alert('responseXML: ' + transport.responseXML);
		alert('responseText: ' + transport.responseText);
}

