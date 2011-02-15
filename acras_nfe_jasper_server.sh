#!/bin/sh -e

### BEGIN INIT INFO
# Provides:             acras_nfe_jasper_server 
# Required-Start:       $local_fs $remote_fs $network
# Required-Stop:        $local_fs $remote_fs $network
# Default-Start:        2 3 4 5
# Default-Stop:         0 1 6
# Short-Description:    Acras NF-e Jasper Server
### END INIT INFO

HELPER_DIR=/home/romulo/projects/java/dist
NFE_DIR=/home/romulo
JSVC_USER=romulo
VERBOSE=

JAVA_HOME=/usr/lib/jvm/java-6-openjdk/jre
JSVC_CP=../jasper_libs/barbecue-1.1.jar:../jasper_libs/commons-beanutils-1.7.jar./jasper_libs/commons-collections-2.1.jar:../jasper_libs/commons-logging-1.0.2.jar:../jasper_libs/iReport.jar:../jasper_libs/itext-1.3.1.jar:../jasper_libs/jasperreports-3.0.0.jar:../jasper_libs/xalan.jar:../java/acras.jar:../java/acras-report.jar:app/reports

LOG_FILE=${HELPER_DIR}/jasper_server.log
PID_FILE=${HELPER_DIR}/jasper_server.pid
JSVC_CLASS=br.com.acras.report.JasperServer
JSVC_ARGS="-p 9981"

do_start()
{
  chdir $HELPER_DIR
  LANG=pt_BR.UTF-8 jsvc $VERBOSE -home $JAVA_HOME -user $JSVC_USER -outfile $LOG_FILE -errfile '&1' -pidfile $PID_FILE -cp $JSVC_CP $JSVC_CLASS $JSVC_ARGS
  chdir -
}

do_stop()
{
  chdir $HELPER_DIR
  LANG=pt_BR.UTF-8 jsvc $VERBOSE -home $JAVA_HOME -user $JSVC_USER -stop -pidfile $PID_FILE -cp $JSVC_CP $JSVC_CLASS
  chdir -
}

case "$1" in
    start)
        do_start
        ;;
    stop)
        do_stop
        ;;
    restart)
        do_stop
        do_start
        ;;
    force-reload | reload)
        do_stop
        do_start
        ;;
    status)
        if [ -e $PID_FILE ]; then
            echo "Serviço iniciado.";
        else
            echo "Serviço parado.";
        fi
        ;;
    *)
        echo "Uso: $0 {start|stop|restart|reload|force-reload|status}"
        exit 1
        ;;
esac

exit 0
