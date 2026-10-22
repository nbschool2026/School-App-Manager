# School App Manager

Kotlin, Android 9+ (API 28), ARM64, direct distribution to school tablets.

## Implemented

- HTTPS JSON catalog; compare installed packageName/longVersionCode.
- Skip equal/newer versions; offer downloads only for missing/older apps.
- Download progress and cancellation; 512 MiB limit, 5 minute deadline, 15 second socket timeouts.
- Require final HTTP 200; follow at most five HTTPS APK redirects, rejecting HTTP downgrades and credentials in URLs. Catalog JSON still requires direct HTTPS with no redirects.
- Verify SHA-256 and cryptographic APK signatures using Google apksig on the tablet's API level.
- Match the catalog's signer certificate, packageName and versionCode; check minSdk and native ABI compatibility.
- Require matching installed signer for updates. Key rotation and multiple signers fail closed in this version.
- Recheck the APK before installation. Share only this private APK through a read-only provider with temporary URI permission.
- Open Android's installer for user confirmation. Unknown-source permission is requested through Android Settings after an explanation.
- Check the installed version when the installer returns; report cancellation/failure and allow rescanning.

## On-tablet update test

1. Push the commits to GitHub.
2. Open the installed manager version 0.2.2. If needed, manually install `app/apk/School-App-Manager-0.2.2-debug.apk` first.
3. Open the manager and check the default online catalog.
4. The catalog offers version 0.2.3 (versionCode 6): expect one update.
5. Download; wait for the SHA-256/signature checks; tap Install.
6. If needed, allow installations from School App Manager in Android Settings, return and tap Install again.
7. Confirm in the system installer. A self-update can close the manager; reopen it.
8. Rescan: expect 12 catalog apps; the manager is current and its header is version 0.2.3. Other missing/older apps can still require installation.

Device installation and network behavior still require this manual test. Desktop tests cover version comparison, a real APK's SHA-256/signature, wrong hashes, wrong signer and tampered APK rejection.

## Build

JDK 17, Gradle 8.9, Android SDK Platform 35 and Build Tools 34.0.0. Use `gradle wrapper --gradle-version 8.9` if no wrapper is present, then `gradlew.bat testDebugUnitTest assembleDebug lintDebug`. Output: `app/build/outputs/apk/debug/app-debug.apk`.

Use the same debug keystore as earlier builds for test updates. Never commit keystores. Final distribution needs a permanent release signing key. The binary APKs in `app/apk` are test builds.

## Catalog

Root: `schemaVersion: 1`, `apps: [...]`. Each entry requires `name`, `packageName`, numeric positive `versionCode`, `versionName`, direct HTTPS `apkUrl`, lowercase 64-character `sha256` and `signerSha256` (SHA-256 of DER signing certificate). Catalog updates must be reviewed by its owner; do not substitute guessed APK metadata.

The catalog includes 12 entries: School App Manager 0.2.4, Windows Learning Desktop, Google Docs, Sheets, Slides, Meet, Viber, YouTube, Chrome, BlockDrop, Chess and F-Droid. The six third-party APKs are published in the school-apks GitHub Release with hashes verified against the uploaded assets. The user confirmed Windows Learning Desktop installation through the manager. The recovered complete school app list is in `catalog/APP-LIST.md`; per-app import progress and remaining blockers are in `catalog/IMPORT-STATUS.md`. Apps whose APKs or permanent download URLs are not verified are not included in the live catalog.

Only single APK files are supported, not XAPK/APKM or split APK sets. No silent installation, automatic uninstall or downgrade. Downloads are tied to the open activity; process termination cancels the operation and requires retrying.

`QUERY_ALL_PACKAGES` supports inventory from a dynamic catalog on Android 11+; this is a dedicated school manager distributed directly, not a Play Store submission.

Version 0.2.4 (7) adds an orange graduation-cap vector launcher icon with adaptive Android icon layers. Editable SVG: artwork/school-app-manager.svg. APK: app/apk/School-App-Manager-0.2.4-debug.apk. Build and API 28 signature verification passed; signer matches 0.2.3.
