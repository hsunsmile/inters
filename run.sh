
export GRAILS_HOME=~/.grails-1.2.2
export PATH=$GRAILS_HOME/bin:$PATH

_data=`date +%Y%m%d%H%M%S`
mkdir ../backups/$_data
mv devDB* execu*log ../backups/$_data
grails run-app 2>&1 | tee execution.log 
