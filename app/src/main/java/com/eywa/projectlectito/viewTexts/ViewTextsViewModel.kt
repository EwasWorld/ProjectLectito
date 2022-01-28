package com.eywa.projectlectito.viewTexts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.texts.TextsRepo
import javax.inject.Inject

class ViewTextsViewModel(application: Application) : AndroidViewModel(application) {
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
     * Text info
     */
    val allTexts = textsRepo.getAllTextsWithSnippetInfo()
}