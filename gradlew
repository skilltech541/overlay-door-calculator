#!/usr/bin/env sh
APP_HOME=`dirname "$0"`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_EXE=java
exec "$JAVA_EXE" -Xmx64m -Xms64m -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
