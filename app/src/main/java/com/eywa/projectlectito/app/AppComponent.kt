package com.eywa.projectlectito.app

import android.app.Application
import com.eywa.projectlectito.addSnippet.AddSnippetViewModel
import com.eywa.projectlectito.database.DatabaseDaggerModule
import com.eywa.projectlectito.editSnippet.EditSnippetViewModel
import com.eywa.projectlectito.readSentence.ReadSentenceViewModel
import com.eywa.projectlectito.viewTexts.ViewTextsViewModel
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

/**
 * Provides all the objects that can be instantiated from dependency injection
 */
@Singleton
@Component(
        dependencies = [],
        modules = [
            AndroidInjectionModule::class, DatabaseDaggerModule::class
        ]
)
interface AppComponent {
    fun inject(app: App)

    // TODO CLEANUP Move these into their own module(s)?
    fun inject(viewModel: ReadSentenceViewModel)
    fun inject(viewModel: ViewTextsViewModel)
    fun inject(viewModel: AddSnippetViewModel)
    fun inject(viewModel: EditSnippetViewModel)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        // TODO CLEANUP Can't I just inject application into db dagger module?
        fun dbModule(databaseDaggerModule: DatabaseDaggerModule): Builder
        fun build(): AppComponent
    }
}
