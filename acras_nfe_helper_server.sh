#!/bin/sh -e

### BEGIN INIT INFO
# Provides:             acras_nfe_helper_server 
# Required-Start:       $local_fs $remote_fs $network
# Required-Stop:        $local_fs $remote_fs $network
# Default-Start:        2 3 4 5
# Default-Stop:         0 1 6
# Short-Description:    Acras NF-e Helper Server
### END INIT INFO

HELPER_DIR=/home/romulo/projects/java/dist
NFE_DIR=/home/romulo
JSVC_USER=romulo
VERBOSE=

JAVA_HOME=/usr/lib/jvm/java-6-openjdk/jre
JSVC_CP=/usr/share/java/commons-daemon.jar:${HELPER_DIR}/acras.jar

LOG_FILE=${HELPER_DIR}/jsvc.log
PID_FILE=${HELPER_DIR}/jsvc.pid
JSVC_CLASS=br.com.acras.nfe.HelperServer
JSVC_ARGS="-w ${NFE_DIR}"

do_start()
{
  LANG=pt_BR.UTF-8 jsvc $VERBOSE -home $JAVA_HOME -user $JSVC_USER -outfile $LOG_FILE -errfile '&1' -pidfile $PID_FILE -cp $JSVC_CP $JSVC_CLASS $JSVC_ARGS
}

do_stop()
{
  LANG=pt_BR.UTF-8 jsvc $VERBOSE -home $JAVA_HOME -user $JSVC_USER -stop -pidfile $PID_FILE -cp $JSVC_CP $JSVC_CLASS
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
