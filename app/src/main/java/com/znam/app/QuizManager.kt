package com.znam.app

class QuizManager(val questions: List<Question>) {
    private var currentQuestionIndex = 0
    var score = 0
        private set
    
    // Track answered questions to prevent duplicates in future sessions if we were persisting
    // For this session, we just track current state
    
    fun getCurrentQuestion(): Question? {
        if (currentQuestionIndex < questions.size && currentQuestionIndex < 15) {
            return questions[currentQuestionIndex]
        }
        return null
    }

    fun submitAnswer(selectedOption: String): Boolean {
        val currentQuestion = getCurrentQuestion() ?: return false
        val isCorrect = selectedOption.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true)
        
        if (isCorrect) {
            score++
        }
        
        return isCorrect
    }
    
    fun advanceToNextQuestion() {
        currentQuestionIndex++
    }
    
    fun isQuizFinished(): Boolean {
        return currentQuestionIndex >= 15 || currentQuestionIndex >= questions.size
    }
    
    fun getCurrentProgress(): String {
        return "${currentQuestionIndex + 1}/15"
    }

    fun getTotalQuestions(): Int {
        return if (questions.size > 15) 15 else questions.size
    }
    
    fun getCurrentIndex(): Int {
        return currentQuestionIndex
    }
}
