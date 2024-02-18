package com.znam.app

object QuizResultsHolder {
    var score: Int = 0
    var questions: List<Question> = emptyList()
    var userAnswers: ArrayList<String> = arrayListOf()

    fun clear() {
        score = 0
        questions = emptyList()
        userAnswers.clear()
    }
}
