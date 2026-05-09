package com.znam.app

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
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

    // Stats — shared database across all quiz types
    single<com.znam.app.data.StatsDatabase> {
        get<DatabaseProvider>().getStatsDatabase()
    }

    single<com.znam.app.data.StatsDao> {
        get<com.znam.app.data.StatsDatabase>().statsDao()
    }

    // ViewModels
    viewModel {
        StatsViewModel(
            application = androidContext() as android.app.Application,
            statsDao = get()
        )
    }
    viewModel {
        QuizViewModel(
            application = androidContext() as android.app.Application,
            savedStateHandle = get(),
            statsDao = get()
        )
    }
}
