#define MyAppName "SlimView"
#define MyAppVersion GetVersionNumbersString("..\target\bin\slimview.exe")
#define MyAppPublisher "Antik Mozib"
#define MyAppURL "https://mozib.io/slimview"
#define MyAppExeName "slimview.exe"
#dim MyAppExtensions[10] {'bmp', 'png', 'gif', 'jpeg', 'jpg', 'tiff', 'ico', 'cur', 'psd', 'psb'}
#define I

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
DefaultDirName={commonpf}\{#MyAppName}
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
Source: "..\target\bin\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Registry]
#sub RegisterAssociation
  Root: HKCR; Subkey: ".{#MyAppExtensions[I]}"; ValueData: "{#MyAppName}.{#MyAppExtensions[I]}"; Flags: uninsdeletevalue; ValueType: string; ValueName: ""
  Root: HKCR; Subkey: "{#MyAppName}.{#MyAppExtensions[I]}"; ValueData: "{#MyAppName} {#Uppercase(MyAppExtensions[I])} Image"; Flags: uninsdeletekey; ValueType: string; ValueName: ""
  Root: HKCR; Subkey: "{#MyAppName}.{#MyAppExtensions[I]}\DefaultIcon"; ValueData: "{app}\{#MyAppExeName},0"; ValueType: string; ValueName: ""
  Root: HKCR; Subkey: "{#MyAppName}.{#MyAppExtensions[I]}\shell\open\command"; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; ValueType: string; ValueName: ""
#endsub

#for {I = 0; I < DimOf(MyAppExtensions); I++} RegisterAssociation

; Delete Java preferences
Root: HKCU; Subkey: "SOFTWARE\JavaSoft\Prefs\io\mozib\slimview"; Flags: dontcreatekey uninsdeletekey

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{%USERPROFILE}\.slimview"

#expr SaveToFile(AddBackslash(SourcePath) + "Preprocessed.iss")