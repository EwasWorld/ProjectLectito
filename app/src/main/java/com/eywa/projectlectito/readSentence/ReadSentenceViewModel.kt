package com.eywa.projectlectito.readSentence

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.eywa.projectlectito.R
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.TextsRepo
import com.eywa.projectlectito.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.wordDefinitions.WordDefinitionRequester
import kotlinx.android.synthetic.main.read_sentence_fragment.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO Don't expose mutable data
class ReadSentenceViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val LOG_TAG = "ReadSentenceViewModel"

        private const val SURROUNDING_SNIPPETS_TO_RETRIEVE = 1
    }

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
    val allSnippets = snippetsRepo.allSnippets

    /*
     * Snippets info
     */
    val textSnippetId = MutableLiveData<Int?>(null)
    private val currentCharacter = MutableLiveData<Int>(329)
    private val currentSnippet: LiveData<TextSnippet?> = textSnippetId.switchMap { id ->
        if (id == null) return@switchMap MutableLiveData(null)
        snippetsRepo.getTextSnippetById(id) as LiveData<TextSnippet?>
    }.distinctUntilChanged()
    private val previousSnippets = currentSnippet.switchMap { currentSnippet ->
        if (currentSnippet == null) return@switchMap MutableLiveData(listOf())
        snippetsRepo.getPreviousSnippets(currentSnippet, SURROUNDING_SNIPPETS_TO_RETRIEVE)
    }
    private val nextSnippets = currentSnippet.switchMap { currentSnippet ->
        if (currentSnippet == null) return@switchMap MutableLiveData(listOf())
        snippetsRepo.getNextSnippets(currentSnippet, SURROUNDING_SNIPPETS_TO_RETRIEVE)
    }

    /*
     * Current sentence info
     */
    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
    val wordSelectMode = MutableLiveData<WordSelectMode>(WordSelectMode.AUTO)
    val sentence: LiveData<SentenceWithInfo?> =
            SentenceMediatorLiveData(
                    currentSnippet,
                    previousSnippets,
                    nextSnippets,
                    currentCharacter.distinctUntilChanged(),
                    wordSelectMode
            )
    val currentSnippetInfo = currentSnippet.map { snippet ->
        if (snippet == null) return@map ""
        val chapter = if (snippet.chapterId != null) "第%d章".format(snippet.chapterId) else ""
        val page = "%dページ".format(snippet.pageReference)
        return@map "%s%s".format(chapter, page)
    }
    val textName = currentSnippet.switchMap { snippet ->
        if (snippet == null) return@switchMap MutableLiveData("")
        textsRepo.getTextById(snippet.textId).map { it.name }
    }.distinctUntilChanged()

    /*
     * Word definitions
     */
    val selectedWord = MutableLiveData<String?>()
    val currentDefinition = MutableLiveData<Int>()
    val definitions: LiveData<WordDefinitionsWithInfo?> = object : MediatorLiveData<WordDefinitionsWithInfo?>() {
        var currentRequester: WordDefinitionRequester? = null

        init {
            addSource(selectedWord.distinctUntilChanged()) { word ->
                if (word.isNullOrBlank()) {
                    postValue(null)
                    return@addSource
                }

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

    fun updateCurrentCharacter(indexInfo: Sentence.IndexInfo): Boolean {
        val snippet = when (indexInfo.snippet) {
            Sentence.SnippetLocation.PREVIOUS -> previousSnippets.value?.lastOrNull()
            Sentence.SnippetLocation.CURRENT -> currentSnippet.value
            Sentence.SnippetLocation.NEXT -> nextSnippets.value?.firstOrNull()
        } ?: return false
        currentCharacter.postValue(indexInfo.index)
        textSnippetId.postValue(snippet.id)
        return true
    }

    private inner class SentenceMediatorLiveData(
            private val currentSnippet: LiveData<TextSnippet?>,
            private val previousSnippets: LiveData<List<TextSnippet>>,
            private val nextSnippets: LiveData<List<TextSnippet>>,
            private val currentCharacter: LiveData<Int>,
            private val wordSelectMode: LiveData<WordSelectMode>
    ) : MediatorLiveData<SentenceWithInfo?>() {
        init {
            addSource(currentSnippet) {
                generateNewSentence()
            }
            addSource(previousSnippets) {
                generateNewSentence()
            }
            addSource(nextSnippets) {
                generateNewSentence()
            }
            // Previous and next will only change when textSnippet changes
            addSource(currentCharacter) {
                generateNewSentence()
            }
            addSource(wordSelectMode) { newWordSelectMode ->
                if (value == null) {
                    return@addSource
                }

                if (newWordSelectMode == WordSelectMode.AUTO && value?.parseError != true && value?.parsedInfo == null) {
                    viewModelScope.launch {
                        value?.sentence?.startParse()
                    }
                    return@addSource
                }
            }
        }

        private fun generateNewSentence() {
            value?.sentence?.cancelParse()
            var sentence: Sentence? = null
            try {
                sentence = Sentence(
                        currentSnippet.value?.content,
                        currentCharacter.value,
                        previousSnippets.value?.lastOrNull()?.content,
                        nextSnippets.value?.firstOrNull()?.content,
                        parserSuccessCallback = {
                            value = SentenceWithInfo(sentence!!, it)
                        },
                        parserFailCallback = {
                            value = SentenceWithInfo(sentence!!, parseError = true)
                        }
                )
            }
            catch (e: Exception) {
                value = null
                return
            }
            Log.d(LOG_TAG, "Current sentence start index: ${sentence.currentSentenceStart.index}")
            value = SentenceWithInfo(sentence)
            if (wordSelectMode.value == WordSelectMode.AUTO) {
                viewModelScope.launch {
                    sentence.startParse()
                }
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

    enum class WordSelectMode(val iconId: Int, val iconDescriptionId: Int) {
        AUTO(R.drawable.ic_auto_fix, R.string.read_sentence__select_mode_auto),
        SELECT(R.drawable.ic_touch, R.string.read_sentence__select_mode_select),
        TYPE(R.drawable.ic_text_fields, R.string.read_sentence__select_mode_type)
    }
}