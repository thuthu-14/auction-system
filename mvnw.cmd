@echo off
setlocal

if "%JAVA_HOME%" == "" goto javaHomeMissing
if not exist "%JAVA_HOME%\bin\java.exe" goto javaHomeInvalid

set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%" == "\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" goto wrapperMissing

"%JAVA_HOME%\bin\java.exe" ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

exit /b %ERRORLEVEL%

:javaHomeMissing
echo Error: JAVA_HOME not found in your environment. 1>&2
exit /b 1

:javaHomeInvalid
echo Error: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
exit /b 1

:wrapperMissing
echo Error: Maven wrapper jar not found: %WRAPPER_JAR% 1>&2
exit /b 1
