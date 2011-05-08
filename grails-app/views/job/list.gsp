
<html>
		<head>
				<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
				<meta name="layout" content="main" />
				<g:javascript library="prototype" />
				<title>Job List</title>
		</head>
		<body>
				<g:if test="${flash.message}">
				<div class="message">${flash.message}</div>
				</g:if>
				<div class="navi">
					<div class="menuButton" align="center">
						<table><tr><td>
						<g:formRemote name="graph" url="[ action:'showGraph' ]" update="all">
						    Job GroupedBy: <input type="text" name="type" value="valid values: [all, functions, clusters]" />
						    &nbsp;NumberOfJob/Graph: <input type="text" name="limit" value="20" />
							<input type="submit" value="ShowGraph" />
							</g:formRemote></td>
							<td><g:formRemote name="graph" url="[ action:'doNothing' ]" update="all">
							<input type="submit" value="HideGraph" />
							</g:formRemote></td></tr></table>
					</div>
				</div>
				<div id="all" class="body" align="center"> <div>
		</body>
</html>
