package com.eywa.projectlectito.features.snippetBrowser

import android.app.Application
import androidx.lifecycle.*
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.texts.TextsRepo
import javax.inject.Inject

class SnippetBrowserViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var db: LectitoRoomDatabase

    init {
        (application as App).appComponent.inject(this)
    }

    /*
     * Repos
     */
    private val textsRepo = TextsRepo(db.textsDao())
    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao())

    /*
     * Data
     */
    val textId = MutableLiveData<Int?>(null)
    val textName = textId.switchMap { textId ->
        if (textId == null) return@switchMap MutableLiveData(null)
        textsRepo.getTextById(textId).map { it?.name }
    }

    val snippetsForText = textId.switchMap { textId ->
        if (textId == null) return@switchMap MutableLiveData(listOf())
        return@switchMap snippetsRepo.getSnippetsForText(textId)
    }
}