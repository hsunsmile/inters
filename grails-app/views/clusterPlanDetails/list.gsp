  
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>ClusterPlanDetails List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="create" action="create">New ClusterPlanDetails</g:link></span>
        </div>
        <div class="body">
            <h1>ClusterPlanDetails List</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                   	        <g:sortableColumn property="id" title="Id" />
                        
                   	        <th>Cluster</th>
                   	    
                   	        <g:sortableColumn property="numberOfJobs" title="Number Of Jobs" />
                        
                   	        <th>Plan</th>
                   	    
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${clusterPlanDetailsList}" status="i" var="clusterPlanDetails">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${clusterPlanDetails.id}">${clusterPlanDetails.id?.encodeAsHTML()}</g:link></td>
                        
                            <td>${clusterPlanDetails.cluster?.encodeAsHTML()}</td>
                        
                            <td>${clusterPlanDetails.numberOfJobs?.encodeAsHTML()}</td>
                        
                            <td>${clusterPlanDetails.plan?.encodeAsHTML()}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${ClusterPlanDetails.count()}" />
            </div>
        </div>
    </body>
</html>
