

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Show Client</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">Client List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New Client</g:link></span>
        </div>
        <div class="body">
            <h1>Show Client</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table>
                    <tbody>

                    
                        <tr class="prop">
                            <td valign="top" class="name">Id:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:clientInstance, field:'id')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Config File Path:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:clientInstance, field:'configFilePath')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Submitter Name:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:clientInstance, field:'submitterName')}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">User Name:</td>
                            
                            <td valign="top" class="value">${fieldValue(bean:clientInstance, field:'userName')}</td>
                            
                        </tr>
                    
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form>
                    <input type="hidden" name="id" value="${clientInstance?.id}" />
                    <span class="button"><g:actionSubmit class="edit" value="Edit" /></span>
                    <span class="button"><g:actionSubmit class="execute" value="Execute" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>
