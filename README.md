<p align="center">
  <img src="https://raw.githubusercontent.com/unnamedthings07/cleanyourtv-prabhas_theme/main/launcher/app/src/main/assets/prabhas-wallpapers/prabhas-01-bahubali.jpg" alt="Prabhas — UNNAMED TV launcher wallpaper" width="900">
</p>

<h1 align="center">UNNAMED — PRABHAS EDITION</h1>

<p align="center">
  <strong>A cinematic Android TV home screen inspired by Prabhas.</strong><br>
  Prabhas wallpapers · muted cinematic intro · custom UNNAMED branding
</p>

<p align="center">
  <a href="https://github.com/unnamedthings07/cleanyourtv-prabhas_theme">Source code</a> ·
  <a href="https://github.com/Gathin23/cleanyourtv/tree/v3.0.0">CleanYourTV v3.0.0 (original project)</a>
</p>

---

## About

**UNNAMED — Prabhas Edition** is a personal Android TV launcher customization built on top of [CleanYourTV](https://github.com/Gathin23/cleanyourtv). It keeps the original launcher’s TV-friendly layout and adds a Prabhas-focused visual theme, locally bundled wallpapers, UNNAMED branding, and a short startup video.

This is an **unofficial fan project**. It is not an official CleanYourTV release and is not affiliated with or endorsed by Prabhas, his film studios, or the original CleanYourTV project.

## Features

- **UNNAMED branding** — the app is labelled `UNNAMED` and uses the separate Android application ID `com.unnamed.tvlauncher`, allowing it to coexist with the original app when installed as configured in this source.
- **Five built-in Prabhas wallpapers** — images inspired by *Baahubali*, *Salaar*, and *Fauzi* are bundled locally, so the launcher does not need the internet to display them.
- **TV-friendly wallpaper framing** — the image treatment is designed to keep the main subject visible on a widescreen TV, using blurred side-fill where needed rather than aggressively zooming into the image.
- **Cinematic startup intro** — `unnamed-intro.mp4` is bundled with the app. The intended transition fades in from black, plays silently, and fades out to reveal the launcher.
- **Home-return intro** — the intro is designed to replay when the launcher is opened again from the TV’s Home button or returned to after another app.
- **Remote-friendly controls** — the underlying launcher supports navigating the app grid, Inputs, network status, Settings, and tile reordering where those controls are enabled in the build.
- **Local playback** — wallpapers and the intro video are packaged in the app; no streaming service is required for the theme itself.

> Playback and Home-button behaviour can vary by TV firmware. Test the release on your own device before relying on it as the default Home app.

## Wallpaper collection

The source images are stored in `launcher/app/src/main/assets/prabhas-wallpapers/`:

| Asset | Theme |
|---|---|
| `prabhas-01-bahubali.jpg` | Baahubali-inspired wallpaper |
| `prabhas-02-bahubali.jpg` | Baahubali-inspired wallpaper |
| `prabhas-03-salaar.jpg` | Salaar-inspired wallpaper |
| `prabhas-04-fauzi.jpg` | Fauzi-inspired wallpaper |
| `prabhas-05-fauzi.jpg` | Fauzi-inspired wallpaper |

The startup video is at `launcher/app/src/main/assets/unnamed-intro.mp4`.

## Build on Windows

### Requirements

- Windows 10/11
- JDK 17
- Android SDK / Android Studio command-line components available to Gradle
- Android Debug Bridge (`adb`) if you want to install the APK over Wi-Fi

### Build the APK

Open PowerShell in the repository root and run:

```powershell
cd launcher
.\gradlew.bat assembleRelease
```

The release APK is normally generated at:

```text
launcher\app\build\outputs\apk\release\app-release.apk
```

### Install using ADB

Connect ADB to your Android TV first. For example, on TVs that support network ADB:

```powershell
adb connect TV_IP_ADDRESS:5555
adb devices
```

Replace `TV_IP_ADDRESS` with the IP address shown in your TV’s network settings. If the TV uses Wireless debugging with pairing, pair using the pairing address and code shown on the TV before connecting.

Install or update the UNNAMED build:

```powershell
adb install -r ".\app\build\outputs\apk\release\app-release.apk"
```

Then select **UNNAMED** as the Home app in the TV’s Default apps settings if the TV offers that option. Some manufacturers restrict third-party launchers.

### Wi-Fi name on the Home screen

Android may restrict access to the Wi-Fi SSID. If the network pill only displays “Wi-Fi”, try granting the declared location permission over ADB and enabling Location in the TV’s Android settings:

```powershell
adb shell pm grant com.unnamed.tvlauncher android.permission.ACCESS_FINE_LOCATION
```

The TV firmware may still hide the network name even with permission enabled.

## Move apps with the TV remote

The CleanYourTV base includes a tile-reordering system. In builds where its tile menu is enabled:

1. Focus the app tile.
2. Hold **OK / Select** and choose **Move** (or open the launcher’s **Reorder** control).
3. Use the D-pad to move the tile, then press **OK** to place it.
4. Press **Back** to finish and save the order.

Available options can vary with the launcher configuration and TV firmware.

## Project layout

```text
launcher/                                      Android launcher source
launcher/app/src/main/assets/prabhas-wallpapers/  Bundled Prabhas wallpapers
launcher/app/src/main/assets/unnamed-intro.mp4    Startup intro video
prabhas-wallpapers/                            Wallpaper source/patch assets
apply-prabhas-wallpapers.ps1                   Wallpaper patch helper
```

## Credits and rights

- **Base launcher:** [CleanYourTV by Gathin23](https://github.com/Gathin23/cleanyourtv), version 3.0.0. The upstream source and its documentation are retained in this repository; consult the included `LICENSE` and upstream project for the applicable terms.
- **UNNAMED customizations:** this repository’s launcher branding, Prabhas-themed wallpaper packaging, and intro-video integration.
- **Movie imagery and video:** Prabhas photographs, film-related artwork, logos, and footage belong to their respective copyright holders. This repository does not claim ownership of those assets or imply endorsement. Confirm you have permission to redistribute them before publishing or distributing builds that include them.

UNNAMED — Prabhas Edition is a community/personal customization, not an official product from the actor, film producers, or CleanYourTV maintainers.
