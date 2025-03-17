import java.util.Properties

val versionName = "6.7.5_73738"

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka") version "1.8.10"
    id("maven-publish")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

apply {
    from("./../kotlin-lint.gradle.kts")
}

android {
    compileSdk = 34
    namespace = "com.geotab.mobile.sdk"

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField("String", "KEYSTORE_ALIAS", "\"" +  System.getenv("KEYSTORE_ALIAS") + "\"")
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
    }

    buildTypes {
        getByName("debug") {
            val localProperties : File = project.file("local.properties")
            val properties = Properties()
            if (localProperties.exists()) {
                localProperties.inputStream().use { input ->
                    properties.load(input) }
            }

            buildConfigField("String", "KEYSTORE_ALIAS", "\"" + properties.getProperty("keystore.alias", System.getenv("KEYSTORE_ALIAS")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_USER1", "\"" + properties.getProperty("integration.test.user1", System.getenv("INTEGRATION_TEST_USER1")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_USER1_PWD", "\"" + properties.getProperty("integration.test.user1.pwd", System.getenv("INTEGRATION_TEST_USER1_PWD")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_USER1_ID", "\"" + properties.getProperty("integration.test.user1.id", System.getenv("INTEGRATION_TEST_USER1_ID")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_USER2", "\"" + properties.getProperty("integration.test.user2", System.getenv("INTEGRATION_TEST_USER2")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_USER2_PWD", "\"" + properties.getProperty("integration.test.user2.pwd", System.getenv("INTEGRATION_TEST_USER2_PWD")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_DEVICE_ID", "\"" + properties.getProperty("integration.test.device.id", System.getenv("INTEGRATION_TEST_DEVICE_ID")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_DATABASE", "\"" + properties.getProperty("integration.test.database", System.getenv("INTEGRATION_TEST_DATABASE")) + "\"")
            buildConfigField("String", "INTEGRATION_TEST_URL", "\"" + properties.getProperty("integration.test.url", System.getenv("INTEGRATION_TEST_URL")) + "\"")

        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    tasks {
        dokkaHtml {
            outputDirectory.set(file("$buildDir/docs"))

            dokkaSourceSets {
                configureEach {
                    noJdkLink.set(true)
                    noStdlibLink.set(true)
                    noAndroidSdkLink.set(true)
                    includeNonPublic.set(false)

                    perPackageOption {
                        matchingRegex.set("com.geotab.mobile.sdk.publicInterfaces.*")
                        moduleName.set("output")
                        suppress.set(false)
                    }

                    perPackageOption {
                        matchingRegex.set("com.geotab.mobile.sdk.models.publicModels.*")
                        moduleName.set("output")
                        suppress.set(false)
                    }

                    perPackageOption {
                        matchingRegex.set("com.geotab.mobile.sdk.*")
                        suppress.set(true)
                    }
                }
            }
        }
    }

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        abortOnError = true
        ignore += listOf("GradleDependency", "ObsoleteLintCustomCheck")
        lintConfig = file("lint.xml")
        warningsAsErrors = true
    }
}

dependencies {
    dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:1.8.10")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.3")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2-native-mt")
    implementation("com.github.spullara.mustache.java:compiler:0.8.18")
    implementation("androidx.fragment:fragment-ktx:1.4.0-alpha07")
    implementation("androidx.lifecycle:lifecycle-common:2.5.1")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    debugImplementation("androidx.fragment:fragment-testing:1.6.0-alpha04")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation ("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    annotationProcessor("androidx.room:room-compiler:2.5.2")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.20")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2-native-mt")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.4.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
}
repositories {
    mavenCentral()
    google()
}

publishing {
    repositories {
        val localProperties : File = project.file("local.properties")
        val properties = Properties()
        if (localProperties.exists()) {
            localProperties.inputStream().use { input ->
                properties.load(input) }
        }

        maven {
            name = "GitHubPackages"
            url = uri {"https://maven.pkg.github.com/geotab/mobile-sdk-android"}
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GPR_USER")
                password = properties.getProperty("gpr.key") ?: System.getenv("GPR_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("aar") {
            groupId = "com.geotab.mobile.sdk"
            artifactId = "mobile-sdk-android"
            version = versionName

            artifact("$buildDir/outputs/aar/geotabdrivesdk-release.aar")

            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")

                configurations.getByName("releaseCompileClasspath").allDependencies
                    .filterIsInstance<ExternalDependency>()
                    .forEach { dependency ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dependency.group)
                        dependencyNode.appendNode("artifactId", dependency.name)
                        dependencyNode.appendNode("version", dependency.version)
                    }
            }
        }
    }
}

tasks.register<Copy>("copyTestFiles") {
    from("src/main/assets")
    into("src/test/resources")
}

afterEvaluate {
    tasks.named("processDebugUnitTestJavaRes") {
        dependsOn("copyTestFiles")
    }
    tasks.named("processReleaseUnitTestJavaRes") {
        dependsOn("copyTestFiles")
    }
}

tasks {
    withType<AbstractPublishToMaven> {
        dependsOn("assemble")
    }
}
