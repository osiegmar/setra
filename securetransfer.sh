#!/bin/bash
set -e
JAVA_HEAP_MIN=${JAVA_HEAP_MIN:="-Xms32m"}
JAVA_HEAP_MAX=${JAVA_HEAP_MAX:="-Xmx54613K"}
JAVA_HEAP_MAX=${JAVA_METASPACE_MAX:="-XX:MaxMetaspaceSize=64M"}
JAVA_HEAP_MAX=${JAVA_STACK_MAX:="-Xss568K"}

JAVA_OPTS="$JAVA_HEAP_MIN $JAVA_HEAP_MAX $JAVA_METASPACE_MAX $JAVA_STACK_MAX -Djava.security.egd=file:/dev/./urandom"

java $JAVA_OPTS -cp /app org.springframework.boot.loader.JarLauncher
