# Getting Started 

## License

tbd  

## Adding the Geotab Mobile SDK as a dependency  

Import the module File->New->New Module  
- select "Import .JAR/.AAR Package"

Add the following dependencies to your app build.gradle file
```Groovy
implementation project(path: ':geotabdrivesdk-release')
implementation 'androidx.fragment:fragment-ktx:1.3.0-alpha06'
implementation 'com.github.spullara.mustache.java:compiler:0.8.18'
implementation 'androidx.exifinterface:exifinterface:1.3.1'
implementation 'com.google.code.gson:gson:2.8.6'
```
## Initialization

The DriveFragment is the starting point of integrating mobile SDK. It's the container of the Geotab Drive Web app equipped with native APIs for accessing Geotab Drive web app's data
```kotlin
val driveView = DriveFragment.newInstance()  
```

## Present the DriveFragment

```kotlin
supportFragmentManager
    .beginTransaction()
    .add(R.id.main_layout, driveView)
    .commit()
```
## Execute API call

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

For a complete list of APIs see [public-apis.md](./public-apis.md)

## Set a Callback listener
```kotlin
driveView.setDriverActionNecessaryCallback { (isDriverActionNecessary, driverActionType) ->
    Log.d(tag, "DriverActionNecessaryCallback: $isDriverActionNecessary, $driverActionType ")
}
```

For a complete list of callbacks see [public-apis.md](./public-apis.md)
## Custom Login

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

## Overwrite Default Background Service Notification Icon

To use a custom icon for the background service notification, create a metadata object in your AndroidManifest.xml
```kotlin
<meta-data
    android:name="default_notification_icon"
    android:resource="@drawable/your_icon" />
```

## Overwrite Default Background color and icon

To override default background color in network error page, create color resource with name="whitelabel_background_color" in color.xml
```kotlin
<color name="whitelabel_background_color">#FFFFFF</color>
```
To override default text color in network error page, create color resource with name="whitelabel_text_color" in color.xml
```kotlin
<color name="whitelabel_text_color">#FBCE07</color>
```
To override default icon in network error page, add a drawable resource with name="whitelabel_banner_icon"

## Permissions

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