package com.znam.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znam.app.StatsUiState
import com.znam.app.StatsViewModel
import com.znam.app.ui.icons.AppIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.znam.app.data.CategoryStats
import com.znam.app.data.QuizSession
import kotlinx.coroutines.delay

// ── Color palette ──────────────────────────────────────────────────────

private val AccuracyGreen = Color(0xFF2E7D32)
private val AccuracyGreenLight = Color(0xFFC8E6C9)
private val CategoryBlue = Color(0xFF1565C0)
private val CategoryBlueBg = Color(0xFFBBDEFB)
private val TrophyGold = Color(0xFFF9A825)
private val TrophyGoldBg = Color(0xFFFFF9C4)
private val TimeIndigo = Color(0xFF283593)
private val TimeIndigoBg = Color(0xFFC5CAE9)
private val ChartColors = listOf(
    Color(0xFF1E88E5),  // Blue
    Color(0xFF43A047),  // Green
    Color(0xFFFB8C00),  // Orange
    Color(0xFFE53935)   // Red
)

// ── Main Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Статистика",  // TODO: localize
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Зареждане...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (!state.hasData) {
            EmptyStatsView(modifier = Modifier.padding(paddingValues))
        } else {
            StatsContent(
                state = state,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────

@Composable
private fun EmptyStatsView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = AppIcons.School,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Все още нямате завършени тестове",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Завършете поне един тест, за да видите статистиката си.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Main content ───────────────────────────────────────────────────────

@Composable
private fun StatsContent(
    state: StatsUiState,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Accuracy ring ──────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }
        ) {
            AccuracyRing(
                accuracy = state.overallAccuracy,
                totalCorrect = state.totalCorrectAnswers,
                totalQuestions = state.totalQuestionsAnswered
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Quick stats row ────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, 100)) + slideInVertically(tween(400, 100)) { -30 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(
                    icon = AppIcons.QuestionAnswer,
                    value = state.totalSessions.toString(),
                    label = "Тестове",
                    tint = CategoryBlue,
                    backgroundColor = CategoryBlueBg,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    icon = AppIcons.Schedule,
                    value = state.totalTimeFormatted,
                    label = "Общо време",
                    tint = TimeIndigo,
                    backgroundColor = TimeIndigoBg,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    icon = AppIcons.EmojiEvents,
                    value = state.bestScore.toString(),
                    label = "Рекорд",
                    tint = TrophyGold,
                    backgroundColor = TrophyGoldBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── This week card ─────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, 200)) + slideInVertically(tween(400, 200)) { -30 }
        ) {
            WeekActivityCard(sessionsThisWeek = state.sessionsThisWeek)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Category breakdown ─────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, 300)) + slideInVertically(tween(400, 300)) { -30 }
        ) {
            CategoryBreakdownCard(categories = state.categoryStats)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Recent sessions ────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, 400)) + slideInVertically(tween(400, 400)) { -30 }
        ) {
            RecentSessionsCard(sessions = state.recentSessions)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Accuracy Ring ──────────────────────────────────────────────────────

@Composable
private fun AccuracyRing(
    accuracy: Float,
    totalCorrect: Int,
    totalQuestions: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = accuracy / 100f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
        label = "accuracyAnim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Обща точност",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated circular progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val progressColor = when {
                    accuracy >= 80f -> AccuracyGreen
                    accuracy >= 60f -> TrophyGold
                    else -> Color(0xFFE53935)
                }

                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${accuracy.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalCorrect / $totalQuestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Stat Mini Card ─────────────────────────────────────────────────────

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Week Activity ──────────────────────────────────────────────────────

@Composable
private fun WeekActivityCard(sessionsThisWeek: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Тази седмица",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$sessionsThisWeek ${if (sessionsThisWeek == 1) "тест" else "теста"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── Category Breakdown ─────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(categories: List<CategoryStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "По категории",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "Няма данни",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categories.forEachIndexed { index, cat ->
                    CategoryRow(
                        stats = cat,
                        color = ChartColors[index % ChartColors.size]
                    )
                    if (index < categories.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(stats: CategoryStats, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = stats.accuracy / 100f,
        animationSpec = tween(800, 300),
        label = "catProgress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stats.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${stats.accuracy.toInt()}% (${stats.correct}/${stats.questions})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

// ── Recent Sessions ────────────────────────────────────────────────────

@Composable
private fun RecentSessionsCard(sessions: List<QuizSession>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Последни тестове",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Text(
                    text = "Няма данни",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                sessions.take(10).forEach { session ->
                    RecentSessionRow(session = session)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentSessionRow(session: QuizSession) {
    val categoryName = when (session.quizType) {
        "class8.db" -> "8 клас"
        "class9.db" -> "9 клас"
        "class10.db" -> "10 клас"
        "db_entrance_exam.db" -> "Матура"
        else -> session.quizType
    }

    val timeAgo = formatTimeAgo(session.timestamp)
    val accuracy = session.accuracyPercent.toInt()
    val accuracyColor = when {
        accuracy >= 80 -> AccuracyGreen
        accuracy >= 60 -> TrophyGold
        else -> Color(0xFFE53935)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = AppIcons.CheckCircle,
                contentDescription = null,
                tint = accuracyColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "${session.score}/${session.totalQuestions}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = accuracyColor
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────

private fun formatTimeAgo(timestampMillis: Long): String {
    val diffMs = System.currentTimeMillis() - timestampMillis
    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    return when {
        diffMinutes < 1 -> "Току-що"
        diffMinutes < 60 -> "Преди ${diffMinutes}мин"
        diffHours < 24 -> "Преди ${diffHours}ч"
        diffDays < 7 -> "Преди ${diffDays}дни"
        else -> {
            val formatter = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault())
            formatter.format(java.util.Date(timestampMillis))
        }
    }
}
