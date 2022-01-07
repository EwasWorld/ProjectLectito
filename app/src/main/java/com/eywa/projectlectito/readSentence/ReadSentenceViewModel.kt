package com.eywa.projectlectito.readSentence

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.eywa.projectlectito.JishoReturnData
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.TextsRepo
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import javax.inject.Inject

class ReadSentenceViewModel(application: Application) : AndroidViewModel(application) {
    private val LOG_TAG = "ECH TEST"

    @Inject
    lateinit var db: LectitoRoomDatabase

    init {
        (application as App).appComponent.inject(this)
    }

    private val textsRepo = TextsRepo(db.textsDao())
    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao(), db.parsedInfoDao())

    val allSnippets = snippetsRepo.allSnippets

    val textSnippetId = MutableLiveData<Int?>(null)
    val textSnippet: LiveData<TextSnippet> = textSnippetId.switchMap { id ->
        if (id != null) snippetsRepo.getTextSnippetById(id) else MutableLiveData()
    }.distinctUntilChanged()
    val currentCharacter = MutableLiveData<Int>()

    val textName = textSnippet.switchMap { snippet ->
        textsRepo.getTextById(snippet.textId).map { it.name }
    }.distinctUntilChanged()

    val definitions: LiveData<JishoReturnData>
    val currentDefinition = MutableLiveData<Int>()

    init {
        // TODO Build this somewhere else
        var finalData: JishoReturnData? = null
        try {
            finalData = GsonBuilder().create().fromJson(TempTestData.umakuDefinition, JishoReturnData::class.java)
            if (finalData == null) {
                Log.e(LOG_TAG, "Error parsing dataset")
            }
        }
        catch (e: JsonSyntaxException) {
            Log.e(LOG_TAG, "Invalid JSON format")
            Log.e(LOG_TAG, e.message ?: "No message on exception")
        }
        definitions = MutableLiveData(finalData)
    }
}