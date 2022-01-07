package com.eywa.projectlectito.app

import android.app.Application
import com.eywa.projectlectito.database.DatabaseDaggerModule
import com.eywa.projectlectito.readSentence.ReadSentenceViewModel
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

    fun inject(viewModel: ReadSentenceViewModel)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun dbModule(databaseDaggerModule: DatabaseDaggerModule): Builder
        fun build(): AppComponent
    }
}
