# Smart Hints Feature - Implementation Instructions

## Overview
Add a hint system to the quiz screen that allows users to request up to 2 progressive hints per question without affecting their score.

---

## Database Changes

### Step 1: Add hint columns to all database files

Run these SQL commands on each database file:
- `app/src/main/assets/class8.db`
- `app/src/main/assets/class9.db`
- `app/src/main/assets/class10.db`
- `app/src/main/assets/db_entrance_exam.db`

```sql
ALTER TABLE questions ADD COLUMN hint1 TEXT;
ALTER TABLE questions ADD COLUMN hint2 TEXT;
```

### Step 2: Update the Question data class

**File:** `app/src/main/java/com/znam/app/Question.kt`

Add hint fields to the entity:

```kotlin
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val questionText: String,
    val options: String,
    val correctAnswer: String,
    val hint1: String? = null,
    val hint2: String? = null
) : Parcelable {
    // Update Parcelable implementation to include hint1 and hint2
    
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),  // hint1 (nullable)
        parcel.readString()   // hint2 (nullable)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(questionText)
        parcel.writeString(options)
        parcel.writeString(correctAnswer)
        parcel.writeString(hint1)
        parcel.writeString(hint2)
    }
    
    // Helper to check if hints are available
    fun hasHints(): Boolean = !hint1.isNullOrBlank()
}
```

### Step 3: Increment database version

**File:** `app/src/main/java/com/znam/app/AppDatabase.kt`

Increment the version number and handle migration (or use fallbackToDestructiveMigration for development).

---

## UI Implementation

### Hint Button Design

**Location:** Top right corner of quiz screen, above the question card

**Specifications:**
| Property | Value |
|----------|-------|
| Shape | Circle |
| Size | 48dp diameter |
| Background | White (#FFFFFF) |
| Border | 2dp teal (#009688) |
| Icon | Question mark (?) centered |
| Icon color | Teal (#009688) |
| Label | "Hint" below the circle |
| Label color | Teal (#009688) |
| Label size | 12sp |

**States:**
| State | Appearance |
|-------|------------|
| Default (hints available) | Full color, clickable |
| Disabled (no hints or all used) | Grayed out (#BDBDBD), not clickable |

### Hint Bubble Design

**Style:** Speech bubble with triangle pointer

**Specifications:**
| Property | Value |
|----------|-------|
| Background | White (#FFFFFF) |
| Border | 2dp teal (#009688) |
| Corner radius | 12dp |
| Padding | 12dp |
| Pointer | Triangle pointing right toward hint button |
| Pointer size | 10dp |
| Max width | 250dp (expands vertically for long text) |
| Shadow/Elevation | 4dp |

**Text styling:**
| Property | Value |
|----------|-------|
| Color | Teal (#009688) |
| Size | 14sp (medium) |
| Style | Plain text, no labels or numbering |

### Bubble Positioning

```
+------------------------------------------+
|                              [?]  <- Hint Button (top right)
|                               Hint
|                                 
|  +------------------+ ◄──────────┘
|  | Hint 1 text here |     (pointer points to button)
|  +------------------+
|  +------------------+
|  | Hint 2 text here |     (appears below Hint 1)
|  +------------------+
|                                          
|  +----------------------------------+    
|  |         Question Card            |    
|  +----------------------------------+    
|                                          
|  [  Answer Option 1  ]                   
|  [  Answer Option 2  ]                   
|  [  Answer Option 3  ]                   
|  [  Answer Option 4  ]                   
+------------------------------------------+
```

- **Hint 1:** Appears to the LEFT of the hint button, pointer points right
- **Hint 2:** Appears BELOW Hint 1, pointer also points toward button area
- **Constraint:** Bubbles must NOT overlap the question card

---

## Animation

### Bubble Appearance Animation

**Type:** Pop/scale animation

**Implementation:**
```kotlin
// Scale animation: starts at 0, expands to 100%
val scaleX = ObjectAnimator.ofFloat(bubbleView, "scaleX", 0f, 1f)
val scaleY = ObjectAnimator.ofFloat(bubbleView, "scaleY", 0f, 1f)
val alpha = ObjectAnimator.ofFloat(bubbleView, "alpha", 0f, 1f)

AnimatorSet().apply {
    playTogether(scaleX, scaleY, alpha)
    duration = 200 // milliseconds
    interpolator = OvershootInterpolator(1.2f) // slight bounce effect
    start()
}
```

**Pivot point:** Set to the pointer location (right edge for Hint 1) so it appears to "pop out" from the button.

---

## Behavior Logic

### State Management

Track per-question hint state in the Quiz Activity/Fragment:

```kotlin
// Add to quiz state
private var hintsShown: Int = 0  // 0, 1, or 2

// Reset when moving to next question
private fun loadNextQuestion() {
    hintsShown = 0
    hideAllHintBubbles()
    updateHintButtonState()
    // ... existing question loading logic
}
```

### Hint Button Click Handler

```kotlin
hintButton.setOnClickListener {
    val currentQuestion = getCurrentQuestion()
    
    when (hintsShown) {
        0 -> {
            if (!currentQuestion.hint1.isNullOrBlank()) {
                showHintBubble(1, currentQuestion.hint1)
                hintsShown = 1
                updateHintButtonState()
            }
        }
        1 -> {
            if (!currentQuestion.hint2.isNullOrBlank()) {
                showHintBubble(2, currentQuestion.hint2)
                hintsShown = 2
                updateHintButtonState()
            }
        }
        2 -> {
            // Do nothing - button should already be disabled
        }
    }
}
```

### Button State Logic

```kotlin
private fun updateHintButtonState() {
    val currentQuestion = getCurrentQuestion()
    
    val shouldBeEnabled = when {
        !currentQuestion.hasHints() -> false  // No hints in DB
        hintsShown >= 2 -> false              // All hints shown
        hintsShown == 1 && currentQuestion.hint2.isNullOrBlank() -> false  // Only 1 hint exists
        else -> true
    }
    
    hintButton.isEnabled = shouldBeEnabled
    hintButton.alpha = if (shouldBeEnabled) 1.0f else 0.5f
    // Also gray out the icon and label if disabled
}
```

### Answer Selection Behavior

```kotlin
private fun onAnswerSelected(selectedAnswer: String) {
    // ... existing answer validation logic
    
    // Hint bubbles STAY VISIBLE during 1.5s feedback delay
    // Do NOT hide them here
    
    // After 1.5s delay, when advancing to next question:
    handler.postDelayed({
        loadNextQuestion()  // This resets hints
    }, 1500)
}
```

---

## XML Layout Changes

### Quiz Activity Layout

Add hint button and bubble containers above the question card:

```xml
<!-- Hint Button (top right) -->
<LinearLayout
    android:id="@+id/hintButtonContainer"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    android:layout_marginTop="16dp"
    android:layout_marginEnd="16dp">

    <com.google.android.material.card.MaterialCardView
        android:id="@+id/hintButton"
        android:layout_width="48dp"
        android:layout_height="48dp"
        app:cardCornerRadius="24dp"
        app:cardBackgroundColor="#FFFFFF"
        app:strokeColor="#009688"
        app:strokeWidth="2dp"
        app:cardElevation="4dp"
        android:clickable="true"
        android:focusable="true">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:text="?"
            android:textColor="#009688"
            android:textSize="24sp"
            android:textStyle="bold"
            android:gravity="center"/>
    </com.google.android.material.card.MaterialCardView>

    <TextView
        android:id="@+id/hintLabel"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hint"
        android:textColor="#009688"
        android:textSize="12sp"
        android:layout_marginTop="4dp"/>
</LinearLayout>

<!-- Hint Bubbles Container (positioned to left of button) -->
<LinearLayout
    android:id="@+id/hintBubblesContainer"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    app:layout_constraintTop_toTopOf="@id/hintButtonContainer"
    app:layout_constraintEnd_toStartOf="@id/hintButtonContainer"
    android:layout_marginEnd="8dp"
    android:visibility="gone">

    <!-- Hint 1 Bubble -->
    <include
        android:id="@+id/hint1Bubble"
        layout="@layout/hint_bubble"
        android:visibility="gone"/>

    <!-- Hint 2 Bubble -->
    <include
        android:id="@+id/hint2Bubble"
        layout="@layout/hint_bubble"
        android:layout_marginTop="8dp"
        android:visibility="gone"/>
</LinearLayout>
```

### Hint Bubble Layout (hint_bubble.xml)

Create new layout file `res/layout/hint_bubble.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">

    <com.google.android.material.card.MaterialCardView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:maxWidth="250dp"
        app:cardCornerRadius="12dp"
        app:cardBackgroundColor="#FFFFFF"
        app:strokeColor="#009688"
        app:strokeWidth="2dp"
        app:cardElevation="4dp"
        android:layout_marginEnd="10dp">

        <TextView
            android:id="@+id/hintText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:padding="12dp"
            android:textColor="#009688"
            android:textSize="14sp"
            android:maxWidth="230dp"/>
    </com.google.android.material.card.MaterialCardView>

    <!-- Triangle pointer (pointing right) -->
    <ImageView
        android:layout_width="10dp"
        android:layout_height="16dp"
        android:layout_gravity="center_vertical|end"
        android:src="@drawable/hint_pointer"
        app:tint="#009688"/>
</FrameLayout>
```

### Pointer Drawable (hint_pointer.xml)

Create `res/drawable/hint_pointer.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="10dp"
    android:height="16dp"
    android:viewportWidth="10"
    android:viewportHeight="16">
    
    <!-- Triangle pointing right -->
    <path
        android:pathData="M0,0 L10,8 L0,16 Z"
        android:fillColor="#009688"/>
</vector>
```

---

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| Question has no hints (hint1 is null/empty) | Hint button is disabled/grayed from start |
| Question has only hint1 (hint2 is null/empty) | Button disables after showing hint1 |
| User answers before requesting hints | Normal flow, hints not shown |
| User answers after seeing 1 hint | Hint 1 stays visible during feedback |
| User answers after seeing 2 hints | Both hints stay visible during feedback |
| Very long hint text | Bubble expands vertically, Hint 2 pushed lower |

---

## Testing Checklist

- [ ] Hint button appears on quiz screen
- [ ] Button disabled when question has no hints
- [ ] First tap shows Hint 1 with pop animation
- [ ] Second tap shows Hint 2 below Hint 1
- [ ] Third tap does nothing (button disabled)
- [ ] Bubbles positioned correctly (left of button, not overlapping question)
- [ ] Speech bubble pointer points toward button
- [ ] Text is teal color, readable
- [ ] Bubbles stay visible during 1.5s answer feedback
- [ ] Bubbles reset when advancing to next question
- [ ] Score is NOT affected by hint usage
- [ ] Long hint text expands bubble vertically
- [ ] Works across all database files (class8, class9, class10, entrance exam)

---

## Files to Create/Modify

**New files:**
- `res/layout/hint_bubble.xml`
- `res/drawable/hint_pointer.xml`

**Modified files:**
- `Question.kt` - Add hint1, hint2 fields
- `AppDatabase.kt` - Increment version
- Quiz Activity layout XML - Add hint button and containers
- Quiz Activity Kotlin - Add hint logic and state management
- All 4 database files - ALTER TABLE to add columns
