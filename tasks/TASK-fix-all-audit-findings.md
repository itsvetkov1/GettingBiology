# TASK: Fix All 12 Audit Findings

**Branch:** `modernization/phase-0-1`
**Repo:** `/home/alpharius/projects/GettingBiology`
**Priority:** Execute in order below (HIGH first, then MEDIUM, then LOW)

---

## F-001 [HIGH] Fix lint errors (2 errors blocking lint gate)

### Fix 1a: `app/src/main/res/menu/quiz_menu.xml`
Add `xmlns:app` namespace and change `android:showAsAction` to `app:showAsAction`:
```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:app="http://schemas.android.com/apk/res-auto">
    <item
        android:id="@+id/action_more"
        android:icon="@drawable/ic_more_vert"
        android:title="More"
        app:showAsAction="always" />
</menu>
```

### Fix 1b: `app/src/main/res/layout/activity_main.xml` (~line 99-104)
Add `android:layout_width` and `android:layout_height` to the `<include>` tag for hint2Bubble:
```xml
<include
    android:id="@+id/hint2Bubble"
    layout="@layout/hint_bubble"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:visibility="gone"/>
```

---

## F-002 [HIGH] Fix failing Koin tests (2/17 tests fail)

**Files:**
- `app/src/test/java/com/znam/app/MainActivityTest.kt`
- `app/src/test/java/com/znam/app/QuizResultTest.kt`

**Root cause:** Both test classes launch activities that trigger `startKoin()` in `GettingBiologyApplication`, but Koin is already started from a previous test class. No cleanup between test classes.

**Fix:** Add a `@Before` or `@After` method (or a `@Rule`) to stop Koin between tests. The simplest approach:

Add to BOTH test files:
```kotlin
import org.koin.core.context.stopKoin

@Before
fun setUp() {
    stopKoin() // Reset Koin before each test to avoid KoinApplicationAlreadyStartedException
}
```

Or alternatively, create a shared test rule in `app/src/test/java/com/znam/app/KoinTestRule.kt`:
```kotlin
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin

class KoinTestRule : TestWatcher() {
    override fun starting(description: Description?) {
        stopKoin()
    }
}
```
Then in each test class: `@get:Rule val koinRule = KoinTestRule()`

---

## F-003 [MEDIUM] QuizViewModel bypasses Koin DI for DB construction

**File:** `app/src/main/java/com/znam/app/QuizViewModel.kt` (~lines 180-193)

**Problem:** ViewModel builds Room directly with `.fallbackToDestructiveMigration()` instead of using the Koin-provided `DatabaseProvider` which uses `.addMigrations(*AppDatabase.ALL_MIGRATIONS)`.

**Fix:** Inject `DatabaseProvider` via Koin and use it instead of direct Room builder:

1. Add `DatabaseProvider` as a constructor dependency or inject via Koin `get()`:
```kotlin
private val databaseProvider: DatabaseProvider = get()
```

2. Replace the direct Room.databaseBuilder block (~lines 184-192) with:
```kotlin
val dbName = resolveDbName(quizType)
db = databaseProvider.createDatabase(quizType)
val allQuestions = db!!.questionDao().getAllQuestions()
```

3. Remove the `db!!` usage — store as a non-null local:
```kotlin
val database = databaseProvider.createDatabase(quizType)
db = database
val allQuestions = database.questionDao().getAllQuestions()
```

4. Remove the `fallbackToDestructiveMigration()` code path entirely.

---

## F-004 [MEDIUM] Improve error handling — typed UI error states

**Files:**
- `app/src/main/java/com/znam/app/QuizViewModel.kt` (~lines 233-236, 410-424)
- `app/src/main/java/com/znam/app/StatsViewModel.kt` (~lines 74-76)

**Fix:**

1. In `QuizViewModel.kt`, replace the generic catch at line 233:
```kotlin
} catch (e: Exception) {
    Log.e("QuizViewModel", "Failed to load questions", e)
    _events.value = QuizEvent.NoQuestionsAvailable
}
```
(Add `Log.e` so failures are diagnosable.)

2. In `QuizViewModel.kt` stats catch at line 422, add logging:
```kotlin
} catch (e: Exception) {
    Log.w("QuizViewModel", "Stats persistence failed (best-effort)", e)
}
```

3. In `StatsViewModel.kt` at line 74, add logging:
```kotlin
} catch (e: Exception) {
    Log.e("StatsViewModel", "Failed to load stats", e)
    _uiState.update { it.copy(isLoading = false) }
}
```

---

## F-005 [MEDIUM] Fix app bundle locale configuration

**File:** `app/build.gradle`

**Fix:** Add bundle language split configuration inside the `android {}` block:
```groovy
bundle {
    language {
        enableSplit = false
    }
}
```
This ensures both BG and EN string resources are always included in the APK regardless of device language.

---

## F-006 [MEDIUM] Add Compose @Preview functions and extract hardcoded colors

**Files:**
- `app/src/main/java/com/znam/app/ui/QuizScreen.kt`
- `app/src/main/java/com/znam/app/ui/StatsScreen.kt`

**Fix part A — Colors:** The 8 hardcoded colors in QuizScreen.kt (lines 62-69) are already extracted as top-level `private val` constants, which is acceptable. Leave them as-is but add a comment:
```kotlin
// Quiz-specific semantic colors — candidates for theme extraction in future Material3 migration
```

**Fix part B — Previews:** Add `@Preview` composables at the bottom of each file. For QuizScreen.kt, add previews for key states:
```kotlin
@Preview(showBackground = true)
@Composable
private fun QuizScreenLoadingPreview() {
    QuizScreenContent(
        uiState = QuizUiState(isLoading = true),
        onAnswerSelected = {},
        onNextQuestion = {},
        onHintRequested = {}
    )
}
```
Add similar previews for: quiz question shown, answer selected (correct), answer selected (incorrect), hints visible. Match the actual composable parameters and state class.

For StatsScreen.kt, add previews for: loading, empty stats, populated stats.

**Note:** You'll need to check the actual composable signatures and state classes to write correct previews. The key requirement is having at least 1-2 previews per screen file.

---

## F-007 [MEDIUM] Fix hardcoded `15` in ResultActivity

**File:** `app/src/main/java/com/znam/app/ResultActivity.kt` (~lines 48-54, 74-79)

**Fix:**

1. Get the actual total from the quiz result or questions list size:
```kotlin
val totalQuestions = questions.size.coerceAtMost(MAX_QUESTIONS_PER_SESSION)
```
Or if `QuizResult` already has a `totalQuestions` field, use it.

2. Replace hardcoded `15` in result_format (line 49):
```kotlin
val fullText = getString(R.string.result_format, score, totalQuestions)
```

3. Replace hardcoded `15` in the score string matching (line 52):
```kotlin
val scoreStart = fullText.indexOf("$score/$totalQuestions")
```

4. Replace hardcoded `15` in the loop guard (line 73):
```kotlin
if (index < totalQuestions) {
```

5. Check if `MAX_QUESTIONS_PER_SESSION` constant is accessible from ResultActivity. If not, pass it via the intent extras or derive from the data.

---

## F-008 [MEDIUM] Configure explicit backup rules

**File:** `app/src/main/res/xml/backup_rules.xml`
Replace the TODO content:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="QuizPrefs.xml" />
    <exclude domain="database" path="class9.db" />
    <exclude domain="database" path="class10.db" />
    <exclude domain="database" path="db_entrance_exam.db" />
</full-backup-content>
```

**File:** `app/src/main/res/xml/data_extraction_rules.xml`
Replace the TODO content:
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="QuizPrefs.xml" />
        <exclude domain="database" path="class9.db" />
        <exclude domain="database" path="class10.db" />
        <exclude domain="database" path="db_entrance_exam.db" />
    </cloud-backup>
    <device-transfer>
        <include domain="sharedpref" path="QuizPrefs.xml" />
        <include domain="database" path="quiz_stats.db" />
    </device-transfer>
</data-extraction-rules>
```
Rationale: back up user preferences (language, last quiz type); exclude pre-bundled asset DBs (they're already in the APK); allow stats DB transfer between devices.

---

## F-009 [LOW] Remove duplicate questions from databases

**Duplicates found:**
- `class9.db`: id 469 duplicates id 396 (same questionText)
- `db_entrance_exam.db`: id 720 duplicates id 37; id 1615 duplicates id 80

**Fix:** Run sqlite3 commands to delete the higher-ID duplicates:
```bash
sqlite3 app/src/main/assets/class9.db "DELETE FROM questions WHERE id = 469;"
sqlite3 app/src/main/assets/db_entrance_exam.db "DELETE FROM questions WHERE id IN (720, 1615);"
```
Then verify: `sqlite3 <db> "SELECT COUNT(*) FROM questions;"` — class9 should be 408, entrance should be 955.

---

## F-010 [LOW] Remove accidental native cache files and unused resources

**Fix:**
1. Delete the entire `app/src/main/res/native/` directory:
```bash
rm -rf app/src/main/res/native/
```

2. Run lint to identify unused resources, then remove them. At minimum, the `native/` directory removal cleans up ~1.4MB of Windows DLL files that should never have been in the Android resources.

---

## F-011 [LOW] Fix Gradle DSL deprecations and duplicate proguardFiles

**File:** `app/build.gradle`

1. Convert space-assignment syntax to `=` at lines ~27, 31, 32, 54, 63:
```groovy
// Before:
compileSdk 35
// After:
compileSdk = 35

// Before:
minSdk 26
// After:
minSdk = 26

// etc. for targetSdk, versionCode, versionName, minifyEnabled, shrinkResources
```

2. Remove the duplicate `proguardFiles` line in the `release` block (~line 330 in audit evidence). The `release` block has `proguardFiles` declared twice. Remove the second one.

---

## F-012 [LOW] Add content descriptions to images

**Files and fixes:**

1. `app/src/main/res/layout/activity_select_quiz.xml` (~line 10):
```xml
android:contentDescription="@string/app_logo_description"
```
Or if decorative: `android:importantForAccessibility="no"`

2. `app/src/main/res/layout/activity_welcome.xml` (~line 25):
```xml
android:contentDescription="@string/app_logo_description"
```
Or: `android:importantForAccessibility="no"`

3. `app/src/main/res/layout/hint_bubble.xml` (~line 30):
```xml
android:contentDescription="@string/hint_icon_description"
```

Add the string resources if they don't exist:
```xml
<!-- In app/src/main/res/values/strings.xml -->
<string name="app_logo_description">App logo</string>
<string name="hint_icon_description">Hint</string>

<!-- In app/src/main/res/values-en/strings.xml -->
<string name="app_logo_description">App logo</string>
<string name="hint_icon_description">Hint</string>
```

---

## VERIFICATION (mandatory after all fixes)

Run these commands and confirm all pass:
```bash
cd /home/alpharius/projects/GettingBiology
./gradlew clean assembleDebug
./gradlew lint        # Must show 0 errors (warnings OK)
./gradlew test        # Must show 17/17 pass (or more if tests added)
```

Save the verification output to `/home/alpharius/projects/GettingBiology/FIXES-VERIFICATION.md`.

Commit all changes with message: `fix: resolve all 12 audit findings (F-001 through F-012)`
