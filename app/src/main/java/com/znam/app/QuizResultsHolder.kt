package com.znam.app

object QuizResultsHolder {
    var score: Int = 0
    var questions: List<Question> = emptyList()
    var userAnswers: ArrayList<String> = arrayListOf()
    var elapsedTimeInSeconds: Int = 0

    fun clear() {
        score = 0
        questions = emptyList()
        userAnswers.clear()
        elapsedTimeInSeconds = 0
    }
}
