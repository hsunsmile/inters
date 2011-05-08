  
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>ClusterFunctionRelationship List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="create" action="create">New ClusterFunctionRelationship</g:link></span>
        </div>
        <div class="body">
            <h1>ClusterFunctionRelationship List</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                   	        <g:sortableColumn property="id" title="Id" />
                        
                   	        <th>Cluster</th>
                   	    
                   	        <th>Function</th>
                   	    
                   	        <th>Client</th>
                   	    
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${clusterFunctionRelationshipList}" status="i" var="clusterFunctionRelationship">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${clusterFunctionRelationship.id}">${clusterFunctionRelationship.id?.encodeAsHTML()}</g:link></td>
                        
                            <td>${clusterFunctionRelationship.cluster?.encodeAsHTML()}</td>
                        
                            <td>${clusterFunctionRelationship.function?.encodeAsHTML()}</td>
                        
                            <td>${clusterFunctionRelationship.client?.encodeAsHTML()}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${ClusterFunctionRelationship.count()}" />
            </div>
        </div>
    </body>
</html>
