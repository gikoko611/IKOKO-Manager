# I KO KO Manager

An offline, no-root APK / ZIP / DEX modding tool for Android.
100% own code — nothing is copied from any other app. No INTERNET permission,
no storage permission, no root.

> ⚠️ **Status: v0.1 (initial scaffold).** Written in one go and not yet compiled on
> a real toolchain. Build it with Android Studio, expect small API/gradle fixes,
> and test before any release.

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

## Build

1. Clone: `git clone https://github.com/gikoko611/IKOKO-Manager.git`
2. Open in Android Studio (or run `gradle wrapper --gradle-version 8.9` once
   to generate the wrapper).
3. Build: `./gradlew assembleDebug`

Toolchain: Kotlin 2.1, Jetpack Compose (Material 3), AGP 8.7.3, Gradle 8.9,
JDK 17, minSdk 24, targetSdk 35.

## Architecture

```
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
    ├── MainScreen.kt         tabs: Files / Apps / Dex / Tools
    ├── FileBrowserScreen.kt  SAF browser + APK actions
    ├── AppListScreen.kt      installed apps + extract
    ├── DexEditorScreen.kt    dex list, smali browser, text editor, build & sign
    ├── ToolsScreen.kt        sign tool, hash tool
    └── Components.kt         shared small composables
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
