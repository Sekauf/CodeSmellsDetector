@echo off
set SONAR_TOKEN=
set SONAR_HOST_URL=http://localhost:9000
start javaw -jar "%~dp0target\CodeSmellsDetector-1.0-SNAPSHOT-gui.jar"
