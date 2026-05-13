package com.znam.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.znam.app.R
import com.znam.app.AnswerFeedback
import com.znam.app.HintState
import com.znam.app.QuizEvent
import com.znam.app.QuizUiState
import com.znam.app.QuizViewModel
import androidx.compose.runtime.collectAsState
import com.znam.app.Question
import com.znam.app.ui.icons.AppIcons

import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Quiz-specific semantic colors  candidates for theme extraction in future Material3 migration
//  Color constants 

private val CorrectGreen = Color(0xFF2E7D32)
private val CorrectGreenBg = Color(0xFFC8E6C9)
private val IncorrectRed = Color(0xFFC62828)
private val IncorrectRedBg = Color(0xFFFFCDD2)
private val SelectedBlue = Color(0xFF1E88E5)
private val SelectedBlueBg = Color(0xFFBBDEFB)
private val HintTeal = Color(0xFF009688)
private val DefaultOptionBorder = Color(0xFFBFC8CA)

//  Main Screen 

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onNavigateToResults: (com.znam.app.QuizResults, com.znam.app.GamificationManager.GamificationResult?) -> Unit,
    onShowInterstitialAd: (com.znam.app.QuizResults, com.znam.app.GamificationManager.GamificationResult?) -> Unit,
    onNoQuestions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()

    // Handle one-shot events
    LaunchedEffect(event) {
        when (val e = event) {
            is QuizEvent.NavigateToResults -> {
                onNavigateToResults(e.results, e.gamificationResult)
                viewModel.consumeEvent()
            }
            is QuizEvent.ShowInterstitialAd -> {
                onShowInterstitialAd(e.results, e.gamificationResult)
                viewModel.consumeEvent()
            }
            is QuizEvent.NoQuestionsAvailable -> {
                onNoQuestions()
                viewModel.consumeEvent()
            }
            null -> {}
        }
    }

    var showKonfetti by remember { mutableStateOf(false) }

    // Trigger konfetti on perfect score navigation event
    LaunchedEffect(event) {
        when (val e = event) {
            is QuizEvent.NavigateToResults -> {
                if (e.results.score == e.results.questions.size) {
                    showKonfetti = true
                }
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            LoadingScreen()
        } else {
            QuizContent(
                state = uiState,
                onOptionSelected = viewModel::selectAnswer,
                onHintRequested = viewModel::requestHint
            )
        }

        // Konfetti celebration for perfect score
        if (showKonfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(80),
                        position = Position.Relative(0.5, 0.0)
                    )
                )
            )
        }
    }
}

//  Loading 

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.loading_dna)
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

//  Quiz Content 

@Composable
private fun QuizContent(
    state: QuizUiState,
    onOptionSelected: (Int) -> Unit,
    onHintRequested: () -> Unit
) {
    Scaffold(
        bottomBar = {
            QuizBottomBar(
                scoreText = state.scoreText,
                timerText = state.timerText
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            //  Header: Counter + Hint button 
            QuizHeader(
                counterText = state.questionCounterText,
                progress = (state.currentQuestionIndex + 1).toFloat() / state.totalQuestions,
                canShowHint = state.canRequestHint,
                onHintRequested = onHintRequested
            )

            Spacer(modifier = Modifier.height(8.dp))

            //  Hint bubbles 
            HintBubbles(hintState = state.hintState)

            Spacer(modifier = Modifier.height(16.dp))

            //  Question card 
            QuestionCard(questionText = state.question?.questionText ?: "")

            Spacer(modifier = Modifier.height(24.dp))

            //  Option buttons 
            state.parsedOptions.forEachIndexed { index, optionText ->
                if (optionText.isNotBlank()) {
                    OptionButton(
                        text = optionText,
                        index = index,
                        feedback = state.answerFeedback,
                        isAnswered = state.isAnswered,
                        onSelected = { onOptionSelected(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

//  Header 

@Composable
private fun QuizHeader(
    counterText: String,
    progress: Float,
    canShowHint: Boolean,
    onHintRequested: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = counterText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Hint button
        Surface(
            onClick = onHintRequested,
            enabled = canShowHint,
            shape = CircleShape,
            color = if (canShowHint)
                HintTeal.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                2.dp,
                if (canShowHint) HintTeal else DefaultOptionBorder.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .size(48.dp)
                .then(if (!canShowHint) Modifier.alpha(0.4f) else Modifier)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = AppIcons.Lightbulb,
                    contentDescription = stringResource(R.string.hint_content_description),
                    tint = if (canShowHint) HintTeal else DefaultOptionBorder.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

//  Hint Bubbles 

@Composable
private fun HintBubbles(hintState: HintState) {
    AnimatedVisibility(
        visible = hintState.hint1Visible || hintState.hint2Visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (hintState.hint1Visible && hintState.hint1Text != null) {
                HintBubble(text = hintState.hint1Text, index = 1)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (hintState.hint2Visible && hintState.hint2Text != null) {
                HintBubble(text = hintState.hint2Text, index = 2)
            }
        }
    }
}

@Composable
private fun HintBubble(text: String, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HintTeal.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, HintTeal.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = AppIcons.Lightbulb,
                contentDescription = null,
                tint = HintTeal,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

//  Question Card 

@Composable
private fun QuestionCard(questionText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = questionText,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

//  Option Button 

@Composable
private fun OptionButton(
    text: String,
    index: Int,
    feedback: AnswerFeedback?,
    isAnswered: Boolean,
    onSelected: () -> Unit
) {
    val (backgroundColor, borderColor, textColor) = resolveOptionColors(
        index = index,
        feedback = feedback,
        isAnswered = isAnswered
    )

    val animatedBg by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(300),
        label = "optionBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = borderColor,
        animationSpec = tween(300),
        label = "optionBorder"
    )

    // Subtle scale pulse on correct answer
    val scale by animateFloatAsState(
        targetValue = if (feedback != null && feedback.correctOption == index && feedback.isCorrect) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "optionScale"
    )

    OutlinedButton(
        onClick = onSelected,
        enabled = !isAnswered,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (feedback != null && (feedback.selectedOption == index || feedback.correctOption == index)) 2.dp else 1.dp,
            color = animatedBorder
        ),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = animatedBg,
            disabledContainerColor = animatedBg
        )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            textAlign = TextAlign.Start
        )
    }
}

private data class OptionColors(
    val background: Color,
    val border: Color,
    val text: Color
)

private fun resolveOptionColors(
    index: Int,
    feedback: AnswerFeedback?,
    isAnswered: Boolean
): OptionColors {
    if (feedback == null) {
        // Default state  not yet answered
        return OptionColors(
            background = Color.White,
            border = DefaultOptionBorder,
            text = Color.Black
        )
    }

    return when {
        // This is the correct answer
        feedback.correctOption == index -> OptionColors(
            background = CorrectGreenBg,
            border = CorrectGreen,
            text = CorrectGreen
        )
        // This is the selected but wrong answer
        feedback.selectedOption == index && !feedback.isCorrect -> OptionColors(
            background = IncorrectRedBg,
            border = IncorrectRed,
            text = IncorrectRed
        )
        // Unrelated option after answer
        else -> OptionColors(
            background = Color.White.copy(alpha = 0.5f),
            border = DefaultOptionBorder.copy(alpha = 0.5f),
            text = Color.Black.copy(alpha = 0.5f)
        )
    }
}

//  Bottom Bar 

@Composable
private fun QuizBottomBar(
    scoreText: String,
    timerText: String
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.score_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Timer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.time_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


//  Previews 

@Preview(showBackground = true)
@Composable
private fun QuizScreenLoadingPreview() {
    LoadingScreen()
}

@Preview(showBackground = true)
@Composable
private fun QuizScreenQuestionPreview() {
    QuizContent(
        state = previewQuizState(),
        onOptionSelected = {},
        onHintRequested = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun QuizScreenCorrectAnswerPreview() {
    QuizContent(
        state = previewQuizState(
            answerFeedback = AnswerFeedback(
                selectedOption = 0,
                correctOption = 0,
                isCorrect = true
            ),
            score = 1
        ),
        onOptionSelected = {},
        onHintRequested = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun QuizScreenIncorrectAnswerWithHintsPreview() {
    QuizContent(
        state = previewQuizState(
            answerFeedback = AnswerFeedback(
                selectedOption = 1,
                correctOption = 0,
                isCorrect = false
            ),
            hintState = HintState(
                hint1Text = "     .",
                hint2Text = "   .",
                hint1Visible = true,
                hint2Visible = true,
                canShowMore = false
            )
        ),
        onOptionSelected = {},
        onHintRequested = {}
    )
}

private fun previewQuizState(
    answerFeedback: AnswerFeedback? = null,
    score: Int = 0,
    hintState: HintState = HintState(
        hint1Text = "     .",
        hint1Visible = false,
        canShowMore = true
    )
): QuizUiState {
    val question = Question(
        id = 1,
        questionText = "         ?",
        options = ";;;",
        correctAnswer = "",
        hint1 = "     .",
        hint2 = "   ."
    )
    return QuizUiState(
        isLoading = false,
        quizType = "class8.db",
        currentQuestionIndex = 0,
        totalQuestions = 15,
        question = question,
        parsedOptions = question.getParsedOptions(),
        score = score,
        elapsedSeconds = 73,
        hintState = hintState,
        answerFeedback = answerFeedback
    )
}
