  
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Edit ClusterFunctionRelationship</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">ClusterFunctionRelationship List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New ClusterFunctionRelationship</g:link></span>
        </div>
        <div class="body">
            <h1>Edit ClusterFunctionRelationship</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${clusterFunctionRelationship}">
            <div class="errors">
                <g:renderErrors bean="${clusterFunctionRelationship}" as="list" />
            </div>
            </g:hasErrors>
            <g:form controller="clusterFunctionRelationship" method="post" >
                <input type="hidden" name="id" value="${clusterFunctionRelationship?.id}" />
                <div class="dialog">
                    <table>
                        <tbody>
                        
                            <tr class='prop'>
                                <td valign='top' class='name'>
                                    <label for='cluster'>Cluster:</label>
                                </td>
                                <td valign='top' class='value ${hasErrors(bean:clusterFunctionRelationship,field:'cluster','errors')}'>
                                    <g:select optionKey="id" from="${Cluster.list()}" name='cluster.id' value="${clusterFunctionRelationship?.cluster?.id}" ></g:select>
                                </td>
                            </tr> 
                        
                            <tr class='prop'>
                                <td valign='top' class='name'>
                                    <label for='function'>Function:</label>
                                </td>
                                <td valign='top' class='value ${hasErrors(bean:clusterFunctionRelationship,field:'function','errors')}'>
                                    <g:select optionKey="id" from="${RemoteFunction.list()}" name='function.id' value="${clusterFunctionRelationship?.function?.id}" ></g:select>
                                </td>
                            </tr> 
                        
                            <tr class='prop'>
                                <td valign='top' class='name'>
                                    <label for='client'>Client:</label>
                                </td>
                                <td valign='top' class='value ${hasErrors(bean:clusterFunctionRelationship,field:'client','errors')}'>
                                    <g:select optionKey="id" from="${Client.list()}" name='client.id' value="${clusterFunctionRelationship?.client?.id}" ></g:select>
                                </td>
                            </tr> 
                        
                        </tbody>
                    </table>
                </div>
                <div class="buttons">
                    <span class="button"><g:actionSubmit class="save" value="Update" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                </div>
            </g:form>
        </div>
    </body>
</html>
