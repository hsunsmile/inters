  
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Show ClusterFunctionRelationship</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">ClusterFunctionRelationship List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New ClusterFunctionRelationship</g:link></span>
        </div>
        <div class="body">
            <h1>Show ClusterFunctionRelationship</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table>
                    <tbody>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Id:</td>
                            
                            <td valign="top" class="value">${clusterFunctionRelationship.id}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Cluster:</td>
                            
                            <td valign="top" class="value"><g:link controller="cluster" action="show" id="${clusterFunctionRelationship?.cluster?.id}">${clusterFunctionRelationship?.cluster}</g:link></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Function:</td>
                            
                            <td valign="top" class="value"><g:link controller="remoteFunction" action="show" id="${clusterFunctionRelationship?.function?.id}">${clusterFunctionRelationship?.function}</g:link></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Client:</td>
                            
                            <td valign="top" class="value"><g:link controller="client" action="show" id="${clusterFunctionRelationship?.client?.id}">${clusterFunctionRelationship?.client}</g:link></td>
                            
                        </tr>
                    
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form controller="clusterFunctionRelationship">
                    <input type="hidden" name="id" value="${clusterFunctionRelationship?.id}" />
                    <span class="button"><g:actionSubmit class="edit" value="Edit" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                    <span class="button"><g:actionSubmit class="addNgHandle" value="AddNgHandle" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>
