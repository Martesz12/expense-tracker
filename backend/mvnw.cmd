@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.

@if "%DEBUG%" == "" @echo off
@setlocal

set ERROR_CODE=0

:init
@REM Find the project base dir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current directory if not found.

set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" goto endDetectBaseDir

set EXEC_DIR=%CD%
set WDIR=%EXEC_DIR%
:findBaseDir
IF EXIST "%WDIR%"\.mvn goto baseDirFound
cd ..
IF "%WDIR%"=="%CD%" goto baseDirNotFound
set WDIR=%CD%
goto findBaseDir

:baseDirFound
set MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
set MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir
IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" goto readMavenProjectBaseDirEnvEnd

@setlocal EnableExtensions EnableDelayedExpansion
for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") do set JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a
@endlocal & set JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS%

:readMavenProjectBaseDirEnvEnd
IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\maven.config" goto readMavenProjectBaseDir

@setlocal EnableExtensions EnableDelayedExpansion
for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\maven.config") do set MAVEN_CONFIG=!MAVEN_CONFIG! %%a
@endlocal & set MAVEN_CONFIG=%MAVEN_CONFIG%

:readMavenProjectBaseDir
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%

IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\pom.xml" goto endDetectPomDir
set WDIR=%MAVEN_PROJECTBASEDIR%
goto endDetectPomDir

:endDetectPomDir

SET "jvmFlags=!JVM_CONFIG_MAVEN_PROPS!"

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
set DOWNLOAD_URL="https://repo.maven.apache.org/maven2"

FOR /F "usebackq tokens=1,* delims==" %%A in ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") do (
    if "%%A"=="distributionUrl" (
        set DOWNLOAD_URL=%%B
    )
    if "%%A"=="snapshotUrl" (
        set SNAPSHOT_URL=%%B
    )
)

@REM Determine the Java command to use in order to perform the actual download, i.e. download.
if not "%JAVA_HOME%" == "" (
    set "javaExecutable=%JAVA_HOME%/bin/java.exe"
) else (
    set "javaExecutable=java.exe"
    %javaExecutable% -version >NUL 2>&1
    if errorlevel 1 (
        echo.
        echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
        echo.
        echo Please set the JAVA_HOME variable in your environment to match the
        echo location of your Java installation.
        goto error
    )
)

if exist "%WRAPPER_JAR%" (
    set "jarUrl=jar:file:///%WRAPPER_JAR%"
) else (
    for /f "usebackq tokens=*" %%a in ('%javaExecutable% -cp ."%WRAPPER_JAR%" "org.apache.maven.wrapper.MavenWrapperMain" --jar-location "%WRAPPER_JAR%"') do set jarUrl=%%a
)

%javaExecutable% ^
  %JVM_CONFIG_MAVEN_PROPS% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  "-Dorg.slf4j.simpleLogger.defaultLogLevel=info" ^
  "-Dbasedir=%MAVEN_PROJECTBASEDIR%" ^
  -classpath "%WRAPPER_JAR%" ^
  "%WRAPPER_LAUNCHER%" %*

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Maven execution failed with exit code %errorlevel%
    echo.
    goto error
)

goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /b %ERROR_CODE%
