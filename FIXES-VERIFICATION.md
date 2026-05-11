# Fixes Verification

Command: ./gradlew clean assembleDebug && ./gradlew lint && ./gradlew test

```

> Configure project :app
WARNING: BuildType 'debug' is both debuggable and has 'isMinifyEnabled' set to true.
All code optimizations and obfuscation are disabled for debuggable builds.

> Task :app:clean
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:generateDebugBuildConfig
> Task :app:checkDebugAarMetadata
> Task :app:processDebugNavigationResources
> Task :app:compileDebugNavigationResources
> Task :app:generateDebugResValues
> Task :app:mapDebugSourceSetPaths
> Task :app:generateDebugResources
> Task :app:packageDebugResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug
> Task :app:parseDebugLocalResources
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:javaPreCompileDebug
> Task :app:mergeDebugShaders
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:compressDebugAssets
> Task :app:extractProguardFiles
> Task :app:mergeDebugJniLibFolders
> Task :app:mergeDebugNativeLibs
> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :app:writeDebugSigningConfigVersions
> Task :app:processDebugManifestForPackage
> Task :app:checkDebugDuplicateClasses
> Task :app:mergeDebugResources
> Task :app:processDebugResources

> Task :app:kaptGenerateStubsDebugKotlin
w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.

> Task :app:kaptDebugKotlin

> Task :app:compileDebugKotlin
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/ComposeQuizActivity.kt:124:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/MainActivity.kt:584:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/ResultActivity.kt:86:13 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/SelectQuizActivity.kt:43:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/WelcomeActivity.kt:53:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/WelcomeActivity.kt:59:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/ui/theme/ZnamTheme.kt:152:20 'var statusBarColor: Int' is deprecated. Deprecated in Java.

> Task :app:compileDebugJavaWithJavac
> Task :app:mergeDebugGeneratedProguardFiles UP-TO-DATE
> Task :app:processDebugJavaRes
> Task :app:mergeDebugJavaResource
> Task :app:minifyDebugWithR8
> Task :app:convertShrunkResourcesToBinaryDebug
> Task :app:packageDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:assembleDebug

BUILD SUCCESSFUL in 37s
40 actionable tasks: 39 executed, 1 up-to-date

> Configure project :app
WARNING: BuildType 'debug' is both debuggable and has 'isMinifyEnabled' set to true.
All code optimizations and obfuscation are disabled for debuggable builds.

> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:generateDebugBuildConfig UP-TO-DATE
> Task :app:checkDebugAarMetadata UP-TO-DATE
> Task :app:processDebugNavigationResources UP-TO-DATE
> Task :app:compileDebugNavigationResources UP-TO-DATE
> Task :app:generateDebugResValues UP-TO-DATE
> Task :app:mapDebugSourceSetPaths UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:packageDebugResources UP-TO-DATE
> Task :app:parseDebugLocalResources UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:extractDeepLinksDebug UP-TO-DATE
> Task :app:processDebugMainManifest UP-TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:kaptGenerateStubsDebugKotlin UP-TO-DATE
> Task :app:kaptDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:compileDebugJavaWithJavac UP-TO-DATE
> Task :app:preDebugAndroidTestBuild SKIPPED
> Task :app:generateDebugAndroidTestResValues
> Task :app:extractProguardFiles UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar
> Task :app:generateDebugLintReportModel
> Task :app:generateDebugAndroidTestLintModel
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:generateDebugUnitTestLintModel
> Task :app:lintAnalyzeDebugAndroidTest
> Task :app:lintAnalyzeDebugUnitTest
> Task :app:lintAnalyzeDebug

> Task :app:lintReportDebug
Wrote HTML report to file:///home/alpharius/projects/GettingBiology/app/build/reports/lint-results-debug.html

> Task :app:lintDebug
> Task :app:lint

BUILD SUCCESSFUL in 12s
32 actionable tasks: 10 executed, 22 up-to-date

> Configure project :app
WARNING: BuildType 'debug' is both debuggable and has 'isMinifyEnabled' set to true.
All code optimizations and obfuscation are disabled for debuggable builds.

> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:generateDebugBuildConfig UP-TO-DATE
> Task :app:checkDebugAarMetadata UP-TO-DATE
> Task :app:processDebugNavigationResources UP-TO-DATE
> Task :app:compileDebugNavigationResources UP-TO-DATE
> Task :app:generateDebugResValues UP-TO-DATE
> Task :app:mapDebugSourceSetPaths UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:packageDebugResources UP-TO-DATE
> Task :app:parseDebugLocalResources UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:extractDeepLinksDebug UP-TO-DATE
> Task :app:processDebugMainManifest UP-TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:kaptGenerateStubsDebugKotlin UP-TO-DATE
> Task :app:kaptDebugKotlin UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:compileDebugJavaWithJavac UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar UP-TO-DATE
> Task :app:bundleDebugClassesToRuntimeJar
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:mergeDebugShaders UP-TO-DATE
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest
> Task :app:mergeDebugAssets UP-TO-DATE
> Task :app:processDebugUnitTestManifest
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:buildKotlinToolingMetadata
> Task :app:preReleaseBuild UP-TO-DATE
> Task :app:generateReleaseBuildConfig
> Task :app:checkReleaseAarMetadata
> Task :app:processReleaseNavigationResources
> Task :app:compileReleaseNavigationResources
> Task :app:generateReleaseResValues
> Task :app:mapReleaseSourceSetPaths
> Task :app:generateReleaseResources

> Task :app:kaptGenerateStubsDebugUnitTestKotlin
w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.

> Task :app:packageReleaseResources
> Task :app:createReleaseCompatibleScreenManifests
> Task :app:extractDeepLinksRelease
> Task :app:parseReleaseLocalResources
> Task :app:processReleaseMainManifest
> Task :app:processReleaseManifest
> Task :app:processReleaseManifestForPackage
> Task :app:preReleaseUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileRelease
> Task :app:javaPreCompileReleaseUnitTest
> Task :app:mergeReleaseShaders
> Task :app:compileReleaseShaders NO-SOURCE
> Task :app:generateReleaseAssets UP-TO-DATE
> Task :app:mergeReleaseAssets
> Task :app:processReleaseUnitTestManifest
> Task :app:convertLinkedResourcesToBinaryDebug
> Task :app:packageDebugUnitTestForUnitTest
> Task :app:generateDebugUnitTestConfig

> Task :app:kaptDebugUnitTestKotlin
warning: The following options were not recognized by any processor: '[room.schemaLocation, kapt.kotlin.generated]'

> Task :app:compileDebugUnitTestKotlin
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes
> Task :app:mergeReleaseResources
> Task :app:testDebugUnitTest
> Task :app:processReleaseResources
> Task :app:convertLinkedResourcesToBinaryRelease
> Task :app:packageReleaseUnitTestForUnitTest
> Task :app:generateReleaseUnitTestConfig

> Task :app:kaptGenerateStubsReleaseKotlin
w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.

> Task :app:kaptReleaseKotlin

> Task :app:compileReleaseKotlin
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/ComposeQuizActivity.kt:124:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/MainActivity.kt:584:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/ResultActivity.kt:86:13 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/SelectQuizActivity.kt:43:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/WelcomeActivity.kt:53:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/WelcomeActivity.kt:59:9 'fun overridePendingTransition(p0: Int, p1: Int): Unit' is deprecated. Deprecated in Java.
w: file:///home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/ui/theme/ZnamTheme.kt:152:20 'var statusBarColor: Int' is deprecated. Deprecated in Java.

> Task :app:compileReleaseJavaWithJavac
> Task :app:processReleaseJavaRes
> Task :app:bundleReleaseClassesToRuntimeJar
> Task :app:bundleReleaseClassesToCompileJar

> Task :app:kaptGenerateStubsReleaseUnitTestKotlin
w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.

> Task :app:kaptReleaseUnitTestKotlin
warning: The following options were not recognized by any processor: '[room.schemaLocation, kapt.kotlin.generated]'

> Task :app:compileReleaseUnitTestKotlin
> Task :app:compileReleaseUnitTestJavaWithJavac NO-SOURCE
> Task :app:processReleaseUnitTestJavaRes
> Task :app:testReleaseUnitTest
> Task :app:test

BUILD SUCCESSFUL in 25s
73 actionable tasks: 48 executed, 25 up-to-date
```

Exit status: 0

Test result summary:
- testDebugUnitTest: 17 passed, 0 failed, 0 errors, 0 skipped
- testReleaseUnitTest: 17 passed, 0 failed, 0 errors, 0 skipped
