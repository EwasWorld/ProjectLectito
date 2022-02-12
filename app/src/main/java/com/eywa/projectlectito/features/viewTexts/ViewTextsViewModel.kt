package com.eywa.projectlectito.features.viewTexts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.database.texts.TextsRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class ViewTextsViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var db: LectitoRoomDatabase

    init {
        (application as App).appComponent.inject(this)
    }

    private val textsRepo = TextsRepo(db.textsDao())
    val allTexts = textsRepo.getAllTextsWithSnippetInfo()

    fun insert(textName: String): Job {
        require(textName.isNotBlank()) { "Text name cannot be blank" }

        return viewModelScope.launch {
            textsRepo.insert(Text(0, textName))
        }
    }
}