## 1. Executive Summary (max 200 words)
Overall score: 6.1 / 10.

Top 5 findings:
1. F-001 HIGH — `./gradlew lint` fails with 2 errors, so the static quality gate is not releasable.
2. F-002 HIGH — `./gradlew test` fails 2/17 unit tests with `KoinApplicationAlreadyStartedException`, so regression confidence is broken.
3. F-003 MEDIUM — `QuizViewModel` bypasses the Koin `DatabaseProvider`, duplicates DB resolution, uses `db!!`, and falls back to destructive migration.
4. F-004 MEDIUM — database/load/statistics errors collapse into generic UI states or are swallowed, preventing actionable failure recovery.
5. F-005 MEDIUM — in-app locale switching is implemented, but lint reports missing Play Core/no locale split configuration for app bundles.

Do this first: fix lint errors and the failing Koin test lifecycle, then rerun `./gradlew clean assembleDebug lint test` before treating the branch as release-candidate.

Scope note: runtime emulator/device testing was not performed; every runtime claim below is based on source, Gradle, lint, unit test output, APK ZIP inspection, and SQLite queries.

## 2. Score Table
| Dimension | Weight | Score (0-10) | Weighted Contribution | One-line rationale |
|---|---:|---:|---:|---|
| D1. Architecture & Code Quality | 10 | 6 | 0.60 | MVVM/Compose/Koin exist, but legacy activity code and duplicated DB construction remain (F-003, F-006). |
| D2. UI/UX Flow & Usability | 10 | 7 | 0.70 | Launch → selection → quiz → results is clear, but hardcoded result total and mixed XML/Compose paths create friction (F-007, F-006). |
| D3. Jetpack Compose Implementation | 8 | 5 | 0.40 | Compose screens are functional but have no `@Preview` coverage and hardcoded colors/dimensions (F-006). |
| D4. Data Layer (Room + SQLite) | 10 | 7 | 0.70 | DB integrity and hint coverage are strong; duplicate questions and inconsistent migration path remain (F-009, F-003). |
| D5. State Management & Lifecycle | 8 | 6 | 0.48 | `viewModelScope` is used and jobs cancel in `onCleared`, but state restoration/result assumptions are incomplete (F-007). |
| D6. Navigation & Screen Flow | 6 | 7 | 0.42 | Explicit Activity transitions are simple and traceable; mixed legacy/modern screens add maintenance overhead (F-006). |
| D7. Error Handling & Edge Cases | 8 | 5 | 0.40 | Missing/corrupt DB and stats failures are not surfaced with actionable error states (F-004). |
| D8. Accessibility | 6 | 6 | 0.36 | Primary Compose hint target is 48dp, but lint reports missing content descriptions in legacy XML (F-012). |
| D9. Performance & APK Size | 8 | 7 | 0.56 | APK is about 18.3 MiB/19.18 MB and DB assets are expected; repo contains unused resources and accidental native cache files (F-010). |
| D10. Internationalization (BG/EN) | 6 | 7 | 0.42 | 62 BG and 62 EN strings match exactly; app-bundle locale warning and `recreate()` flash remain (F-005). |
| D11. Build, Gradle & Dependencies | 6 | 4 | 0.24 | Debug build passes, but lint fails and Gradle/dependency warnings are numerous (F-001, F-011). |
| D12. Security & Data Privacy | 5 | 7 | 0.35 | No sensitive SharedPreferences/custom trust manager found; backup rules are generic/TODO (F-008). |
| D13. Play Store Readiness | 5 | 6 | 0.30 | targetSdk/compileSdk 35 is acceptable, but lint/locale/backup/signing issues remain (F-001, F-005, F-008). |
| D14. Testing & Quality Assurance | 4 | 4 | 0.16 | Unit suite exists but fails 2/17; no emulator/device run was available (F-002). |
| Overall | 100 | 6.1 | 6.09 → 6.1 | SUM(score × weight) / 100 = 6.09, rounded to 6.1. |

## 3. Findings Register
| ID | Severity | Dimension | Location (file path + line range or DB + table) | Evidence | Current State | Recommended Fix | Expected Impact | Effort |
|---|---|---|---|---|---|---|---|---|
| F-001 | HIGH | D11 Build, Gradle & Dependencies | `app/src/main/res/menu/quiz_menu.xml:2-7`; `app/src/main/res/layout/activity_main.xml:94-104` | `./gradlew lint` reports 2 errors and 100 warnings; first errors are `AppCompatResource` and `IncludeLayoutParam`. | Lint task fails and aborts the build quality gate. | Change menu to declare `xmlns:app` and use `app:showAsAction`; add width/height to the second `<include>` or move margin into child/container. | Restores lint gate for CI/release readiness. | S |
| F-002 | HIGH | D14 Testing & QA | `app/src/test/java/com/znam/app/MainActivityTest.kt:13-19`; `app/src/test/java/com/znam/app/QuizResultTest.kt:11-15` | `./gradlew test` reports 17 tests, 2 failed, both `KoinApplicationAlreadyStartedException`. | Regression suite cannot be trusted as a release gate. | Stop/reset Koin between Robolectric tests or use a test Application/rule that owns Koin lifecycle. | Restores automated confidence for quiz/result behavior. | S |
| F-003 | MEDIUM | D1 Architecture & Code Quality; D4 Data Layer | `app/src/main/java/com/znam/app/QuizViewModel.kt:180-193`; `app/src/main/java/com/znam/app/DatabaseProvider.kt:22-28` | ViewModel builds Room directly with `.fallbackToDestructiveMigration()` while `DatabaseProvider` uses `.addMigrations(*AppDatabase.ALL_MIGRATIONS)`. | Two divergent DB construction paths can behave differently under schema changes and DI testing. | Inject `DatabaseProvider` or `QuestionDao` into `QuizViewModel`; remove direct Room builder and `db!!`; use the migration-backed provider path. | Consistent migrations, better testability, less crash risk. | M |
| F-004 | MEDIUM | D7 Error Handling & Edge Cases | `app/src/main/java/com/znam/app/QuizViewModel.kt:233-236`; `app/src/main/java/com/znam/app/QuizViewModel.kt:410-424`; `app/src/main/java/com/znam/app/StatsViewModel.kt:74-76` | DB load failure emits `NoQuestionsAvailable`; stats insert/load exceptions are swallowed or reduce to `isLoading=false`. | Missing/corrupt DB, stats failures, and empty content are indistinguishable to user and developer. | Add typed UI error state (`EmptyQuestions`, `DatabaseUnavailable`, `StatsUnavailable`) plus non-sensitive logging; keep quiz flow best-effort only where intentional. | Faster diagnosis and clearer user recovery. | M |
| F-005 | MEDIUM | D10 Internationalization; D13 Play Store Readiness | `app/src/main/java/com/znam/app/LocaleHelper.kt:32-40`; lint `AppBundleLocaleChanges` | Lint says dynamic locale changes exist without Play Core language download calls or disabled language splitting. | BG/EN toggle persists, but app bundle language delivery could omit runtime-selected locale resources. | Add bundle language split configuration (`bundle { language { enableSplit = false } }`) or integrate Play Core language install flow; rerun lint. | Prevents broken EN/BG toggle for Play-distributed AABs. | S |
| F-006 | MEDIUM | D3 Compose Implementation; D6 Navigation | `app/src/main/java/com/znam/app/ui/QuizScreen.kt:62-71`; `QuizScreen.kt`/`StatsScreen.kt` static counts | Static scan found 0 `@Preview`, 8 `Color(0x...)` in QuizScreen, 14 in StatsScreen, and legacy XML screens still active. | Compose migration is partial and less reviewable in isolation. | Add previews for loading/quiz/answered/hints/stats-empty/stats-filled; move local colors into theme tokens; document remaining XML ownership. | Faster UI QA and more consistent theming. | M |
| F-007 | MEDIUM | D2 UI/UX Flow & Usability; D5 State Management | `app/src/main/java/com/znam/app/QuizViewModel.kt:214-225`; `app/src/main/java/com/znam/app/ResultActivity.kt:48-54`, `74-79` | ViewModel sets `totalQuestions = minOf(questions.size, 15)` but ResultActivity displays and loops against hardcoded `15`. | Result screen can misreport denominator if a DB/category has fewer than 15 available questions. | Include `totalQuestions` in `QuizResult` or derive from `questions.size`; use it in `result_format` and loop bound. | Correct score display across small/filtered pools. | S |
| F-008 | MEDIUM | D12 Security & Data Privacy; D13 Play Store Readiness | `app/src/main/AndroidManifest.xml:10-14`; `app/src/main/res/xml/data_extraction_rules.xml:7-11`; `backup_rules.xml:8-12` | App has `allowBackup="true"` and sample/TODO backup rules. | Quiz progress/language/stats are eligible for generic cloud/device transfer without explicit policy. | Decide backup policy; exclude transient quiz progress if undesired, or document and explicitly include allowed prefs/databases. | Clear privacy posture and fewer Play review surprises. | S |
| F-009 | LOW | D4 Data Layer | `class9.db/questions`; `db_entrance_exam.db/questions` | SQL duplicate query found 1 duplicated prompt in class9 and 2 duplicated prompts in entrance exam. | Content quality is mostly good, but repeated questions can appear across sessions. | Deduplicate exact duplicate `questionText` rows or intentionally tag shared questions. | Improves perceived question variety. | S |
| F-010 | LOW | D9 Performance & APK Size | `app/src/main/res/native/**`; lint `UnusedResources`; APK ZIP breakdown | Source tree contains Windows native Gradle/Jansi cache DLLs under `res/native`; lint reports 39 unused resources; APK largest entries include 447,320-byte `res/drawable/logo.png`. | APK remains acceptable, but repo/resources carry cleanup debt. | Remove accidental cache directories from `app/src/main/res`; prune unused resources; consider compressing/replacing large logo if visual quality allows. | Keeps resource set understandable and size controlled. | S |
| F-011 | LOW | D11 Build, Gradle & Dependencies | `app/build.gradle:27-68`, `106-140`; `gradle/wrapper/gradle-wrapper.properties:3` | Build logs show Gradle 10 DSL deprecation warnings; lint reports outdated Gradle/dependencies; release has duplicate `proguardFiles`. | Build passes, but future Gradle upgrade will require cleanup. | Convert Groovy DSL property calls to `prop = value`; remove duplicate `proguardFiles`; schedule dependency bumps with smoke tests. | Reduces future upgrade friction. | S |
| F-012 | LOW | D8 Accessibility | `activity_select_quiz.xml:10`; `activity_welcome.xml:25`; `hint_bubble.xml:30`; lint `ContentDescription` | Lint reports 3 missing image content descriptions. | Decorative/content images are not explicitly marked or described. | Add meaningful `android:contentDescription` or `importantForAccessibility="no"` for decorative images. | Improves TalkBack behavior and lint health. | S |

## 4. Quick Wins
- Fix `quiz_menu.xml` namespace and `activity_main.xml` include sizing to clear both lint errors (F-001).
- Add a Robolectric/Koin test rule that calls `stopKoin()` after each test or owns `startKoin()` setup (F-002).
- Replace ResultActivity hardcoded `15` with `quizResult.questions.size` or a passed `totalQuestions` field (F-007).
- Configure app bundle language splitting or Play Core language handling for runtime BG/EN switching (F-005).
- Add explicit backup/data-extraction rules for `QuizPrefs` and `quiz_stats.db` (F-008).
- Add content descriptions or decorative suppressions for the three XML image warnings (F-012).
- Remove accidental `app/src/main/res/native/**` cache files and prune unused resources (F-010).
- Convert the small set of Gradle Groovy space-assignment deprecations to `=` syntax (F-011).

## 5. Strategic Recommendations
Finish the architecture migration by making `ComposeQuizActivity + QuizViewModel + DatabaseProvider` the single production quiz path. Today the modern path exists, but legacy `MainActivity` and direct DB creation remain. Resolving F-003, F-006, and F-007 should leave one DB construction policy, one result contract, and fewer branch-specific bugs.

Turn quality gates into release blockers. The app builds, but lint and tests fail. Resolve F-001 and F-002, then require `clean assembleDebug lint test` before marking future audit items done. Expected outcome: regressions are caught before manual QA and Play upload.

Create an explicit localization/release-readiness checklist. F-005, F-008, F-011, and F-012 are small individually, but together they are Play Store readiness debt. Expected outcome: AAB delivery, backup policy, Gradle compatibility, and accessibility are verified in one repeatable path.

Continue data-quality governance for the bundled biology content. Integrity and hint coverage are excellent, but duplicates remain (F-009). Add a content validation script that checks integrity, duplicate prompts, option counts, answer membership after split/trim/case-fold, and hint coverage before every DB asset update.

## 6. Data Quality Report
| DB | File size | Questions | Tables | Integrity | hint1 coverage | hint2 coverage | Duplicate exact questionText | Null/empty question/options/correct | Correct answer membership |
|---|---:|---:|---|---|---:|---:|---:|---:|---|
| `class9.db` | 483,328 bytes | 409 | `questions`, `UserProgress` | ok | 409/409 = 100% | 409/409 = 100% | 1 duplicate prompt (`Как се формира озоновият слой?`, ids 396,469) | 0 / 0 / 0 | 0 bad after split+trim+case-fold |
| `class10.db` | 405,504 bytes | 338 | `questions`, `UserProgress` | ok | 338/338 = 100% | 338/338 = 100% | 0 | 0 / 0 / 0 | 0 bad after split+trim+case-fold |
| `db_entrance_exam.db` | 892,928 bytes | 957 | `questions`, `UserProgress` | ok | 957/957 = 100% | 957/957 = 100% | 2 duplicate prompts (`Как се формира озоновият слой?`, `Какво е симбиоза?`) | 0 / 0 / 0 | 0 bad after split+trim+case-fold |

Additional DB notes: option counts are mostly 4 per question. `class9.db` has two 5-option rows, `class10.db` has one 5-option row, and entrance exam has one 3-option row plus three 5-option rows. The raw SQL `instr(options, correctAnswer)=0` returned 18 class10 rows only because option text uses lower-case Bulgarian while `correctAnswer` starts with uppercase; app code compares selected and correct answer with `ignoreCase = true`, so these are not scored as bad after split/trim/case-fold validation.

## 7. Methodology & Coverage
Phases executed:
- Phase A — branch/status checked, `app/src/main` tree captured, `./gradlew clean assembleDebug` run successfully.
- Phase B — architecture inventory, Koin module, ViewModels, DAOs, entities, direct data-layer references, coroutine usage, `!!`, TODO, and catch patterns inspected.
- Phase C — all three required SQLite DBs opened with Python `sqlite3`; schemas, counts, integrity, hints, duplicates, empty fields, and answer membership checked.
- Phase D — launch/selection/quiz/results/statistics flow traced from source; Compose and XML layouts reviewed statically; BG/EN string parity checked.
- Phase E — source-level edge-case review performed; `./gradlew test` run and failing tests captured.
- Phase F — APK inspected by Python ZIP fallback because `apkanalyzer` is not installed; size buckets and largest entries captured.
- Phase G — manifest, SharedPreferences usage, backup rules, permissions, custom trust/network patterns, and ProGuard/R8 rules inspected.
- Phase H — targetSdk/compileSdk, Play-sensitive permissions, signing config, ad IDs, lint release blockers, and debug/test content inspected.

Tools and environment:
- Repo: `/home/alpharius/projects/GettingBiology`
- Branch: `modernization/phase-0-1`
- HEAD: `0fae7f30a0066e0980eb2674c222df21c200af4c` (`Enable R8, remove unused animation, shrink APK`)
- Git state at audit start: branch clean except untracked `tasks/` supplied for this audit.
- Java: OpenJDK 17.0.18 on Linux 6.17.0-23-generic amd64.
- Gradle: 8.13; Kotlin plugin: 2.0.21.
- Build: `./gradlew clean assembleDebug --warning-mode all` succeeded in 1m31s.
- Lint: `./gradlew lint --warning-mode all` failed; report path `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`.
- Tests: `./gradlew test --warning-mode all` failed 2/17.
- Skipped/not available: emulator or physical device runtime testing; `apkanalyzer` command was not installed, so APK size was measured via ZIP inspection.

## 8. Appendix — Raw Evidence
F-001 evidence:
- Lint command output: `Lint found 2 errors and 100 warnings. First failure: /home/alpharius/projects/GettingBiology/app/src/main/res/menu/quiz_menu.xml:7: Error: Should use app:showAsAction... [AppCompatResource]`.
- Second lint error: `/home/alpharius/projects/GettingBiology/app/src/main/res/layout/activity_main.xml:103: Error: Layout parameter layout_marginTop ignored unless both layout_width and layout_height are also specified on <include> tag [IncludeLayoutParam]`.
- `app/src/main/res/menu/quiz_menu.xml:2-7`:
```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/action_more"
        android:icon="@drawable/ic_more_vert"
        android:title="More"
        android:showAsAction="always" />
```
- `app/src/main/res/layout/activity_main.xml:99-104`:
```xml
<!-- Hint 2 Bubble -->
<include
    android:id="@+id/hint2Bubble"
    layout="@layout/hint_bubble"
    android:layout_marginTop="8dp"
    android:visibility="gone"/>
```

F-002 evidence:
- Test command output: `17 tests completed, 2 failed` and `Execution failed for task ':app:testDebugUnitTest'`.
- Failures: `MainActivityTest > testHintButtonFlow FAILED org.koin.core.error.KoinApplicationAlreadyStartedException at GlobalContext.kt:44`; `QuizResultTest > parcelableRoundTrip_preservesQuizResultData FAILED org.koin.core.error.KoinApplicationAlreadyStartedException at GlobalContext.kt:44`.
- `app/src/test/java/com/znam/app/MainActivityTest.kt:13-19`:
```kotlin
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MainActivityTest {

    @Test
    fun testHintButtonFlow() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
```
- `app/src/test/java/com/znam/app/QuizResultTest.kt:11-15`:
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QuizResultTest {
    @Test
    fun parcelableRoundTrip_preservesQuizResultData() {
```

F-003 evidence:
- `app/src/main/java/com/znam/app/QuizViewModel.kt:180-193`:
```kotlin
viewModelScope.launch {
    try {
        var loadedQuestions = withContext(Dispatchers.IO) {
            val dbName = resolveDbName(quizType)
            db = Room.databaseBuilder(
                getApplication(),
                AppDatabase::class.java,
                dbName
```
- Continuation `QuizViewModel.kt:189-193`:
```kotlin
    .createFromAsset(dbName)
    .fallbackToDestructiveMigration()  // TODO: replace with proper migration (Task 1.4)
    .build()

val allQuestions = db!!.questionDao().getAllQuestions()
```
- `app/src/main/java/com/znam/app/DatabaseProvider.kt:22-28`:
```kotlin
fun createDatabase(quizType: String): AppDatabase {
    val appContext = requireNotNull(context) { "Context is required to create AppDatabase" }.applicationContext
    val dbName = databaseNameForQuizType(quizType)
    return Room.databaseBuilder(appContext, AppDatabase::class.java, dbName)
        .createFromAsset(dbName)
        .addMigrations(*AppDatabase.ALL_MIGRATIONS)
        .build()
```

F-004 evidence:
- `app/src/main/java/com/znam/app/QuizViewModel.kt:233-236`:
```kotlin
} catch (e: Exception) {
    // TODO: expose error state to UI
    _events.value = QuizEvent.NoQuestionsAvailable
}
```
- `app/src/main/java/com/znam/app/QuizViewModel.kt:410-424`:
```kotlin
viewModelScope.launch(Dispatchers.IO) {
    try {
        statsDao?.insertSession(
            QuizSession(
                quizType = state.quizType,
```
- Continuation `QuizViewModel.kt:422-424`:
```kotlin
} catch (_: Exception) {
    // Stats persistence is best-effort; don't crash the quiz flow
}
```
- `app/src/main/java/com/znam/app/StatsViewModel.kt:74-76`:
```kotlin
} catch (e: Exception) {
    _uiState.update { it.copy(isLoading = false) }
}
```

F-005 evidence:
- Lint issue: `/home/alpharius/projects/GettingBiology/app/src/main/java/com/znam/app/LocaleHelper.kt:37: Warning: Found dynamic locale changes, but did not find corresponding Play Core library calls for downloading languages and splitting by language is not disabled in the bundle configuration [AppBundleLocaleChanges]`.
- `app/src/main/java/com/znam/app/LocaleHelper.kt:24-29`:
```kotlin
fun setSavedLanguage(context: Context, language: String) {
    val normalized = if (language == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_BG
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_APP_LANGUAGE, normalized)
        .apply()
```
- `app/src/main/java/com/znam/app/LocaleHelper.kt:32-40`:
```kotlin
private fun updateLocale(context: Context, language: String): Context {
    val locale = Locale(language)
    Locale.setDefault(locale)

    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
```
- String parity script result: `values 62`, `values-en 62`, `missing_en []`, `extra_en []`.

F-006 evidence:
- Static scan results: `QuizScreen.kt Color(0x= 8 .dp= 30 @Preview 0`; `StatsScreen.kt Color(0x= 14 .dp= 41 @Preview 0`; total `@Composable` count = 24 and total `@Preview` count = 0.
- `app/src/main/java/com/znam/app/ui/QuizScreen.kt:62-71`:
```kotlin
private val CorrectGreen = Color(0xFF2E7D32)
private val CorrectGreenBg = Color(0xFFC8E6C9)
private val IncorrectRed = Color(0xFFC62828)
private val IncorrectRedBg = Color(0xFFFFCDD2)
private val SelectedBlue = Color(0xFF1E88E5)
private val SelectedBlueBg = Color(0xFFBBDEFB)
private val HintTeal = Color(0xFF009688)
private val DefaultOptionBorder = Color(0xFFBFC8CA)
```
- Active legacy XML screens in `app/src/main`: `activity_welcome.xml`, `activity_select_quiz.xml`, `activity_result.xml`; active Compose screens: `QuizScreen.kt`, `StatsScreen.kt`.

F-007 evidence:
- `app/src/main/java/com/znam/app/QuizViewModel.kt:214-225`:
```kotlin
questions = loadedQuestions
val totalToShow = minOf(questions.size, MAX_QUESTIONS_PER_SESSION)

// Initialize user answers list with placeholder
repeat(totalToShow) {
    userAnswers.add(SKIPPED_ANSWER)
}
```
- Continuation `QuizViewModel.kt:222-225`:
```kotlin
_uiState.update {
    it.copy(
        isLoading = false,
        totalQuestions = totalToShow,
```
- `app/src/main/java/com/znam/app/ResultActivity.kt:48-54`:
```kotlin
findViewById<TextView>(R.id.result_text_view).apply {
    val fullText = getString(R.string.result_format, score, 15)
    val spannable = SpannableString(fullText)
    val tealColor = ContextCompat.getColor(this@ResultActivity, R.color.md_theme_light_primary)
    val scoreStart = fullText.indexOf("$score/15")
```
- `app/src/main/java/com/znam/app/ResultActivity.kt:74-79`:
```kotlin
questions.forEachIndexed { index, question ->
    if (index < 15) {
        val userAnswer = userAnswers.getOrNull(index) ?: SKIPPED_ANSWER
        Log.d("QuizDebug", "Displaying Question ${index + 1}: Correct Answer: ${question.correctAnswer}, User Answer: $userAnswer")
        questionsLayout.addView(createQuestionView(questionsLayout, question, userAnswer))
```

F-008 evidence:
- `app/src/main/AndroidManifest.xml:10-14`:
```xml
<application
    android:name=".GettingBiologyApplication"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
```
- `app/src/main/res/xml/data_extraction_rules.xml:7-11`:
```xml
<cloud-backup>
    <!-- TODO: Use <include> and <exclude> to control what is backed up.
    <include .../>
    <exclude .../>
    -->
```
- `app/src/main/res/xml/backup_rules.xml:8-12`:
```xml
<full-backup-content>
    <!--
   <include domain="sharedpref" path="."/>
   <exclude domain="sharedpref" path="device.xml"/>
-->
```
- SharedPreferences scan: `QuizPrefs` stores `LAST_QUIZ_TYPE`, `AnsweredQuestionIds`, and `app_language`; no secrets/API keys were found in SharedPreferences usage.

F-009 evidence:
- SQL query used: `SELECT questionText, COUNT(*), group_concat(id) FROM questions GROUP BY questionText HAVING COUNT(*)>1`.
- `class9.db` result: `('Как се формира озоновият слой?', 2, '396,469')`.
- `db_entrance_exam.db` results: `('Как се формира озоновият слой?', 2, '37,720')` and `('Какво е симбиоза?', 2, '80,1615')`.
- Integrity checks: `class9.db [('ok',)]`, `class10.db [('ok',)]`, `db_entrance_exam.db [('ok',)]`.

F-010 evidence:
- Source-file size scan: `app/src/main/res/native/e376.../windows-amd64/native-platform-file-events.dll` = 1,295,872 bytes; `app/src/main/res/native/68d5.../windows-amd64/native-platform.dll` = 139,776 bytes; `app/src/main/res/native/jansi/1.18/windows64/jansi.dll` = 26,112 bytes.
- Lint issue count summary: `UnusedResources` = 39.
- APK ZIP inspection: `app-debug.apk` = 19,183,757 bytes; largest entries include `classes.dex` 8,053,640, `classes2.dex` 7,885,332, `assets/db_entrance_exam.db` 892,928, and `res/drawable/logo.png` 447,320.
- `apkanalyzer` check output: `apkanalyzer not available; used Python zipfile breakdown`.

F-011 evidence:
- Build warning excerpt: `Properties should be assigned using the 'propName = value' syntax... removed in Gradle 10.0` at `app/build.gradle` lines 27, 31, 32, 54, and 63.
- Build warning excerpt: `BuildType 'debug' is both debuggable and has 'isMinifyEnabled' set to true. All code optimizations and obfuscation are disabled for debuggable builds.`
- `app/build.gradle:61-69`:
```groovy
release {
    minifyEnabled true
    shrinkResources true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    manifestPlaceholders = [ADMOB_APPLICATION_ID: releaseAdMobApplicationId]
    buildConfigField 'String', 'ADMOB_BANNER_AD_UNIT_ID', "\"${releaseBannerAdUnitId}\""
    buildConfigField 'String', 'ADMOB_INTERSTITIAL_AD_UNIT_ID', "\"${releaseInterstitialAdUnitId}\""
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
```
- Lint dependency summary: `GradleDependency` = 13, `NewerVersionAvailable` = 5, `AndroidGradlePluginVersion` = 1.

F-012 evidence:
- Lint content-description warnings:
  - `app/src/main/res/layout/activity_select_quiz.xml:10: Warning: Missing contentDescription attribute on image [ContentDescription]`
  - `app/src/main/res/layout/activity_welcome.xml:25: Warning: Missing contentDescription attribute on image [ContentDescription]`
  - `app/src/main/res/layout/hint_bubble.xml:30: Warning: Missing contentDescription attribute on image [ContentDescription]`
- Touch-target static check: `activity_select_quiz.xml` category buttons have `56dp` heights; `activity_welcome.xml` main buttons include `56dp` and `48dp`; Compose hint button uses `Modifier.size(48.dp)` at `QuizScreen.kt:245`.

Build/lint/test raw output locations on Hydra during audit:
- `/tmp/gettingbiology-assembleDebug.log`
- `/tmp/gettingbiology-lint.log`
- `/tmp/gettingbiology-test.log`
- Lint text report: `/home/alpharius/projects/GettingBiology/app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`
- Unit test XML results: `/home/alpharius/projects/GettingBiology/app/build/test-results/testDebugUnitTest/`
