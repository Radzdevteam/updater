
# RadzUpdater

RadzUpdater is a library to check for updates in an Android app. This library simplifies the process of checking for updates and downloading them in the background.

## Setup Instructions

### Step 1: Add the repository to your project

In your `settings.gradle` file, include the following:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add the dependency

In your `build.gradle` file, add the following dependency:

[![](https://jitpack.io/v/Radzdevteam/updater.svg)](https://jitpack.io/#Radzdevteam/updater)
```gradle
dependencies {
     implementation ("com.github.Radzdevteam:updater:Tag")
}
```

### Step 3: Update your `MainActivity`

In your `MainActivity`, add the following code to check for updates:

```kotlin
import com.radzdev.radzupdater.updater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updater(this,
            "https://raw.githubusercontent.com/Radzdevteam/test/refs/heads/main/updatertest")
            .checkForUpdates()
    }
}
```

### Step 4: Update your `AndroidManifest.xml`

You need to add the following permissions and provider configuration to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/provider_paths"/>
</provider>
```

### Step 5: Add the `provider_paths.xml` file

Create a file named `provider_paths.xml` under `res/xml/` directory and add the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="external_files" path="."/>
</paths>
```

## Additional Notes

- Ensure your app has permission to access the internet, write to external storage, and request package installation to allow seamless updates.
- The `updater` function checks for updates from the provided URL and automatically handles the update process.
