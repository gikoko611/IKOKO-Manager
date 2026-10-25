# I KO KO Manager

An offline, no-root APK / ZIP / DEX modding tool for Android.
100% own code — nothing is copied from any other app. No INTERNET permission,
no storage permission, no root.

> ⚠️ **Status: v0.1 (initial scaffold).** Written in one go and not yet compiled on
> a real toolchain. Build it with Android Studio or directly in Termux, expect
> small API/gradle fixes, and test before any release.

## Features (v0.1)

| Area | What works in v0.1 |
|---|---|
| File browser | SAF folder browsing, navigation, file info — no permissions, no root |
| APK info | package, version, SDKs, permission/component counts |
| ZIP engine | entry listing (APK/ZIP/JAR/XAPK), alignment-preserving rebuild |
| App manager | installed app list, APK extraction (split APKs too) |
| **DEX editor** | dex → smali (baksmali), edit smali in built-in text editor, smali → dex, rebuild APK, re-sign, install |
| Signer | APK signing v1+v2+v3 via apksig, own key stored in Android Keystore |
| Tools | standalone "sign any APK", MD5/SHA-1/SHA-256 hashes |

### How DEX editing works

```
pick APK ─▶ copy into workspace
        ─▶ extract classes.dex
        ─▶ baksmali  ─▶  .smali files (readable code)
        ─▶ edit .smali in the text editor
        ─▶ smali     ─▶  new classes.dex
        ─▶ rebuild APK zip (alignment-preserving)
        ─▶ re-sign with your own key
        ─▶ save / install
```

A modified APK has a different signature than the original app, so the original
must be uninstalled first — that is normal Android behavior, not a bug.

## Branding (icon + logo)

The launcher icon and the top banner logo are **generated from your own images** —
`scripts/setup_images.sh` produces every icon size, so you never resize anything
by hand. Until then, a placeholder vector logo keeps the project buildable.

**Recommended:** commit your two originals into the repo once, as:

| Repo file | Used as |
|---|---|
| `branding/icon.jpeg` | launcher icon → `mipmap-*/ic_launcher.png`, `ic_launcher_round.png`, `ic_launcher_foreground.png` (adaptive icon, all densities) |
| `branding/logo.jpeg` | top banner logo → `drawable-nodpi/ikoko_logo.jpg` |

Ways to get the images into the repo:

- **From Termux:** `bash scripts/upload_branding.sh` — copies them from
  `/storage/emulated/0/Download/download (6).jpeg` / `(5).jpeg` and pushes.
  Needs a one-time GitHub login (`gh auth login`, short code + browser).
- **From the GitHub web UI:** *Add file → Upload files*, drag the images,
  name them `branding/icon.jpeg` and `branding/logo.jpeg`.

`.jpg` / `.png` extensions work too — upload the originals as-is, no resizing.
Alternatively keep the images only on the device (the build finds them in the
Download folder automatically), or pass explicit paths:
`bash scripts/setup_images.sh <icon> <logo>`.

You can also commit ready-made icon resources directly into the
`mipmap-*/` + `drawable-nodpi/` folders (same file names); the build then skips
image generation automatically.

## Build

### On a PC

1. Clone: `git clone https://github.com/gikoko611/IKOKO-Manager.git`
2. Open in Android Studio (or run `gradle wrapper --gradle-version 8.9` once
   to generate the wrapper).
3. Build: `./gradlew assembleDebug`

### In Termux (on-device, no PC needed)

```bash
git clone https://github.com/gikoko611/IKOKO-Manager.git
cd IKOKO-Manager
bash scripts/termux_setup.sh        # one time: JDK 17, SDK 35, aapt2, ImageMagick, gh
bash scripts/upload_branding.sh     # one time: push your icon + logo into branding/
bash scripts/termux_build.sh        # branding + build + copy APK to Download
```

The APK lands in `/storage/emulated/0/Download/I-KO-KO-Manager-debug.apk`.

Termux specifics handled by the scripts: the system `aapt2` via
`android.aapt2FromMavenOverride`, `openjdk-17` as the Gradle JVM, and a
device-friendly 1.5 GB Gradle heap.

Toolchain: Kotlin 2.1, Jetpack Compose (Material 3), AGP 8.7.3, Gradle 8.9,
JDK 17, minSdk 24, targetSdk 35.

## Architecture

```
branding/                    your icon + logo source images (committed)
app/src/main/java/com/gikoko/ikokomanager/
├── MainActivity.kt
├── core/                     (all engine code, plain Kotlin)
│   ├── ZipService.kt         own zip engine: list/extract/rebuild, zipalign-style padding
│   ├── DexEditorService.kt   dex↔smali pipeline + session workspace
│   ├── ApkSigner.kt          apksig v1/v2/v3 + Android Keystore key
│   ├── ApkRepository.kt      installed apps + archive info (no root)
│   ├── FileRepository.kt     SAF listing helpers
│   └── Utils.kt              size formatting, hashes
└── ui/                       (Compose screens)
    ├── MainScreen.kt         top banner logo + tabs: Files / Apps / Dex / Tools
    ├── FileBrowserScreen.kt  SAF browser + APK actions
    ├── AppListScreen.kt      installed apps + extract
    ├── DexEditorScreen.kt    dex list, smali browser, text editor, build & sign
    ├── ToolsScreen.kt        sign tool, hash tool
    └── Components.kt         shared small composables

scripts/
├── upload_branding.sh        push your icon + logo images into branding/
├── setup_images.sh           generate icon + logo resources from your images
├── termux_setup.sh           one-time on-device build environment
└── termux_build.sh           on-device build + APK output to Download
```

## Libraries (all legally clean open source)

| Library | License | Use |
|---|---|---|
| [smali / baksmali / dexlib2](https://github.com/google/smali) `3.0.10` | BSD-3-Clause | dex ↔ smali |
| [apksig](https://mvnrepository.com/artifact/com.android.tools.build/apksig) `8.13.2` | Apache-2.0 | APK signing v1/v2/v3 |
| AndroidX / Jetpack Compose | Apache-2.0 | UI |

AXML / ARSC / DEX are public documented file formats; every parser written for
this app is implemented from the public format documentation, not from any
other app's code.

## Roadmap

- [x] Phase 1 — file browser, app manager, zip engine, dex editor + save + sign
- [ ] Phase 2 — text editor improvements, archive entry add/replace/delete
- [ ] Phase 3 — AndroidManifest (AXML) viewer/editor
- [ ] Phase 4 — resources.arsc editor + translation mode
- [ ] Phase 5 — XAPK / APKM / APKS converter, split APK installer
- [ ] Phase 6 — dex class browser (search classes/methods)

## Legal

- This project contains **no code, resources, fonts or assets from MT Manager**
  or any other proprietary app.
- It deliberately **does not include** license/anti-piracy bypass features for
  other apps.
- Use only on apps you own or have permission to modify.
