[Setup]
AppName=Disk Utility
AppVersion=1.0
DefaultDirName={pf}\Disk Utility
DefaultGroupName=Disk Utility
OutputDir=.\target
OutputBaseFilename=DiskUtilitySetup
SetupIconFile=src\main\resources\icons\app_icon.ico
Compression=lzma2
SolidCompression=yes

[Files]
; Головний JAR
Source: "target\disk-utility-1.0-SNAPSHOT.jar"; DestDir: "{app}"; Flags: ignoreversion

; Лаунчер батник та VBS
Source: "launch.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "launch.vbs"; DestDir: "{app}"; Flags: ignoreversion

; JavaFX SDK (шлях оновлено)
Source: "C:\javafx-sdk-24.0.1\javafx-sdk-24.0.1\lib\*"; DestDir: "{app}\lib"; Flags: recursesubdirs ignoreversion
Source: "C:\javafx-sdk-24.0.1\javafx-sdk-24.0.1\bin\*"; DestDir: "{app}\bin"; Flags: recursesubdirs ignoreversion

; Іконка
Source: "src\main\resources\icons\app_icon.ico"; DestDir: "{app}\icons"; Flags: ignoreversion

[Icons]
; Ярлики на запуск через launch.vbs — щоб не було консольного вікна
Name: "{group}\Disk Utility"; Filename: "{app}\launch.vbs"; IconFilename: "{app}\icons\app_icon.ico"
Name: "{commondesktop}\Disk Utility"; Filename: "{app}\launch.vbs"; IconFilename: "{app}\icons\app_icon.ico"

[Run]
; Автоматичний запуск після встановлення
Filename: "wscript.exe"; Description: "Запустити Disk Utility"; Flags: postinstall nowait
