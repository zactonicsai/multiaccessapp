#!/bin/sh
# Maven Wrapper script

set -e

# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR" > /dev/null; pwd`

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$BASEDIR}"
export MAVEN_PROJECTBASEDIR

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -r "$WRAPPER_PROPERTIES" ]; then
  WRAPPER_URL=`sed -n 's/^wrapperUrl=//p' "$WRAPPER_PROPERTIES" | tr -d '\r'`
  DISTRIBUTION_URL=`sed -n 's/^distributionUrl=//p' "$WRAPPER_PROPERTIES" | tr -d '\r'`
fi

# Download maven-wrapper.jar if not exists
if [ ! -f "$WRAPPER_JAR" ]; then
  if command -v wget > /dev/null; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL" || rm -f "$WRAPPER_JAR"
  elif command -v curl > /dev/null; then
    curl -s -o "$WRAPPER_JAR" "$WRAPPER_URL" || rm -f "$WRAPPER_JAR"
  fi
fi

# If wrapper jar exists, use it. Otherwise, try to use system Maven
if [ -f "$WRAPPER_JAR" ]; then
  MAVEN_CMD="java -jar $WRAPPER_JAR"
elif command -v mvn > /dev/null; then
  MAVEN_CMD="mvn"
else
  echo "Maven wrapper not found and Maven not installed."
  echo "Downloading Maven directly..."
  
  MAVEN_HOME="$MAVEN_PROJECTBASEDIR/.mvn/maven"
  MAVEN_TAR="$MAVEN_PROJECTBASEDIR/.mvn/maven.tar.gz"
  
  mkdir -p "$MAVEN_HOME"
  
  if command -v wget > /dev/null; then
    wget -q -O "$MAVEN_TAR" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz"
  elif command -v curl > /dev/null; then
    curl -s -o "$MAVEN_TAR" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz"
  fi
  
  tar -xzf "$MAVEN_TAR" -C "$MAVEN_HOME" --strip-components=1
  rm -f "$MAVEN_TAR"
  
  MAVEN_CMD="$MAVEN_HOME/bin/mvn"
fi

exec $MAVEN_CMD "$@"
