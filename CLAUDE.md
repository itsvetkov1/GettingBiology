# CLAUDE.md - AI Assistant Guide for GettingBiology

This document provides comprehensive guidance for AI assistants working with the GettingBiology Android application codebase.

## Project Overview

**GettingBiology** (branded as "3HAM") is an educational Android application providing biology quiz assessments for Bulgarian students in grades 8-10 and entrance exam candidates. The app features:

- Multiple-choice quiz interface with 15-question sessions
- Four difficulty levels (Grade 8, 9, 10, and Entrance Exam)
- Real-time answer feedback with visual color coding
- Progress tracking across sessions
- Ad-supported monetization model
- Completely offline-capable (pre-bundled question databases)

**Key Metadata:**
- **Package**: `com.znam.app`
- **Language**: Kotlin
- **Min SDK**: 24 (Android 8.0)
- **Target SDK**: 33 (Android 13)
- **Current Version**: 1.1 (versionCode 2)
- **Primary Language**: Bulgarian (UI text)

---

## Codebase Structure

```
GettingBiology/
├── app/
│   ├── build.gradle                      # App-level build configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/znam/app/        # Kotlin source files
│   │   │   │   ├── MainActivity.kt       # Core quiz engine
│   │   │   │   ├── SelectQuizActivity.kt # Quiz category selection
│   │   │   │   ├── WelcomeActivity.kt    # App entry point with animation
│   │   │   │   ├── ResultActivity.kt     # Quiz results and review
│   │   │   │   ├── AppDatabase.kt        # Room database configuration
│   │   │   │   ├── Question.kt           # Question entity model
│   │   │   │   ├── QuestionDao.kt        # Question data access object
│   │   │   │   ├── UserProgress.kt       # Progress tracking entity
│   │   │   │   ├── UserProgressDao.kt    # Progress data access object
│   │   │   │   └── QuizResultsHolder.kt  # Singleton for inter-activity data
│   │   │   ├── assets/                   # Pre-built database files
│   │   │   │   ├── class8.db             # Grade 8 questions (323 KB)
│   │   │   │   ├── class9.db             # Grade 9 questions (483 KB)
│   │   │   │   ├── class10.db            # Grade 10 questions (405 KB)
│   │   │   │   ├── db_entrance_exam.db   # Entrance exam (892 KB)
│   │   │   │   └── dbquestions.db        # Fallback database (90 KB)
│   │   │   ├── res/
│   │   │   │   ├── drawable/             # UI graphics and shapes
│   │   │   │   ├── layout/               # Activity XML layouts
│   │   │   │   ├── mipmap-*/             # App icons (various densities)
│   │   │   │   ├── values/               # Strings, colors, themes
│   │   │   │   ├── values-night/         # Dark mode themes
│   │   │   │   └── xml/                  # Backup and security configs
│   │   │   └── AndroidManifest.xml       # App configuration and permissions
│   │   ├── test/                         # Unit tests
│   │   └── androidTest/                  # Instrumented tests
│   └── schemas/                          # Room database schema exports
├── gradle/                               # Gradle wrapper files
├── build.gradle                          # Project-level build config
├── settings.gradle                       # Project settings
├── gradle.properties                     # Gradle configuration properties
├── gradlew                               # Gradle wrapper script (Unix)
├── gradlew.bat                           # Gradle wrapper script (Windows)
└── .gitignore                            # Git ignore patterns
```

---

## Key Technologies & Dependencies

### Core Framework
- **Kotlin**: 1.9.20
- **Android Gradle Plugin**: 8.2.1
- **Compile SDK**: 34
- **Java Compatibility**: 1.8

### Major Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| AndroidX Core KTX | 1.12.0 | Kotlin extensions for Android |
| AndroidX AppCompat | 1.6.1 | Backward-compatible UI components |
| Material Components | 1.11.0 | Material Design UI elements |
| Room Runtime | 2.6.1 | SQLite database abstraction |
| Room KTX | 2.6.1 | Kotlin coroutines support for Room |
| Glide | 4.12.0 | Image loading (for welcome GIF) |
| Google Play Services Ads | 22.6.0 | Banner and interstitial ads |

### Build Plugins
- `com.android.application`
- `org.jetbrains.kotlin.android`
- `kotlin-parcelize` (for Parcelable generation)
- `kotlin-kapt` (for annotation processing)

---

## Architecture & Design Patterns

### Overall Architecture
**Type**: Traditional Android Activity-based architecture (not MVVM/MVP)

**Data Flow**:
```
User Input → Activity → CoroutineScope(Dispatchers.IO) → Room Database
                    ← (UI Thread) ← Result
```

### Key Design Patterns

#### 1. Repository Pattern (Lightweight)
- Room DAOs act as data repositories
- Database operations isolated from UI logic
- Coroutines handle async execution

#### 2. Singleton Pattern
```kotlin
object QuizResultsHolder {
    var results: List<Triple<String, String, String>>? = null
}
```
Used for passing quiz results between MainActivity and ResultActivity.

#### 3. Data Class Pattern
All models use Kotlin data classes for automatic implementations:
- `Question` (implements Parcelable)
- `UserProgress`

#### 4. Builder Pattern
Frequent use of `.apply {}` blocks for object initialization:
```kotlin
val adRequest = AdRequest.Builder().build()
interstitialAd?.apply {
    fullScreenContentCallback = object : FullScreenContentCallback() { ... }
}
```

### Database Architecture

**Room Database Configuration**:
- Version: 2
- Migration Strategy: `fallbackToDestructiveMigration()` (development mode)
- Schema Export: Enabled to `app/schemas/`
- **Important**: Each quiz type loads its own database file from assets at runtime

**Database Selection Logic**:
```kotlin
when (quizType) {
    "class8" -> createFromAsset("class8.db")
    "class9" -> createFromAsset("class9.db")
    "class10" -> createFromAsset("class10.db")
    "entrance_exam" -> createFromAsset("db_entrance_exam.db")
    else -> createFromAsset("dbquestions.db")
}
```

---

## Activity Flow & Navigation

### Complete User Journey

```
┌─────────────────────┐
│  WelcomeActivity    │ (Launcher)
│  - GIF animation    │
│  - 3-second delay   │
│  - Start button     │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│ SelectQuizActivity  │
│  - 4 quiz buttons   │
│  - Saves selection  │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│   MainActivity      │
│  - Load questions   │
│  - Display quiz UI  │
│  - Check answers    │
│  - Banner ad        │
│  - Max 15 questions │
└──────────┬──────────┘
           │
           v (Interstitial Ad)
           │
           v
┌─────────────────────┐
│  ResultActivity     │
│  - Final score      │
│  - Answer review    │
│  - Color coding     │
└─────────────────────┘
```

### Activity Responsibilities

| Activity | Launch Mode | Key Responsibilities | Intent Data |
|----------|-------------|---------------------|-------------|
| **WelcomeActivity** | LAUNCHER | Display animated splash, navigate to quiz selection | None |
| **SelectQuizActivity** | Standard | Present quiz category buttons, save quiz type to SharedPreferences | None |
| **MainActivity** | Standard | Load questions, manage quiz state, validate answers, track progress, display ads | `QUIZ_TYPE` (String) |
| **ResultActivity** | Standard | Display final score, show detailed answer review | None (uses QuizResultsHolder) |

---

## Code Conventions & Standards

### Naming Conventions

**Activities**: `<Feature>Activity.kt`
```kotlin
WelcomeActivity.kt
SelectQuizActivity.kt
MainActivity.kt
ResultActivity.kt
```

**Database Components**:
```kotlin
AppDatabase.kt          // Database class
<Entity>.kt             // Entity models (Question, UserProgress)
<Entity>Dao.kt          // DAO interfaces
```

**Layout Files**: `activity_<name>.xml`
```xml
activity_welcome.xml
activity_select_quiz.xml
activity_main.xml
activity_result.xml
```

**Drawable Resources**: `<purpose>_<description>.xml`
```xml
correct_answer_background.xml
incorrect_answer_background.xml
radio_button_custom.xml
```

### Kotlin Style Guide

**Variable Declaration**:
```kotlin
// Prefer immutable (val) over mutable (var)
val questionText: String = question.questionText
var currentQuestionIndex = 0

// Use lateinit for views initialized in onCreate()
private lateinit var radioGroup: RadioGroup
private lateinit var submitButton: Button
```

**String Templates**:
```kotlin
// Use string templates for interpolation
hintText.text = "Въпрос ${currentQuestionIndex + 1}/15"
```

**Safe Calls and Elvis Operator**:
```kotlin
// Use safe calls (?.) and elvis operator (?:)
val lastProgress = progressDao?.getLastProgress() ?: return
questions.getOrNull(currentQuestionIndex)?.let { question -> ... }
```

**Coroutine Pattern**:
```kotlin
// All database operations on IO dispatcher
CoroutineScope(Dispatchers.IO).launch {
    val allQuestions = questionDao.getAllQuestions()
    withContext(Dispatchers.Main) {
        // Update UI
    }
}
```

### Resource Naming

**Colors** (in `values/colors.xml`):
```xml
<color name="turquoise">#00BCD4</color>
<color name="green">#1AA637</color>
<color name="transparent_white">#CCFFFFFF</color>
```

**Strings** (in `values/strings.xml`):
- All user-facing text in Bulgarian
- Use descriptive keys: `app_name`, `quiz_title`, etc.

### Code Organization

**File Structure** (MainActivity.kt example):
```kotlin
class MainActivity : AppCompatActivity() {
    // 1. Properties (lateinit vars, regular vars)
    private lateinit var radioGroup: RadioGroup
    private var currentQuestionIndex = 0

    // 2. Lifecycle methods
    override fun onCreate(savedInstanceState: Bundle?) { ... }
    override fun onPause() { ... }
    override fun onResume() { ... }

    // 3. Core logic methods
    private fun loadQuestion() { ... }
    private fun checkAnswer() { ... }

    // 4. Helper methods
    private fun saveProgress() { ... }
    private fun loadProgress() { ... }
}
```

---

## Database Schema Reference

### Question Entity

```kotlin
@Entity(tableName = "questions")
@Parcelize
data class Question(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val questionText: String,        // Full question text
    val options: String,             // Semicolon-separated: "A;B;C;D"
    val correctAnswer: String        // Single correct answer text
) : Parcelable
```

**Important Notes**:
- `options` field stores semicolon-delimited strings (`;`)
- When parsing: `options.split(";")` to get array
- `correctAnswer` matches one of the options exactly

### UserProgress Entity

```kotlin
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey
    val questionId: Int,
    val isCompleted: Boolean
)
```

**Usage**:
- Tracks which questions have been answered
- Primary key on `questionId` prevents duplicate tracking
- DAO uses `OnConflictStrategy.REPLACE` for updates

### DAO Operations

**QuestionDao**:
```kotlin
@Query("SELECT * FROM questions")
fun getAllQuestions(): List<Question>

@Insert
fun insertAll(vararg questions: Question)
```

**UserProgressDao**:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
fun insertProgress(progress: UserProgress)

@Query("SELECT * FROM user_progress ORDER BY questionId DESC LIMIT 1")
fun getLastProgress(): UserProgress?
```

---

## Development Workflows

### Building the App

**Debug Build**:
```bash
./gradlew assembleDebug
```

**Release Build**:
```bash
./gradlew assembleRelease
```

**Install on Device**:
```bash
./gradlew installDebug
```

### Running Tests

**Unit Tests**:
```bash
./gradlew test
```

**Instrumented Tests**:
```bash
./gradlew connectedAndroidTest
```

### Database Schema Export

Room automatically exports schema to `app/schemas/` when version changes. To manually trigger:
```bash
./gradlew build
```
Check `app/schemas/com.znam.app.AppDatabase/` for JSON schema files.

### Working with Database Assets

**Adding New Questions**:
1. Open the appropriate `.db` file in `app/src/main/assets/` using SQLite browser
2. Add questions to `questions` table following the schema
3. Ensure `options` field uses semicolon delimiter
4. Save and test in app

**Creating New Database Level**:
1. Create `.db` file with `questions` table matching schema
2. Place in `app/src/main/assets/`
3. Update `AppDatabase.kt` to add new case in database builder
4. Add button in `SelectQuizActivity.kt`
5. Update layout `activity_select_quiz.xml`

---

## Common Development Tasks

### Task 1: Adding a New Question Category

**Steps**:
1. Create database file: `app/src/main/assets/new_category.db`
2. Update `MainActivity.kt`:
   ```kotlin
   when (quizType) {
       // ... existing cases
       "new_category" -> createFromAsset("new_category.db")
   }
   ```
3. Add button in `res/layout/activity_select_quiz.xml`
4. Update `SelectQuizActivity.kt` button click listener:
   ```kotlin
   newCategoryButton.setOnClickListener {
       sharedPreferences.edit().putString("LAST_QUIZ_TYPE", "new_category").apply()
       navigateToQuiz("new_category")
   }
   ```

### Task 2: Modifying Quiz Length

**Current Limit**: 15 questions (hardcoded)

**Change in** `MainActivity.kt:loadQuestion()`:
```kotlin
if (currentQuestionIndex >= 15) {  // Change this value
    showResults()
    return
}
```

### Task 3: Adjusting Answer Feedback Delay

**Current Delay**: 2 seconds

**Change in** `MainActivity.kt:checkAnswer()`:
```kotlin
radioGroup.postDelayed({
    loadQuestion()
}, 2000)  // Change milliseconds here
```

### Task 4: Customizing Visual Feedback Colors

**Edit** `res/drawable/correct_answer_background.xml`:
```xml
<solid android:color="#4CAF50"/>  <!-- Green for correct -->
```

**Edit** `res/drawable/incorrect_answer_background.xml`:
```xml
<solid android:color="#F44336"/>  <!-- Red for incorrect -->
```

### Task 5: Modifying Ad Units

**Update** `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
```

**Update** `MainActivity.kt` (banner ad):
```kotlin
adView.adUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
```

**Update** `MainActivity.kt` (interstitial ad):
```kotlin
InterstitialAd.load(this, "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX", ...)
```

---

## Testing Guidelines

### Unit Testing

**Test Location**: `app/src/test/java/com/example/app/`

**Current Test**: `ExampleUnitTest.kt` (placeholder)

**Recommended Tests**:
- Question option parsing logic
- Score calculation
- Progress tracking logic
- Database query validation

**Example Test Structure**:
```kotlin
@Test
fun questionOptions_parseCorrectly() {
    val question = Question(
        id = 1,
        questionText = "Test?",
        options = "A;B;C;D",
        correctAnswer = "A"
    )
    val parsed = question.options.split(";")
    assertEquals(4, parsed.size)
}
```

### Instrumented Testing

**Test Location**: `app/src/androidTest/java/com/example/app/`

**Recommended Tests**:
- Activity navigation flow
- Database operations
- SharedPreferences persistence
- Ad loading (with test ad units)

---

## Build & Deployment

### Version Management

**Update Version** in `app/build.gradle`:
```gradle
defaultConfig {
    versionCode 3        // Increment for each release
    versionName "1.2"    // Semantic versioning
}
```

### ProGuard/R8 Configuration

**Current Setting**: Minification disabled

**Enable for Release** in `app/build.gradle`:
```gradle
buildTypes {
    release {
        minifyEnabled true  // Enable code shrinking
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),
                      'proguard-rules.pro'
    }
}
```

### Signing Configuration

**Note**: No signing configuration currently in `build.gradle`

**Add Signing** for release builds:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("your-keystore.jks")
            storePassword "your-password"
            keyAlias "your-alias"
            keyPassword "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### Google Play Release Checklist

1. Update `versionCode` and `versionName`
2. Test on multiple devices/Android versions
3. Verify ad units are production (not test IDs)
4. Enable ProGuard/R8 minification
5. Generate signed APK/AAB
6. Test signed build thoroughly
7. Create release notes in Bulgarian
8. Upload to Google Play Console

---

## Git Workflow

### Branch Strategy

**Current Development Branch**:
```
claude/claude-md-mi9xwx1fodpkm64l-01ThtigsqPaM6DtQLa95DBue
```

**Convention**: Claude-generated branches follow pattern `claude/<session-id>`

### Commit Message Style

Based on git history, follow this pattern:
```
<verb> <description in lowercase>

Examples:
- disable submit button upon usage
- renaming package
- separate dbs
- user progress tracking implemented
```

**Preferred Verbs**:
- `add`: New features
- `fix`: Bug fixes
- `update`: Enhancements to existing features
- `refactor`: Code restructuring
- `remove`: Deletion of code/features

### Important Git Commands

**Check Current Branch**:
```bash
git status
git branch
```

**Push to Feature Branch**:
```bash
git push -u origin claude/claude-md-mi9xwx1fodpkm64l-01ThtigsqPaM6DtQLa95DBue
```

**Retry on Network Errors**: Use exponential backoff (2s, 4s, 8s, 16s)

---

## Important Constraints & Known Limitations

### Technical Constraints

1. **15-Question Session Limit**: Hardcoded in `MainActivity.kt:loadQuestion()`
2. **Single-Attempt Questions**: Once answered, questions excluded from future sessions
3. **No Question Retry**: Cannot re-answer same question in a session
4. **Offline-Only**: No network sync, no remote question updates
5. **Semicolon Delimiter**: Options parsing breaks if answers contain `;`

### Database Limitations

1. **Destructive Migration**: Schema changes wipe all data (development mode)
2. **No Backup/Restore**: User progress not backed up to cloud
3. **Flat Option Storage**: Options stored as delimited string, not normalized
4. **No Question Metadata**: No difficulty rating, tags, or categories per question

### UI/UX Limitations

1. **No Question Skip**: Must answer current question to proceed
2. **2-Second Delay**: Cannot proceed faster even if desired
3. **Bulgarian-Only**: No internationalization support
4. **No Dark Mode Images**: Drawables not optimized for night theme
5. **Large GIF Asset**: 28.6 MB welcome animation impacts APK size

### Ad Integration Constraints

1. **Network Required**: Ads require internet connectivity
2. **No Fallback UI**: If ad fails, user sees empty space
3. **Hardcoded Ad Units**: Changing requires code modification

---

## Security & Privacy Considerations

### Permissions

**Required Permissions**:
- `INTERNET`: For ad serving
- `ACCESS_NETWORK_STATE`: Check connectivity before loading ads
- `AD_ID`: Google Ads personalization

### Data Privacy

**Local Storage Only**:
- All user progress stored locally in Room database
- No user accounts or authentication
- No data sent to external servers (except ad impressions)

**Backup Configuration**:
- Backup rules defined in `res/xml/backup_rules.xml`
- Data extraction rules in `res/xml/data_extraction_rules.xml`

### Best Practices for Development

1. **Never Commit Sensitive Data**: Keep test ad units out of production
2. **Validate User Input**: Although limited, ensure quiz selection is valid
3. **Handle Null Cases**: Use safe calls for all database operations
4. **Test Offline Mode**: Ensure quiz functionality works without network

---

## Troubleshooting Guide

### Common Issues

#### Issue 1: Database Not Found
**Symptom**: App crashes when selecting quiz type
**Cause**: Database file missing from assets
**Solution**: Verify `.db` file exists in `app/src/main/assets/`

#### Issue 2: Options Not Displaying
**Symptom**: Radio buttons empty or show single option
**Cause**: Incorrect delimiter in database `options` field
**Solution**: Ensure options use semicolon (`;`) separator, not comma or pipe

#### Issue 3: Ads Not Loading
**Symptom**: Empty space where ads should appear
**Cause**: Invalid ad unit ID or network issues
**Solution**:
- Verify ad unit IDs in code and manifest
- Test with Google test ad units first
- Check `INTERNET` permission granted

#### Issue 4: Progress Not Saving
**Symptom**: Answered questions reappear
**Cause**: SharedPreferences not persisting correctly
**Solution**: Check `onPause()` calls `saveProgress()` properly

#### Issue 5: Build Fails on Room Schema Export
**Symptom**: Compilation error about missing schema directory
**Cause**: Room schema export directory not created
**Solution**:
```bash
mkdir -p app/schemas
./gradlew clean build
```

---

## Performance Optimization Tips

### Database Performance

1. **Lazy Loading**: Questions loaded only when needed
2. **Index Primary Keys**: Already done via `@PrimaryKey`
3. **Async Operations**: All DB queries on IO dispatcher
4. **Avoid N+1 Queries**: Load all questions once, filter in memory

### UI Performance

1. **Reduce GIF Size**: 28.6 MB welcome animation is excessive
   - Consider converting to WebP or using Lottie animation
2. **Image Loading**: Glide handles caching automatically
3. **RecyclerView**: Not used (only 15 questions), but consider for large result lists

### APK Size Reduction

**Current Size**: ~30+ MB (due to GIF asset)

**Optimization Strategies**:
1. Enable R8/ProGuard shrinking
2. Use vector drawables instead of PNG
3. Compress or replace welcome GIF
4. Enable APK splits by density:
   ```gradle
   splits {
       density {
           enable true
           exclude "ldpi", "mdpi"
       }
   }
   ```

---

## Resources & Documentation

### Official Documentation
- [Android Developers Guide](https://developer.android.com/docs)
- [Kotlin Language Reference](https://kotlinlang.org/docs/reference/)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Google Mobile Ads SDK](https://developers.google.com/admob/android/quick-start)

### Project-Specific Resources
- **Main Package**: `com.znam.app`
- **Database Version**: 2
- **Schema Location**: `app/schemas/`
- **Assets Location**: `app/src/main/assets/`

### Useful Gradle Commands

```bash
# List all tasks
./gradlew tasks

# Dependency tree
./gradlew app:dependencies

# Clean build
./gradlew clean build

# Generate APK
./gradlew assembleDebug

# Run all checks
./gradlew check
```

---

## AI Assistant Guidelines

### When Making Changes

1. **Read Before Modifying**: Always read existing files before making changes
2. **Preserve Bulgarian Text**: Keep all user-facing strings in Bulgarian unless instructed otherwise
3. **Test Database Changes**: Validate schema changes don't break existing data
4. **Maintain Consistency**: Follow existing naming conventions and code style
5. **Update Documentation**: Keep this CLAUDE.md updated with significant changes

### Code Review Checklist

- [ ] Kotlin style guide followed (immutability preference, safe calls)
- [ ] Database operations on IO dispatcher
- [ ] UI updates on Main dispatcher
- [ ] Resource files follow naming conventions
- [ ] No hardcoded strings in code (use `strings.xml`)
- [ ] Null safety maintained throughout
- [ ] Error handling implemented for critical paths
- [ ] Ad unit IDs not changed accidentally
- [ ] Version numbers updated appropriately

### Testing Before Commit

1. Build succeeds: `./gradlew build`
2. Unit tests pass: `./gradlew test`
3. App installs: `./gradlew installDebug`
4. Manual test: Complete one quiz end-to-end
5. Verify ads display (if modified)
6. Check progress persists across app restarts

---

## Contact & Support

For questions about this codebase or project, refer to:
- Git commit history for context on changes
- This CLAUDE.md for architectural decisions
- Official Android documentation for framework questions

**Last Updated**: 2025-11-22
**Document Version**: 1.0
**Codebase Version**: 1.1 (versionCode 2)

---

*This document is maintained for AI assistants working with the GettingBiology codebase. Keep it updated as the project evolves.*
