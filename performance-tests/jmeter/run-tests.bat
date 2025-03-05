@echo off
SET JMETER_HOME=C:\workspace\apache-jmeter-5.6.3
SET TEST_PLAN=test-plans/transaction-api-test.jmx
SET RESULTS_DIR=results

:: Clean previous results
echo Cleaning previous results...
if exist %RESULTS_DIR%\detailed\results.csv del /f /q %RESULTS_DIR%\detailed\results.csv
if exist %RESULTS_DIR%\html-report rmdir /s /q %RESULTS_DIR%\html-report

:: Create directories if they don't exist
if not exist %RESULTS_DIR% mkdir %RESULTS_DIR%
if not exist %RESULTS_DIR%\detailed mkdir %RESULTS_DIR%\detailed
if not exist %RESULTS_DIR%\html-report mkdir %RESULTS_DIR%\html-report

echo Running JMeter Tests...
"%JMETER_HOME%\bin\jmeter" -n ^
    -t %TEST_PLAN% ^
    -l %RESULTS_DIR%\detailed\results.csv ^
    -e -o %RESULTS_DIR%\html-report ^
    -Jjmeter.save.saveservice.output_format=csv ^
    -Jjmeter.save.saveservice.default_delimiter=, ^
    -Jjmeter.save.saveservice.print_field_names=true ^
    -Jjmeter.save.saveservice.response_data=true ^
    -Jjmeter.save.saveservice.samplerData=true ^
    -Jjmeter.save.saveservice.requestHeaders=true ^
    -Jjmeter.save.saveservice.url=true ^
    -Jjmeter.save.saveservice.responseHeaders=true

IF %ERRORLEVEL% NEQ 0 (
    echo Test execution failed
    exit /b 1
)

echo Test completed successfully
echo Results saved in: %RESULTS_DIR%
echo HTML Report available at: %RESULTS_DIR%\html-report\index.html

:: Clear JMeter from memory
taskkill /F /IM java.exe /FI "WINDOWTITLE eq JMeter*"
taskkill /F /IM jmeter.exe

pause