  
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>Show ClusterPlanDetails</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">ClusterPlanDetails List</g:link></span>
            <span class="menuButton"><g:link class="create" action="create">New ClusterPlanDetails</g:link></span>
        </div>
        <div class="body">
            <h1>Show ClusterPlanDetails</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table>
                    <tbody>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Id:</td>
                            
                            <td valign="top" class="value">${clusterPlanDetails.id}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Cluster:</td>
                            
                            <td valign="top" class="value"><g:link controller="cluster" action="show" id="${clusterPlanDetails?.cluster?.id}">${clusterPlanDetails?.cluster}</g:link></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Number Of Jobs:</td>
                            
                            <td valign="top" class="value">${clusterPlanDetails.numberOfJobs}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name">Plan:</td>
                            
                            <td valign="top" class="value"><g:link controller="plan" action="show" id="${clusterPlanDetails?.plan?.id}">${clusterPlanDetails?.plan}</g:link></td>
                            
                        </tr>
                    
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form controller="clusterPlanDetails">
                    <input type="hidden" name="id" value="${clusterPlanDetails?.id}" />
                    <span class="button"><g:actionSubmit class="edit" value="Edit" /></span>
                    <span class="button"><g:actionSubmit class="delete" onclick="return confirm('Are you sure?');" value="Delete" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>
