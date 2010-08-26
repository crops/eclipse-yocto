#!/bin/sh

help ()
{
  echo "Usage $0 command [options] application [application argument]"
  echo "command:"
  echo "  start - start an application"
  echo "  stop - stop an application"
  echo "  restart - restart an application"
  echo ""
  echo "options: -d | -l <log file>"
  echo "  -d  - start an application as a singleton daemon"
  echo "  -l <log file name> - redirect the standard output/error in the the file" 
  echo "  note: Option -d and -l are exclusive to each other"
  exit 1
}

killproc() {        # kill the named process(es)
    pid=`/bin/pidof $1`
    [ "$pid" != "" ] && kill $pid
}

start () 
{
        if [ "x$DAEMON" != "x" ]; then
            start-stop-daemon -S -b --oknodo -x $APP -- $APPARG
        elif [ "x$LOGFILE" != "x" ]; then
            $APP $APPARG $>${LOGFILE}
        else
            $APP $APPARG
        fi
}

stop ()
{
  if [ "x$DAEMON" != "x" ]; then
    start-stop-daemon -K -x $APP
  else
    count=0
    while [ -n "`/bin/pidof $APP`" -a $count -lt 10 ] ; do
      killproc $APP >& /dev/null
      sleep 1
      RETVAL=$?
      if [ $RETVAL != 0 -o -n "`/bin/pidof $APP`" ] ; then
          sleep 3
      fi
      count=`expr $count + 1`
    done
  fi
}

restart ()
{
  stop
  sleep 1
  start
}

#get command
case $1 in 
start) CMD=$1; shift 1
    ;;
stop)  CMD=$1; shift 1
    ;;
*)  help
    ;;
esac

#get options
while [ $# -gt 0 ]; do
  case $1 in
  -d) DAEMON=true; shift 1
    ;; 
  -l) LOGFILE=$2; shift 2
    ;;
  *) break
    ;;
  esac
done

#get application
APP_NAME=$1
shift 1

#get app argument
APPARG="$@"

#validate options
if [ "x$DAEMON" != "x" -a "x$LOGFILE" != "x" ]; then
  help
fi
if [ "x$DAEMON" != "x" ]; then
  APP=`which $APP_NAME`
fi
if [ "x$APP" == "x" ]; then
  help
fi

#run script
case $CMD in 
start) start
    ;;
stop) stop
    ;;
restart)
    restart
    ;;
esac
