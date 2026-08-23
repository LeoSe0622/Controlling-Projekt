@echo off
rem ===========================================================================
rem  Controlling-Monatsbericht - zum Reinziehen
rem
rem  Eine CSV-Datei auf diese Datei ziehen. Windows uebergibt den Pfad als
rem  erstes Argument (%1), der Parser nimmt ihn ohne --input entgegen.
rem
rem  Vorher einmal bauen:  gradlew installDist
rem ===========================================================================

rem %~dp0 ist der Ordner DIESER Datei. Ohne das wuerde der Aufruf nur klappen,
rem wenn das Arbeitsverzeichnis zufaellig stimmt - und beim Reinziehen ist das
rem irgendein Windows-Systemordner, nie der Projektordner.
set PROGRAMM=%~dp0build\install\controlling-report\bin\controlling-report.bat

if not exist "%PROGRAMM%" (
    echo Das Programm wurde noch nicht gebaut.
    echo Fuehre im Projektordner einmal aus:  gradlew installDist
    echo.
    pause
    exit /b 3
)

if "%~1"=="" (
    echo Keine Datei uebergeben.
    echo.
    echo So geht es: Zieh eine CSV-Datei auf diese Datei.
    echo.
    pause
    exit /b 3
)

rem %~1 statt %1: entfernt die Anfuehrungszeichen, die Windows um Pfade mit
rem Leerzeichen legt - wir setzen unsere eigenen.
call "%PROGRAMM%" "%~1"

rem Den Exit-Code des Programms merken. Jeder weitere Befehl wuerde ihn
rem ueberschreiben, deshalb sofort sichern.
set CODE=%ERRORLEVEL%

echo.
if %CODE%==0 echo Fertig - keine Beanstandungen.
if %CODE%==1 echo Fertig - mit Warnungen. Siehe Tab "Datenqualitaet".
if %CODE%==2 echo Fertig - es gab Fehlerzeilen. Siehe Tab "Datenqualitaet".
if %CODE%==3 echo ABGEBROCHEN - es wurde kein Bericht erzeugt.

echo.
pause
exit /b %CODE%
