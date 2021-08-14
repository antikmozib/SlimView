#define MyAppName "SlimView"
#define MyAppVersion GetVersionNumbersString("..\target\bin\slimview.exe")
#define MyAppPublisher "Antik Mozib"
#define MyAppURL "https://mozib.io/slimview"
#define MyAppExeName "slimview.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{9DAEB0E1-66A2-4C0D-9400-5B7EF97D115E}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
LicenseFile=gpl-3.0.txt
OutputDir=output
OutputBaseFilename={#MyAppName}-{#MyAppVersion}-setup
Compression=lzma
SolidCompression=yes
UninstallDisplayName={#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}
ChangesAssociations=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\target\bin\slimview.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\notice.txt"; DestDir: "{app}"
Source: "..\target\bin\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\target\bin\lib\commons-codec-1.11.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\target\bin\lib\commons-io-2.9.0.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\target\bin\lib\commons-logging-1.2.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\target\bin\lib\httpclient-4.5.13.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\target\bin\lib\httpcore-4.4.13.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\target\bin\lib\imgscalr-lib-4.2.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\target\bin\lib\metadata-extractor-2.16.0.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\target\bin\lib\xmpcore-6.1.11.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Registry]
Root: HKCR; Subkey: ".jpg"; ValueData: "{#MyAppName}.jpg"; Flags: uninsdeletevalue; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.jpg"; ValueData: "{#MyAppName} JPG Image"; Flags: uninsdeletekey; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.jpg\DefaultIcon"; ValueData: "{app}\{#MyAppExeName},0"; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.jpg\shell\open\command"; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; ValueType: string; ValueName: ""

Root: HKCR; Subkey: ".jpeg"; ValueData: "{#MyAppName}.jpeg"; Flags: uninsdeletevalue; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.jpeg"; ValueData: "{#MyAppName} JPEG Image"; Flags: uninsdeletekey; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.jpeg\DefaultIcon"; ValueData: "{app}\{#MyAppExeName},0"; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.jpeg\shell\open\command"; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; ValueType: string; ValueName: ""

Root: HKCR; Subkey: ".gif"; ValueData: "{#MyAppName}.gif"; Flags: uninsdeletevalue; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.gif"; ValueData: "{#MyAppName} GIF Image"; Flags: uninsdeletekey; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.gif\DefaultIcon"; ValueData: "{app}\{#MyAppExeName},0"; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.gif\shell\open\command"; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; ValueType: string; ValueName: ""

Root: HKCR; Subkey: ".png"; ValueData: "{#MyAppName}.png"; Flags: uninsdeletevalue; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.png"; ValueData: "{#MyAppName} PNG Image"; Flags: uninsdeletekey; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.png\DefaultIcon"; ValueData: "{app}\{#MyAppExeName},0"; ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#MyAppName}.png\shell\open\command"; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; ValueType: string; ValueName: ""


[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{%USERPROFILE}\.slimview"