# GEMINI.md - AI Assistant Guide for GettingBiology

This document provides comprehensive guidance for AI assistants working with the GettingBiology Android application codebase.

## Project Overview

**GettingBiology** (branded as "3HAM") is an educational Android application providing biology quiz assessments for Bulgarian students in grades 8-10 and entrance exam candidates.

**Key Metadata:**
- **Package**: `com.znam.app`
- **Language**: Kotlin 1.9.24
- **Min SDK**: 28 (Android 9.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Current Version**: 4 (versionCode 4)
- **Primary Language**: Bulgarian (UI text)
- **Architecture**: Traditional Android Activity-based (Migrating to MVVM recommended)
- **Database**: Room (SQLite) with pre-packaged assets

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
│   │   │   │   ├── WelcomeActivity.kt    # App entry point
│   │   │   │   ├── ResultActivity.kt     # Quiz results
│   │   │   │   ├── AppDatabase.kt        # Room database config
│   │   │   │   ├── Question.kt           # Entity model
│   │   │   │   └── QuizResultsHolder.kt  # Singleton data holder
│   │   │   ├── assets/                   # Pre-built databases
│   │   │   │   ├── class8.db, class9.db, class10.db, db_entrance_exam.db
│   │   │   ├── res/                      # Resources (layout, values, drawable)
│   │   │   └── AndroidManifest.xml       # App manifest
│   │   └── test/                         # Unit tests
│   └── schemas/                          # Room schemas
├── gradle/                               # Gradle wrapper
├── build.gradle                          # Project-level build config
└── settings.gradle                       # Project settings
```

---

## Tech Stack & Dependencies

- **Core**: Kotlin, Android SDK 34
- **UI**: Android Views (XML), Material Components 1.11.0
- **Database**: Room Runtime/KTX 2.6.1
- **Ads**: Google Play Services Ads 22.6.0, UMP SDK 1.0.0
- **Images**: Glide 4.12.0
- **Build**: Gradle 8.5.1, Android Gradle Plugin 8.5.1

---

## Architecture & Design Patterns

### Data Flow
`User Input` → `Activity` → `CoroutineScope(Dispatchers.IO)` → `Room Database` → `UI Update (Main Thread)`

### Key Components
1.  **Activities**: Handle UI and logic (Welcome, SelectQuiz, Main, Result).
2.  **Room Database**:
    *   `AppDatabase`: Abstract class extending `RoomDatabase`.
    *   `QuestionDao`: Data Access Object for questions.
    *   **Pre-packaged Assets**: Databases are copied from `assets/` to internal storage.
3.  **Singleton State**: `QuizResultsHolder` passes data between `MainActivity` and `ResultActivity` to avoid Intent limits.

### Database Strategy
*   Different databases for each quiz type (class8, class9, etc.).
*   `Room.databaseBuilder(...).createFromAsset(...)` used for initialization.
*   **Schema**:
    *   `questions`: `id` (PK), `questionText`, `options` (semicolon-separated), `correctAnswer`.
    *   `user_progress`: `questionId` (PK), `isCompleted`.

---

## Application Flow

1.  **WelcomeActivity**: Splash screen with GIF → `SelectQuizActivity`.
2.  **SelectQuizActivity**: User chooses category (Grade 8, 9, 10, Exam) → Saves pref → `MainActivity`.
3.  **MainActivity**:
    *   Initializes Ads (Banner + Interstitial).
    *   Loads specific database based on selection.
    *   Fetches questions (filtering out answered ones).
    *   Runs quiz loop (15 questions).
    *   Shows Interstitial Ad → `ResultActivity`.
4.  **ResultActivity**: Displays score and detailed answer review.

---

## Coding Guidelines

### Kotlin Style
*   Use `val` over `var` where possible.
*   Use `lateinit` for Views.
*   Use Safe Calls (`?.`) and Elvis Operator (`?:`).
*   **Coroutines**: Dispatch DB operations to `Dispatchers.IO`, UI updates to `Dispatchers.Main`.

### Naming Conventions
*   **Classes**: PascalCase (`MainActivity`)
*   **Functions/Variables**: camelCase (`loadQuestion`, `currentScore`)
*   **Resources**: snake_case (`activity_main.xml`, `ic_launcher_background`)
*   **Layout IDs**: snake_case (`question_text_view`, `next_button`)

### Critical Constraints
*   **Options format**: `options` string in DB must be separated by `;`.
*   **Quiz Limit**: Hardcoded to 15 questions per session.
*   **Min SDK**: 28 – Ensure APIs used are compatible or use checks.

---

## Common Commands

### Build
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### Test
```bash
./gradlew test                  # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

### Database Schema
Triggers auto-export to `app/schemas` if version changes.
```bash
./gradlew build
```

---

## Debugging Tips

*   **Ads**: Need `INTERNET` permission. Test IDs used in development.
*   **Database**: If questions don't load, check `assets/` for the `.db` file and ensure `options` are properly `;` delimited.
*   **Progress**: Stored in `SharedPreferences` ("AnsweredQuestionIds") and local Room DB (`user_progress`).

---

## CI/CD Pipeline & Commit Flags

The project uses a GitHub Actions pipeline (`.github/workflows/android-ci.yml`) that responds to specific flags in commit messages.

| Flag | Behavior |
|------|----------|
| `[test]` | Runs all unit tests and generates a test report. |
| `[build]` | Forces the creation of APK artifacts (Debug and Release) on any branch. |
| `[ignore]` | Skips APK artifact creation (overrides all other conditions). |
| `[test][build]` | Runs tests AND creates APK artifacts. |

**Usage Notes:**
- Flags are case-insensitive (e.g., `[TEST]` works).
- By default, APKs are built automatically on `master` and `release/*` branches unless `[ignore]` is present.
- Use `[build]` when working on feature branches to verify the build and get downloadable APKs.
- Use `[test]` frequently to ensure code changes don't break existing logic.

