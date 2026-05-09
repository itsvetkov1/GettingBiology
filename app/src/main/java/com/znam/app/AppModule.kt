package com.znam.app

import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {
    single { DatabaseProvider(androidContext()) }

    factory<AppDatabase> { (quizType: String) ->
        get<DatabaseProvider>().createDatabase(quizType)
    }

    factory<QuestionDao> { (quizType: String) ->
        get<AppDatabase> { parametersOf(quizType) }.questionDao()
    }

    factory<UserProgressDao> { (quizType: String) ->
        get<AppDatabase> { parametersOf(quizType) }.userProgressDao()
    }
}
