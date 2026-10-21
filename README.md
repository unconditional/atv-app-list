# ATV App List

An Android TV (Google TV / Android TV) side app that lists **every launchable app
installed on the device — including sideloaded APKs** that do not appear on the
stock home screen — with sorting, launching, and per-app actions.

Built with pure Kotlin and **zero third-party dependencies** (no AndroidX/Compose),
so the release APK is tiny and works on Android 5.0+.

## Install

Direct link to the newest CI build:

```
https://github.com/unconditional/atv-app-list/releases/latest/download/atv-app-list.apk
```

1. Download `atv-app-list.apk` on the TV (or `adb install`).
2. It appears in the Google TV home **Apps row** (and in "All apps").
   If it does not show up after the first install, reboot the TV once.

## Features

- Grid of all launchable apps (TV-style and phone-style packages)
- Sort: `A-Z`, `Last used` (self-tracked, optionally merged with system usage
  statistics when you grant *Usage access*), `Newest` (recently installed)
- `OK` launches; **long-press OK** (or the `Actions` chip while a tile is
  highlighted) opens: Launch / Force stop / App info / Uninstall / Hide
- Settings: show system apps, manage hidden apps, request Usage access, About
- No INTERNET permission; nothing leaves the device

## Notes

- **Force stop** opens the system *App info* screen (real force-stopping of other
  apps is system-only; no root required).
- Package enumeration uses package visibility queries (`<queries>`), so
  Android 11+ filtering does not hide apps.

## Building

CI (GitHub Actions) builds `:app:assembleRelease` on every push to `main` and
publishes a rolling `latest` prerelease; pushing a tag `v1.2.3` publishes a
versioned release.

Toolchain: AGP 9.1.1 / Gradle 9.3.1 / Kotlin 2.4.20 / JDK 17 / compileSdk 35.

- `tools/gen/Gen.java` regenerates launcher icons + banners (plain Java 2D,
  run from the repo root: `java -cp tools/gen Gen`).
- `app/keystore.jks` + `keystore.properties` hold the release signing key
  (committed intentionally so CI produces a consistent signature).

## Project layout

```
app/src/main/java/...   Kotlin sources (activities, grid/tile views, repository)
app/src/main/res/       layouts, drawables, launcher icons (mipmap), banner
.github/workflows/      CI: build + artifact + releases
tools/gen/              optional asset generator
```
