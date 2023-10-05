import java.util.Properties

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka") version "1.8.10"
    id("maven-publish")
}

apply {
    from("./../kotlin-lint.gradle.kts")
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        val versionName = "6.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        exclude("META-INF/AL2.0")
        exclude("META-INF/LGPL2.1")
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
    implementation("androidx.constraintlayout:constraintlayout:2.0.3")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2-native-mt")
    implementation("com.github.spullara.mustache.java:compiler:0.8.18")
    implementation("androidx.fragment:fragment-ktx:1.4.0-alpha07")
    debugImplementation("androidx.fragment:fragment-testing:1.4.0-alpha07")
    implementation("com.google.android.gms:play-services-location:17.1.0")
    implementation("com.google.code.gson:gson:2.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.0")
    testImplementation("io.mockk:mockk:1.13.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2-native-mt")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
    androidTestImplementation("androidx.test:core:1.0.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.4.0")
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
            version = android.defaultConfig.versionName + "_" + android.defaultConfig.versionCode

            artifact("$buildDir/outputs/aar/geotabdrivesdk-release.aar")

            pom.withXml {
                val dependencies = asNode().appendNode("dependencies")
                configurations.getByName("releaseCompileClasspath").allDependencies
                    .filterIsInstance<ExternalDependency>()
                    .forEach { dependency ->
                        dependencies.appendNode("dependency").apply {
                            appendNode("groupId", dependency.group)
                            appendNode("artifactId", dependency.name)
                            appendNode("version", dependency.version)
                        }
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
    tasks.named("javaPreCompileDebugUnitTest") {
        dependsOn("copyTestFiles")
    }
}

tasks {
    withType<AbstractPublishToMaven> {
        dependsOn("assemble")
    }
}