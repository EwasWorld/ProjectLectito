package com.eywa.projectlectito.app

import android.app.Application
import com.eywa.projectlectito.database.DatabaseDaggerModule
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

    // TODO Move these into their own module(s)?
    fun inject(viewModel: ReadSentenceViewModel)
    fun inject(viewModel: ViewTextsViewModel)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        // TODO Can't I just inject application into db dagger module?
        fun dbModule(databaseDaggerModule: DatabaseDaggerModule): Builder
        fun build(): AppComponent
    }
}
