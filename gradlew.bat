@if "%DEBUG%" == "" @echo off
set DIR=%~dp0
set CLASSPATH=%DIR%gradle\wrapper\gradle-wrapper.jar
set JAVA_EXE=java
"%JAVA_EXE%" -Xmx64m -Xms64m -cp "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
