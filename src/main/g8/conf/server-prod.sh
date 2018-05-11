#!/bin/bash

# For Production Env                                                            #
# ----------------------------------------------------------------------------- #
# Start/Stop script on *NIX                                                     #
# ----------------------------------------------------------------------------- #
# Command-line arguments:                                                       #
# -h help and exist                                                             #
#    --pid <path-to-.pid-file>                                                  #
# -m|--mem <max-memory-in-mb>                                                   #
# -c|--conf <path-to-config-file.conf>                                          #
# -l|--logconf <path-to-logback-file.xml>                                       #
#    --logdir <path-to-log-directory>, env app.logdir will be set to this value #
# -j|--jvm "extra-jvm-options"                                                  #
# ----------------------------------------------------------------------------- #

# from http://stackoverflow.com/questions/242538/unix-shell-script-find-out-which-directory-the-script-file-resides
pushd \$(dirname "\${0}") > /dev/null
_basedir=\$(pwd -L)
popd > /dev/null

# Setup common environment variables
# Override environment variables if needed after the next line
. \$_basedir/env.sh

DEFAULT_APP_LOGBACK=logback-prod.xml

APP_LOGBACK=\$DEFAULT_APP_LOGBACK

doStart() {
    preStart

    RUN_CMD=(\$APP_HOME/bin/\$APP_NAME -Dapp.home=\$APP_HOME -Dapp.logdir=\$APP_LOGDIR)
    RUN_CMD+=(-Dpidfile.path=\$APP_PID)
    if [ "\$APP_PROXY_HOST" != "" -a "\$APP_PROXY_PORT" != "0" ]; then
        RUN_CMD+=(-Dhttp.proxyHost=\$APP_PROXY_HOST -Dhttp.proxyPort=\$APP_PROXY_PORT)
        RUN_CMD+=(-Dhttps.proxyHost=\$APP_PROXY_HOST -Dhttps.proxyPort=\$APP_PROXY_PORT)
    fi
    if [ "\$APP_PROXY_USER" != "" ]; then
        RUN_CMD+=(-Dhttp.proxyUser=\$APP_PROXY_USER -Dhttp.proxyPassword=\$APP_PROXY_PASSWORD)
    fi
    if [ "\$APP_NOPROXY_HOST" != "" ]; then
        RUN_CMD+=(-Dhttp.nonProxyHosts=\$APP_NOPROXY_HOST)
    fi
    RUN_CMD+=(-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -J-server -J-Xms\${APP_MEM}m -J-Xmx\${APP_MEM}m)
    RUN_CMD+=(-J-XX:+UseThreadPriorities -J-XX:ThreadPriorityPolicy=42 -J-XX:+HeapDumpOnOutOfMemoryError -J-Xss256k)
    RUN_CMD+=(-J-XX:+UseTLAB -J-XX:+ResizeTLAB -J-XX:+UseNUMA -J-XX:+PerfDisableSharedMem)
    RUN_CMD+=(-J-XX:+UseG1GC -J-XX:G1RSetUpdatingPauseTimePercent=5 -J-XX:MaxGCPauseMillis=500)
    RUN_CMD+=(-J-XX:+PrintGCDetails -J-XX:+PrintGCDateStamps -J-XX:+PrintHeapAtGC -J-XX:+PrintTenuringDistribution)
    RUN_CMD+=(-J-XX:+PrintGCApplicationStoppedTime -J-XX:+PrintPromotionFailure -J-XX:PrintFLSStatistics=1)
    RUN_CMD+=(-J-Xloggc:\${APP_LOGDIR}/gc.log -J-XX:+UseGCLogFileRotation -J-XX:NumberOfGCLogFiles=10 -J-XX:GCLogFileSize=10M)
    RUN_CMD+=(-Dconfig.file=\$FINAL_APP_CONF -Dlogback.configurationFile=\$FINAL_APP_LOGBACK)
    RUN_CMD+=(\$JVM_EXTRA_OPS)

    echo "APP_MEM             : \$APP_MEM"
    echo "APP_CONF            : \$FINAL_APP_CONF"
    echo "APP_LOGBACK         : \$FINAL_APP_LOGBACK"
    echo "APP_LOGDIR          : \$APP_LOGDIR"
    echo "APP_PID             : \$APP_PID"
    echo "JVM_EXTRA_OPS       : \$JVM_EXTRA_OPS"
    
    execStartBackground \${RUN_CMD[@]}
    echo "STARTED \$APP_NAME `date`"
}

ACTION=\$1
shift

while [ "\$1" != "" ]; do
    PARAM=\$1
    shift
    VALUE=\$1
    shift

    parseParam \$PARAM \$VALUE
done

doAction \$ACTION
