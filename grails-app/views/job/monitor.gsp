
<html>
		<head>
				<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
				<meta name="layout" content="main" />
				<g:javascript library="prototype" />
				<title>Job List</title>
		</head>
		<body>
				<g:if test="${flash.message}">
				<div id="mes" class="message">${flash.message}</div>
				</g:if>
				<div class="body">
						<table>
							<tr>
								<td width="160">
						<div>
						<g:formRemote name="graphShow" url="[ action:'showGraph' ]" update="all">
						    Job GroupedBy: <input type="text" name="type" value="" />
						    &nbsp;NumberOfJob/Graph: <input type="text" name="limit" value="20" />
							<input type="submit" value="ShowGraph" />
							</g:formRemote></div>
							<div><g:formRemote name="graphHide" url="[ action:'doNothing' ]" update="all">
							<input type="submit" value="HideGraph" />
							</g:formRemote></div>
							<hr>
							<div><g:formRemote name="chgCluster" url="[ action:'changeCluster' ]" update="mes">
							<input type="text" name="ids" value="" />
							<g:select optionKey="id" from="${Cluster.list()}" name='cluster.id' 
									noSelection="['':'-- select cluster --']"></g:select>
							<input type="submit" value="changeTo" />
							</g:formRemote></div>
	</td>
	<td>
					<div id="all"> <div>
			</td></tr></table>
				</div>
		</body>
</html>
