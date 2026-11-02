# CleanYourTV v3.0.0 — Prabhas wallpaper-only patch

This patch keeps the original CleanYourTV launcher UI and behavior intact except for its wallpaper source/selection:
- Bundles the five user-provided Prabhas images locally.
- Picks a random image when Home is first created and a different image whenever Home resumes after being hidden.
- Excludes the immediately previous image.
- Disables timed wallpaper rotation.
- Does not change the launcher icon, app grid, dock, settings, focus, Inputs, Wi-Fi, or other UI.

## Apply

1. Download and extract the official CleanYourTV v3.0.0 source:
   https://github.com/Gathin23/cleanyourtv/archive/refs/tags/v3.0.0.zip
2. Extract this patch ZIP too.
3. Open PowerShell and run (replace the source path with your actual extracted folder):

   `powershell -ExecutionPolicy Bypass -File .\apply-prabhas-wallpapers.ps1 -SourceRoot "C:\path\to\cleanyourtv-3.0.0"`

4. Build using the repository's normal build process. For Windows, from the source root:

   `powershell -ExecutionPolicy Bypass -File .\launcher\build-and-install.ps1 -TvHost YOUR_TV_IP`

   This installer can set the launcher as Home and may disable the stock launcher after verifying the handover. If you only want to build the APK first, run `cd launcher` then `.\gradlew.bat assembleRelease`; the APK is at `launcher\app\build\outputs\apk\release\app-release.apk`.

The patch script checks that it is targeting the expected v3.0.0 Java source and will stop instead of writing if its expected code anchors are missing. Make a backup of your source folder before applying.
