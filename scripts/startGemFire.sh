#!/bin/bash

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/.." >&-
APP_HOME="`pwd -P`"
cd "$SAVED" >&-

DEFAULT_LOCATOR_MEMORY="--initial-heap=128m --max-heap=128m"

DEFAULT_SERVER_MEMORY="--initial-heap=2g --max-heap=2g"

DEFAULT_JVM_OPTS="--mcast-port=0"
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS  --J=-Djava.security.krb5.realm=GEMFIRE"
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS  --J=-Djava.security.krb5.kdc=localhost"
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS  --J=-Djava.security.auth.login.config=${APP_HOME}/jaas.conf"
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS  --J=-Djavax.security.auth.useSubjectCredsOnly=false"
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS  --J=-Djsun.security.krb5.debug=true"
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS  --classpath=${APP_HOME}/build/libs/vmware-gemfire-kerberos-0.0.1-SNAPSHOT.jar"

DEFAULT_JVM_OPTS="${DEFAULT_JVM_OPTS} --locators=localhost[10334]"

STD_SERVER_ITEMS="--server-port=0  --locator-wait-time=5 --rebalance"

rm -rf ${APP_HOME}/data/*

mkdir -p ${APP_HOME}/data/locator1
mkdir -p ${APP_HOME}/data/locator2
mkdir -p ${APP_HOME}/data/server1
mkdir -p ${APP_HOME}/data/server2
mkdir -p ${APP_HOME}/data/server3


gfsh -e "start locator ${DEFAULT_LOCATOR_MEMORY} ${DEFAULT_JVM_OPTS} --name=locator1 --port=10334 --dir=${APP_HOME}/data/locator1 --security-properties-file=${APP_HOME}/etc/gfsecurity-cluster.properties" &

wait

start_server(){
    local serverName=server${1}
    gfsh -e "start server ${DEFAULT_SERVER_MEMORY} ${DEFAULT_JVM_OPTS} --name=${serverName} --dir=${APP_HOME}/data/${serverName} ${STD_SERVER_ITEMS}  --security-properties-file=${APP_HOME}/etc/gfsecurity-cluster.properties" &
}

start_server 1

wait

# gfsh -e "connect --locator=localhost[10334]" -e "create region --name=test --type=PARTITION" -e "list members"
