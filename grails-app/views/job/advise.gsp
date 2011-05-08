
<html>
		<head>
				<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
				<meta name="layout" content="main" />
				<g:javascript library="prototype" />
				<script language="JavaScript" src="/scheduler/js/_pt_updater.js" ></script>
				<script language="JavaScript" type="text/javascript">
					window.onload = 
						ajaxPeriodicUpdate('/scheduler/advise/listAdvise', 30)
				</script>
					<title>Advise List</title>
		</head>
						<body>
								<g:if test="${flash.message}">
								<div id="show" class="message">${flash.message}</div>
								</g:if>
								<div class="body" align="center">
								<g:formRemote name="chgAdvise" url="[ controller:'advise', action:'useAdvise' ]" 
								update="show">
									InputAdviseIds: <input type="text" name="ids" value="" />
									Or SelectOne: <g:select optionKey="id" from="${Advise.list()}" name='advise.id' 
										noSelection="['':'-- select Advise --']"></g:select>
									<input type="submit" value="useIt" />
									</g:formRemote>
								</div>
							<hr>
							<div class="body" align="center" height="100%">
								<div id="failure"> failure </div>
								<div id="success"> success </div>
								</tr></table>
							</div>
						</body>
</html>
