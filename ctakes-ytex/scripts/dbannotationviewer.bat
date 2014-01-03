@setlocal
@call %~dp0../../setenv.bat
java -cp %CLASSPATH% org.apache.ctakes.ytex.tools.DBAnnotationViewerMain
@endlocal