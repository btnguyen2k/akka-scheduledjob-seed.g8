#!/bin/bash

# Common Environment Settings #

# from http://stackoverflow.com/questions/242538/unix-shell-script-find-out-which-directory-the-script-file-resides
pushd \$(dirname "\${0}") > /dev/null
_basedir=\$(pwd -L)
popd > /dev/null

APP_HOME=\$_basedir/..
APP_NAME=$name;format="normalize"$

# Setup proxy from environment properties
APP_PROXY_HOST="\$PROXY_HOST"
APP_PROXY_PORT="\$PROXY_PORT"
APP_NOPROXY_HOST="\$NOPROXY_HOST"
APP_PROXY_USER="\$PROXY_USER"
APP_PROXY_PASSWORD="\$PROXY_PASSWORD"

DEFAULT_APP_MEM=64
DEFAULT_APP_CONF=application.conf
DEFAULT_APP_LOGBACK=logback-dev.xml
DEFAULT_APP_PID=\$APP_HOME/\$APP_NAME.pid
DEFAULT_APP_LOGDIR=\$APP_HOME/logs

APP_MEM=\$DEFAULT_APP_MEM
APP_CONF=\$DEFAULT_APP_CONF
APP_LOGBACK=\$DEFAULT_APP_LOGBACK
APP_PID=\$DEFAULT_APP_PID
APP_LOGDIR=\$DEFAULT_APP_LOGDIR

JVM_EXTRA_OPS=

isRunning() {
    local PID=\$(cat "\$1" 2>/dev/null) || return 1
    kill -0 "\$PID" 2>/dev/null
}

preStart() {
    if [ "\$APP_PID" == "" ]; then
        echo "ERROR: PID file not specified!"
        exit 1
    fi
    if [ -f "\$APP_PID" ]; then
        if isRunning \$APP_PID; then
            echo "Already running!"
            exit 1
        else
            # dead pid file - remove
            rm -f "\$APP_PID"
        fi
    fi

    if [ "\$APP_LOGDIR" == "" ]; then
        echo "ERROR: Log directory not specified!"
        exit 1
    else
        mkdir -p \$APP_LOGDIR
        if [ ! -d \$APP_LOGDIR ]; then
            echo "ERROR: Log directory \$APP_LOGDIR cannot be created or not a writable directory!"
        fi
    fi

    local _startsWithSlash_='^\/.*\$'

    if [ "\$APP_CONF" == "" ]; then
        echo "ERROR: Application configuration file not specified!"
        exit 1
    else
        if [[ \$APP_CONF =~ \$_startsWithSlash_ ]]; then
            FINAL_APP_CONF=\$APP_CONF
        else
            FINAL_APP_CONF=\$APP_HOME/conf/\$APP_CONF
        fi

        if [ ! -f "\$FINAL_APP_CONF" ]; then
            echo "ERROR: Application configuration file not found: \$FINAL_APP_CONF"
            exit 1
        fi
    fi

    if [ "\$APP_LOGBACK" == "" ]; then
        echo "ERROR: Application logback config file not specified!"
        exit 1
    else
        if [[ \$APP_LOGBACK =~ \$_startsWithSlash_ ]]; then
            FINAL_APP_LOGBACK=\$APP_LOGBACK
        else
            FINAL_APP_LOGBACK=\$APP_HOME/conf/\$APP_LOGBACK
        fi

        if [ ! -f "\$FINAL_APP_LOGBACK" ]; then
            echo "ERROR: Application logback config file not found: \$FINAL_APP_LOGBACK"
            exit 1
        fi
    fi
}

execStartBackground() {
    local CMD=(\$1)
    shift
    while [ "\$1" != "" ]; do
        CMD+=(\$1)
        shift
    done

    echo -n "Starting \$APP_NAME: "

    "\${CMD[@]}" &
    disown \$!
    echo \$! > "\$APP_PID"
}

execStartForeground() {
    local CMD=(\$1)
    shift
    while [ "\$1" != "" ]; do
        CMD+=(\$1)
        shift
    done

    echo -n "Starting \$APP_NAME: "

    "\${CMD[@]}"
}

usageAndExit() {
    echo "Usage: \${0##*/} <{start|stop|restart}> [-h] [--pid <.pid file>] [--logdir <log directory>] [-m <memory limit in mb>] [-c <custom config file>] [-l <custom logback config>] [-j \"<extra jvm options>\"]"
    echo "    stop   : stop the server"
    echo "    start  : start the server"
    echo "    restart: restart the server"
    echo "       -h or --help          : Display this help screen"
    echo "       -m or --mem           : JVM memory limit in mb (default \$DEFAULT_APP_MEM)"
    echo "       -c or --conf          : Custom app config file, relative file is prefixed with ./conf (default \$DEFAULT_APP_CONF)"
    echo "       -l or --logconf       : Custom logback config file, relative file is prefixed with ./conf (default \$DEFAULT_APP_LOGBACK)"
    echo "       -j or --jvm           : Extra JVM options (example: \"-Djava.rmi.server.hostname=localhost)\""
    echo "       --pid                 : Specify application's .pid file (default \$DEFAULT_APP_PID)"
    echo "       --logdir              : Specify application's log directory (default \$DEFAULT_APP_LOGDIR)"
    echo
    echo "Example: start server 64mb memory limit, with custom configuration file"
    echo "    \${0##*/} start -m 64 -c abc.conf"
    echo
    exit 1
}

doStop() {
    echo -n "Stopping \$APP_NAME: "

    if isRunning \$APP_PID; then
        local PID=\$(cat "\$APP_PID" 2>/dev/null)
        kill "\$PID" 2>/dev/null
        
        TIMEOUT=30
        while isRunning \$APP_PID; do
            if (( TIMEOUT-- == 0 )); then
                kill -KILL "\$PID" 2>/dev/null
            fi
            sleep 1
        done
        
        rm -f "\$APP_PID"
    fi
    
    echo OK
}

handleUnknownParam() {
    local PARAM=\$1
    shift
    local VALUE=\$1
    shift

    echo "ERROR: unknown parameter \"\$PARAM\""
    usageAndExit
}

parseParam() {
    # parse parameters: see https://gist.github.com/jehiah/855086
    local _number_='^[0-9]+\$'
    local PARAM=\$1
    shift
    local VALUE=\$1
    shift

    case \$PARAM in
        -h|--help)
            usageAndExit
            ;;

        --pid)
            APP_PID=\$VALUE
            ;;

        -m|--mem)
            APP_MEM=\$VALUE
            if ! [[ \$VALUE =~ \$_number_ ]]; then
                echo "ERROR: invalid memory value \"\$VALUE\""
                usageAndExit
            fi
            ;;

        -c|--conf)
            APP_CONF=\$VALUE
            ;;

        -l|--logconf)
            APP_LOGBACK=\$VALUE
            ;;

        --logdir)
            APP_LOGDIR=\$VALUE
            ;;

        -j)
            JVM_EXTRA_OPS=\$VALUE
            ;;

        *)
            handleUnknownParam \$PARAM \$VALUE
            ;;
    esac
}

doAction() {
    local ACTION=\$1

    case "\$ACTION" in
        stop)
            doStop
            ;;

        start)
            doStart
            ;;

        restart)
            doStop
            doStart
            ;;

        *)
            usageAndExit
            ;;
    esac
}
