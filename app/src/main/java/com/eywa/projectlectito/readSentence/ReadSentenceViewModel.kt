package com.eywa.projectlectito.readSentence

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.eywa.projectlectito.JishoReturnData
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.ParsedInfo
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.TextsRepo
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO Don't expose mutable data
class ReadSentenceViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val LOG_TAG = "ReadSentenceViewModel"
    }

    @Inject
    lateinit var db: LectitoRoomDatabase

    init {
        (application as App).appComponent.inject(this)
    }

    private val textsRepo = TextsRepo(db.textsDao())
    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao(), db.parsedInfoDao())

    val textSnippetId = MutableLiveData<Int?>(null)
    val textSnippet = textSnippetId.switchMap { id ->
        if (id != null) snippetsRepo.getTextSnippetById(id) else MutableLiveData()
    }.distinctUntilChanged()
    val currentCharacter = MutableLiveData<Int>()
    val sentence: LiveData<SentenceWithInfo> =
            SentenceMediatorLiveData(textSnippet, currentCharacter.distinctUntilChanged())

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

    private inner class SentenceMediatorLiveData(
            private val textSnippet: LiveData<TextSnippet>,
            private val currentCharacter: LiveData<Int>
    ) : MediatorLiveData<SentenceWithInfo>() {
        init {
            addSource(textSnippet) {
                generateNewSentence()
            }
            addSource(currentCharacter) {
                generateNewSentence()
            }
        }

        private fun generateNewSentence() {
            value?.sentence?.cancelParse()
            var sentence: Sentence? = null
            sentence = Sentence(
                    textSnippet.value?.content,
                    currentCharacter.value,
                    {
                        value = SentenceWithInfo(sentence!!, it)
                    },
                    {
                        value = SentenceWithInfo(sentence!!, parseError = true)
                    }
            )
            value = SentenceWithInfo(sentence)
            viewModelScope.launch {
                sentence.startParse()
            }
        }
    }

    data class SentenceWithInfo(
            val sentence: Sentence,
            val parsedInfo: List<ParsedInfo>? = null,
            val parseError: Boolean = false
    )
}