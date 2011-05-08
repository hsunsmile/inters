<html>
    <head>
        <title>GrpcScheduler ControllCenter</title>
        <meta name="layout" content="dojo" />
    </head>
    <body>
        <div class="body">
            <div class="titleSection">
        <h1 class="title">Welcome to GrpcScheduler ControllCenter</h1>
            </div>
        <p style="margin-left:20px;width:80%">Congratulations! you have successfully started GrpcScheduler ControllCenter application! This is the main menu, you may monitoring the job infomation, change the scheduling algorithm and do many other things. 
        Below is a list of controllers that are currently provided under this version,
        click on each to execute its default action:</p>
        <div class="dialog">
            <ul>
              <g:each var="c" in="${grailsApplication.controllerClasses}">
                    <li class="controller"><g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link> </li>
              </g:each>
            </ul>
        </div>
        </div>
    </body>
</html>
