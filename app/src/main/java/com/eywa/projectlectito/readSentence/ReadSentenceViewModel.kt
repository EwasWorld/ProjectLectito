package com.eywa.projectlectito.readSentence

import android.app.Application
import androidx.lifecycle.*
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.TextsRepo
import com.eywa.projectlectito.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.wordDefinitions.WordDefinitionRequester
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
    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao())

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

    val selectedWord = MutableLiveData<String>()
    val currentDefinition = MutableLiveData<Int>()
    val definitions: LiveData<WordDefinitionsWithInfo> = object : MediatorLiveData<WordDefinitionsWithInfo>() {
        var currentRequester: WordDefinitionRequester? = null

        init {
            addSource(selectedWord.distinctUntilChanged()) { word ->
                if (word.isNullOrBlank()) return@addSource

                currentRequester?.cancelParse()
                /*
                 * `value =` does not work here:
                 * IllegalStateException: Cannot invoke setValue on a background thread
                 * This is presumably because requester.getDefinition() is changing the Dispatcher to IO
                 */
                val requester = WordDefinitionRequester(word, {
                    postValue(WordDefinitionsWithInfo(it))
                }, {
                    postValue(WordDefinitionsWithInfo(error = true))
                })
                currentRequester = requester
                currentDefinition.postValue(0)
                viewModelScope.launch {
                    requester.getDefinition()
                }
            }
        }
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

    data class WordDefinitionsWithInfo(
            val jishoWordDefinitions: JishoWordDefinitions? = null,
            val error: Boolean = false
    )
}