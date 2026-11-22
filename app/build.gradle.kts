plugins { id("com.android.application") }

android {
    namespace = "vn.studentfinance"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.studentfinance"
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        targetSdk = 36
        vectorDrawables.useSupportLibrary = true
        javaCompileOptions.annotationProcessorOptions.arguments["room.schemaLocation"] = "$projectDir/schemas"
        versionCode = 2
        versionName = "0.2"
    }
    buildTypes {
        create("demo") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".demo"
            matchingFallbacks += "debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
android.testOptions.unitTests.isIncludeAndroidResources = true

// Robolectric 4.14 runs the JVM tests on JDK 21 independently of the Gradle daemon.
val testJdk = extensions.getByType<JavaToolchainService>().launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.withType<Test>().configureEach { javaLauncher.set(testJdk) }

androidComponents.beforeVariants(androidComponents.selector().withBuildType("demo")) { variant ->
    variant.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
}

dependencies {
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("junit:junit:4.13.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
    implementation("androidx.room:room-runtime:2.6.1")
}
