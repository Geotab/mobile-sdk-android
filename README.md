[![GitHub license](https://img.shields.io/github/license/Geotab/mobile-sdk-android)](https://github.com/Geotab/mobile-sdk-android/blob/main/LICENSE) [![GitHub docs](https://img.shields.io/badge/docs-passing-brightgreen)](https://geotab.github.io/mobile-sdk-android/geotabdrivesdk/) ![GitHub kotlin](https://img.shields.io/badge/Kotlin-1.4.32-brightgreen) ![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/Geotab/mobile-sdk-android?label=release)
# Mobile SDK Android     

## How to start  

### Adding the Geotab Mobile SDK as a dependency  
Geotab Mobile SDK can be added as gradle dependency, which is hosted in a GitHub repository. Use a valid GitHub user and token to access the library.
Update build.gradle inside the app module with the Geotab Mobile SDK's Github repository path and credentials.

``` Groovy
repositories {
    /**
     * Create local.properties in root project folder file.
     * Add properties gpr.usr=GITHUB_USERID and gpr.key=PERSONAL_ACCESS_TOKEN.
     * Replace GITHUB_USERID with Github User ID and PERSONAL_ACCESS_TOKEN with a personal access token for this user.
     */
    File localPropertiesFile = rootProject.file("local.properties")
    Properties localProperties = new Properties()
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.newDataInputStream())
    }

    maven {
        name = "GitHubPackages"
        url = uri {"https://maven.pkg.github.com/geotab/mobile-sdk-android"}
        credentials {
            username = localProperties["gpr.usr"]
            password = localProperties["gpr.key"]
        }
    }
}
```
check [this](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token#creating-a-token) to create a personal access token, from the user account. Select the `read:packages` option when selecting your token's scope.

Add the Geotab Mobile SDK library under the dependencies, to the application's build gradle.
``` Groovy
dependencies {
    implementation 'com.geotab.mobile.sdk:mobile-sdk-android:$sdk_version'
}
```
Sync project with Gradle files to ensure the dependencies are resolved.

### Initialization

The DriveFragment is the starting point of integrating mobile SDK. It's the container of the Geotab Drive Web app equipped with native APIs for accessing Geotab Drive web app's data
```kotlin
val driveView = DriveFragment.newInstance()  
```

### Present the DriveFragment

```kotlin
supportFragmentManager
    .beginTransaction()
    .add(R.id.main_layout, driveView)
    .commit()
```
### Execute API call

Most API calls require a user to be logged in to get a meaningful result.  You can listen for the `setPageNavigationCallback` to know when the user has successfully logged in.
```kotlin
driveView.setPageNavigationCallback { path ->
    Log.d(tag, "PageNavigationCallback: $path")
    if (path.contains("main")) {
        //User is logged in
    }
}
```
Now you can make API calls like `getUserViolations` to get the logged in user's HOS violations. 
```kotlin
driveView.getUserViolations { result ->
    when (result) {
        is Success -> {
            for (violation in result.value) {
                Log.d(tag, "Successful get violation $violation")
            }
        }
        is Failure -> {
            Log.d(
                tag, "Get violations failed with ${result.reason.getErrorCode()}," +
                    result.reason.getErrorMessage()
            )
        }
    }
}
```  

For a complete list of APIs see [geotabdrivesdk](https://geotab.github.io/mobile-sdk-android/geotabdrivesdk/index.html)

### Set a Callback listener
```kotlin
driveView.setDriverActionNecessaryCallback { (isDriverActionNecessary, driverActionType) ->
    Log.d(tag, "DriverActionNecessaryCallback: $isDriverActionNecessary, $driverActionType ")
}
```

For a complete list of callbacks see [geotabdrivesdk](https://geotab.github.io/mobile-sdk-android/geotabdrivesdk/index.html)
### Custom Login

The Mobile SDK allows integrators to use their own authentication and user management. All the SDK needs to log into Geotab Drive is a user's credentials.

```kotlin
driveView.setSession(credentials, isCoDriver)
```
Where `credentials` is of type `CredentialResult` and `isCoDriver` is a `Boolean`
```kotlin
data class CredentialResult(
    val credentials: GeotabCredentials,
    val path: String
) : Serializable

data class GeotabCredentials(
    val userName: String,
    val database: String,
    val sessionId: String
) : Serializable
``` 

### Overwrite Default Background Service Notification Icon

To use a custom icon for the background service notification, create a metadata object in your AndroidManifest.xml
```kotlin
<meta-data
    android:name="default_notification_icon"
    android:resource="@drawable/your_icon" />
```

### Overwrite Default Background color and icon

To override default background color in network error page, create color resource with name="whitelabel_background_color" in color.xml
```kotlin
<color name="whitelabel_background_color">#FFFFFF</color>
```
To override default text color in network error page, create color resource with name="whitelabel_text_color" in color.xml
```kotlin
<color name="whitelabel_text_color">#FBCE07</color>
```
To override default icon in network error page, add a drawable resource with name="whitelabel_banner_icon"

### Permissions

To use the background location functionality, just add the permission in your AndroidManifest.xml

```kotlin
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

To use the activity motion tracking from the device, just add these two permissions as needed in your AndroidManifest.xml

```kotlin
<!-- Required for Android API 28 and below. -->
<uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />
<!-- Required for Android API 29+. -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
```

## License
GeotabDriveSDK is available under the MIT license. See the LICENSE file for more info. 
