# Android Keyless SDK


## Requirements

The Keyless SDK for Android devices requires the latest version of [Android Studio](https://developer.android.com/studio) and the application module’s minSdkVersion must be equal to or greater than the version defined by the library (v21).

## Add Keyless SDK to Android Project
The Keyless SDK is available as a gradle dependency hosted on GitHub. To add the Keyless SDK to your Android project, add the following lines to the application’s build.gradle (../app/build.gradle) file then sync the project to ensure that the dependencies are resolved.

```gradle
dependencies {
  // …
  // Add the Keyless SDK
  implementation "com.geotab:keyless:1.1.0"
}
android {
  // …
  // Add the Keyless SDK repository
  repositories {
    def properties = new Properties()
    File githubPropertiesFile = rootProject.file(".github.properties")
    if (githubPropertiesFile.exists()) {
      properties.load(githubPropertiesFile.newDataInputStream())
    }
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/Geotab/keyless-sdk-android")
      credentials {
        username = properties["GITHUB_USERNAME"] ?: System.getenv("GITHUB_USERNAME")
        password = properties["GITHUB_PERSONAL_ACCESS_TOKEN"] ?: System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")
      }
    }
  }
}
```
Follow these [steps](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line) to create your personal access token. The personal access token can be stored in a properties file at the root of the project and ignored by version control as a security precaution.

## SDK Integration

The Keyless SDK exposes the execution of various commands through the `KeylessManager` class. An instance of the `KeylessManager` can be created in Kotlin and Java.

### Kotlin

```Kotlin
  private val keylessManager: KeylessManager by lazy {
      KeylessManager (applicationContext, AndroidLogger())
  }
```

### Java

```Java
  private KeylessManager keylessManager;
  @Override
  protected void onCreate (Bundle savedInstanceState) {
    super.onCreate (savedInstanceState);
    setContentView (R.layout.activity_main);
    keylessManager = new KeylessManager (getApplicationContext(), new AndroidLogger());
  }
```

### KeylessManager Class

The **KeylessManager** class takes an Android context and optional Logger as parameters. By default,
the library logs are disabled. You can pass the **KeylessManager** an implementation of the Logger
interface to change the library’s log verbosity or tracing.
The **KeylessManager** class provides a set of functions that can be used to interact with a
Keyless device via Bluetooth. **KeylessManager** automatically handles opening and closing connections to Keyless devices. To perform a complete operation only the `execute` method needs to be used.

```kotlin
execute(operations: Set<Operation>, keylessToken: String, callback: ((error: Error?) -> Unit)? = null)
```

Operations can be executed with the set of Operations passed in, a valid keylessToken and a callback. The results of the operation are returned to a callback function if one is supplied.

The first time  `execute` is called with the given token a connection to the Keyless device is automatically opened by **KeylessManager**. It is reused for further functions using the same token. If the connection is left idle for a period of time it is automatically closed.

**Operation Enum**

* CHECK_IN
* CHECK_OUT
* LOCK
* UNLOCK_ALL
* LOCATE
* IGNITION_ENABLE
* IGNITION_INHIBIT
* OPEN_TRUNK
* CLOSE_TRUNK

More detail on [**KeylessManager**](keyless/src/main/java/com/geotab/keyless/KeylessManager.md)

### Kotlin

```kotlin
import com.geotab.keyless.AndroidLogger
import com.geotab.keyless.KeylessClient
import com.geotab.keyless.KeylessClientDelegate
import com.geotab.keyless.Operation
import com.geotab.keyless.Errorimport jdk.dynalink.Operation

class MainActivity : AppCompatActivity() {
  private val keylessManager: KeylessManager by lazy { KeylessManager(this, AndroidLogger()) }

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)

      val keylessToken = "your token here" // pass in a token from the Keyless service

      lock.setOnClickListener {
        keylessManager.execute(setOf(Operation.IGNITION_INHIBIT, Operation.LOCK), keylessToken) { error ->
            if (error == null) {
                println("Car is locked")
            } else {
                println("Error locking car: $error")
            }
        }
      }
  }
}
```

#### isConnected

Query KeylessManager connection state.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License
[MIT](https://choosealicense.com/licenses/mit/)
