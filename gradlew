#!/bin/sh
set -e
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  mkdir -p gradle/wrapper
  curl -sL https://github.com/gradle/gradle/raw/v8.6.0/gradle/wrapper/gradle-wrapper.jar -o "$WRAPPER_JAR"
fi
exec java -jar "$WRAPPER_JAR" "$@"
