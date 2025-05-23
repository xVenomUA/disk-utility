@echo off
cd /d %~dp0
java -Djava.library.path="%~dp0bin" --module-path "%~dp0lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -jar disk-utility-1.0-SNAPSHOT.jar
pause