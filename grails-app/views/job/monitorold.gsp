
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
				<div class="body">
						<table> <tr>
								<td width="200" vailgn="top">
								<g:formRemote name="cgraph" url="[ action:'showGraph' ]" update="all">
								Job GroupedBy: <input type="text" name="type" />
								NumberOfJob/Graph: <input type="text" name="limit" value="20" />
								<input type="submit" value="ShowGraph" />
								</g:formRemote>
								<g:formRemote name="dgraph" url="[ action:'doNothing' ]" update="all">
									<input type="submit" value="HideGraph" />
								</g:formRemote>
								<hr>
            					<g:form controller="job" method="post" >
									<input type="text" name="ids" />
									<g:select optionKey="id" from="${Cluster.list()}" name='toCluster' 
										noSelection="['':' -- Select Cluster -- ']">
									</g:select>
                    				<span class="button"><g:actionSubmit class="save" value="changeCluster" /></span>
								</g:form>
								</td>
								<td>									
									<div id="all"><div>
								</td>
								</tr>
						</table>
				</div>
		</body>
</html>
