[Setup]
AppName=DiskUtility
AppVersion=1.0
DefaultDirName={pf}\DiskUtility
OutputDir=.\target
OutputBaseFilename=DiskUtilitySetup
SetupIconFile=src/main/resources/icons/app_icon.ico
Compression=lzma2
SolidCompression=yes

[Files]
Source: "target\disk-utility-1.0-SNAPSHOT.jar"; DestDir: "{app}";  Flags: ignoreversion; Attribs: hidden
Source: "target\launch.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Program Files\Java\javafx-sdk-24.0.1\lib\*"; DestDir: "{app}\lib"; Flags: recursesubdirs
Source: "C:\Program Files\Java\javafx-sdk-24.0.1\bin\*"; DestDir: "{app}\bin"; Flags: recursesubdirs
Source: "src\main\resources\icons\app_icon.ico"; DestDir: "{app}\icons"; Flags: ignoreversion

[Icons]
Name: "{group}\DiskUtility"; Filename: "{app}\launch.bat"; IconFilename: "{app}\icons\app_icon.ico"
Name: "{commondesktop}\DiskUtility"; Filename: "{app}\launch.bat"; IconFilename: "{app}\icons\app_icon.ico"