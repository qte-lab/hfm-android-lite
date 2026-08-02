# HFM Android Lite

A lightweight Android expense tracker for personal finance management, built as a native XML/Fragment application with Kotlin, Hilt, Room, Retrofit, WorkManager, and MPAndroidChart.

## What this app does

- Track daily expenses with category, amount, date, and notes
- Show budget status and expense statistics
- Visualize spending with chart views
- Sync data with a backend service when network is available
- Import and export spreadsheet data
- Support optional AI-assisted expense recognition through the server-side integration

## Current technical stack

- Kotlin
- Android View system with XML layouts and Fragments
- Navigation components
- ViewBinding
- Hilt for dependency injection
- Room for local persistence
- Retrofit + OkHttp for REST communication
- SQLCipher for encrypted local storage
- WorkManager for background synchronization jobs
- MPAndroidChart for charts
- fastExcel for spreadsheet import/export

## Project structure

- `app/` - Android application module
- `app/src/main/java/` - Kotlin source code
- `app/src/main/res/` - XML layouts, drawables, strings, navigation graph, and styles
- `app/schemas/` - Room schema outputs
- `gradle/` - Gradle wrapper files

## Prerequisites

- Android Studio with Android SDK 37 installed
- JDK 17
- A working Android emulator or connected device
- A project-level `local.properties` file if your environment needs Android SDK path overrides

## Build and run

From the repository root:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
```

For a quick debug run in Android Studio:

1. Open the project root in Android Studio
2. Let Gradle sync finish
3. Choose a device or emulator
4. Run the `app` module

## Release signing

The app uses a release signing config in `app/build.gradle`. For local release builds, provide the following values in `local.properties`:

```properties
RELEASE_STORE_FILE=your_keystore.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=your_alias
RELEASE_KEY_PASSWORD=your_key_password
```

If these values are absent, the build falls back to the `release.keystore` file in the project root.

## Configuration notes

### Network base URL

The API base URL is set in the Retrofit network module. Update that value to point to your backend service.

### Optional Firebase / Google services

A `google-services.json` file is optional. If it is present, the Google services plugin is applied automatically. If it is missing, the app still builds without push notification support.

## Main features

### Expense management

- Add, edit, delete, and browse expenses
- Filter expenses by date and other criteria
- View total/average/median statistics

### Budget support

- Enable and configure monthly budget limits
- Monitor warning and over-limit states

### Charts

- Review spending trends and weekday-style chart insights

### Data sync

- Schedule and run background synchronization
- Keep local records and remote data aligned

### Import / export

- Import expenses from spreadsheet files
- Export expense records to Excel-compatible output

## Troubleshooting

### Gradle sync or build issues

- Make sure Java is installed and `JAVA_HOME` is configured
- Invalidate Android Studio caches if the sync becomes inconsistent
- Check that the Android SDK path is available to Gradle

### Runtime issues

- Watch Logcat for Room, Retrofit, or Hilt initialization errors
- Clear app data if a stale DB state is suspected
- Confirm the backend service is reachable if sync or AI features fail

## License

This project is intended for the Home Money application codebase and should be used according to the repository owner’s licensing terms.
