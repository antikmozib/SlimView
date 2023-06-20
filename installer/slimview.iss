#define MyAppName "SlimView"
#define MyAppVersion Copy(GetVersionNumbersString("..\target\dist\slimview.exe"), 1, Len(GetVersionNumbersString("..\target\dist\slimview.exe")) - 2)
#define MyAppPublisher "Antik Mozib"
#define MyAppURL "https://mozib.io/slimview"
#define MyAppExeName "slimview.exe"
#dim MyAppExtensions[11] {'bmp', 'png', 'gif', 'jpeg', 'jpg', 'tiff', 'ico', 'cur', 'psd', 'psb', 'webp'}
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
LicenseFile=gpl-3.0.rtf
OutputDir=output
OutputBaseFilename={#MyAppName}-{#MyAppVersion}-setup
Compression=lzma
SolidCompression=yes
UninstallDisplayName={#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}
ChangesAssociations=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags:

[Files]
Source: "..\attributions.txt"; DestDir: "{app}"
Source: "..\target\dist\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
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

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
  begin
    if MsgBox('Do you want to remove all user-related preferences as well?', mbConfirmation, MB_YESNO or MB_DEFBUTTON2) = IDYES then
    begin
      DelTree(ExpandConstant('{%USERPROFILE}\.slimview'), True, True, True);
      RegDeleteKeyIncludingSubkeys(HKCU, 'SOFTWARE\JavaSoft\Prefs\io\mozib\slimview');
    end;
  end;
end;

#expr SaveToFile(AddBackslash(SourcePath) + "Preprocessed.iss")