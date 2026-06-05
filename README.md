# AK Spareparts — Android App

Offline auto-parts management app. Kotlin + Jetpack Compose + Room (local SQLite).
No backend. Material 3, deep-blue (#1565C0) theme.

## Build the APK on GitHub (no install needed) — recommended

This repo includes a GitHub Actions workflow that compiles the APK in the cloud.

1. Create a free account at https://github.com and click **New repository**
   (give it any name, e.g. `ak-spareparts`). You can leave it Private.
2. On the new repo page, choose **uploading an existing file**, then drag in
   **everything inside this folder** (keep the folder structure — make sure the
   hidden `.github` folder and `gradlew` files are included).
3. Click **Commit changes**. The build starts automatically.
4. Open the **Actions** tab → click the latest run named "Build APK".
5. When it finishes (≈3–5 min, green check), scroll to **Artifacts** and download
   **AK-Spareparts-debug-apk** — that zip contains `app-debug.apk`.
6. Copy the APK to your phone and install it (allow "install from unknown sources").

Tip: if uploading by drag-and-drop drops the `.github` folder, install
GitHub Desktop (or `git`) and push the whole folder instead — that preserves it.

## Build the APK in Android Studio (alternative)

1. Open this folder in **Android Studio** (Hedgehog or newer) and let Gradle sync.
2. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   The APK appears at `app/build/outputs/apk/debug/app-debug.apk`.
3. Or from a terminal: `./gradlew assembleDebug`

- Target SDK 34, Min SDK 26. Requires Android SDK + JDK 17 (bundled with Android Studio).

## Login accounts (hardcoded, seeded on first launch)

| Username | Password | Name   |
|----------|----------|--------|
| immy     | 117      | Imran  |
| bagni    | 118      | Sulman |

Session is saved with SharedPreferences — you stay logged in until you tap **Logout**.

## Anthropic API key (photo extraction)

After the first login a one-time dialog asks for your Anthropic API key.
It is stored locally in SharedPreferences and reused for all image extractions.
The "Add Parts" tab can read a photo of a parts list/invoice via
`claude-sonnet-4-20250514` vision and returns parts for review before saving.

## Features

- **Customers**: list (name + city), add new, open detail.
- **Customer detail** (5 tabs): Already Sold Parts · Add Parts (manual + photo
  extraction + autosuggest) · Edit Parts (searchable, per-row save) ·
  Generate Bill (checkbox + qty + live totals, PDF preview, share, explicit Save) ·
  All Bills (history, tap to re-share PDF).
- **All Parts List**: global catalog, searchable.
- **New Part**: add to global catalog (auto-suggests when adding to customers).
- Bills export as PDF (native `PdfDocument`) and share via WhatsApp/email/etc.
- Global catalog pre-seeded with 28 parts on first launch.

## Project layout

```
app/src/main/java/com/akspareparts/app/
├── AKApplication.kt          service locator (Repository + SessionManager)
├── MainActivity.kt           nav drawer + NavHost
├── data/                     Room entities, DAOs, AppDatabase (+ seed), Repository, models
├── prefs/SessionManager.kt   SharedPreferences (session + API key)
├── network/ClaudeVisionApi.kt  Anthropic vision call
├── pdf/BillPdfGenerator.kt   styled PDF bill
└── ui/                       theme, screens, viewmodels, components
```
