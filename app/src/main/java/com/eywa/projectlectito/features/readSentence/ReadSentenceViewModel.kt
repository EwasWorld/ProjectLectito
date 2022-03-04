package com.eywa.projectlectito.features.readSentence

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ReadSentenceViewModel(application: Application) : AndroidViewModel(application) {
    //    companion object {
//        private const val LOG_TAG = "ReadSentenceViewModel"
//
//        private const val SURROUNDING_SNIPPETS_TO_RETRIEVE = 1
//    }
//
//    @Inject
//    lateinit var db: LectitoRoomDatabase
//
//    init {
//        (application as App).appComponent.inject(this)
//    }
//
//    private val textsRepo = TextsRepo(db.textsDao(), db.textSnippetsDao())
//    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao())
//
//    /*
//     * Word select mode - how to choose words to look up in the dictionary
//     */
//    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
//    private val _isWordSelectModeMenuOpen = MutableLiveData<Boolean>(false)
//    val isWordSelectModeMenuOpen = _isWordSelectModeMenuOpen.distinctUntilChanged()
//
//    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
//    private val _wordSelectMode = MutableLiveData<WordSelectMode>(WordSelectMode.SELECT)
//    val wordSelectMode: LiveData<WordSelectMode> = _wordSelectMode.distinctUntilChanged()
//
//    /*
//     * Current Snippet/Text/Sentence
//     */
//    fun getFirstSnippetForText(textId: Int) = snippetsRepo.getFirstSnippet(textId)
//    private val currentSnippetId = MutableLiveData<SnippetIdAndChar?>(null)
//    private val currentSnippet = currentSnippetId.distinctUntilChanged().switchMap {
//        Log.i(LOG_TAG, "Current ID: ${it?.snippetId}")
//        val currentChar = it?.currentCharacter ?: 0
//        val id = it?.snippetId ?: return@switchMap MutableLiveData(null)
//        snippetsRepo.getTextSnippetById(id).map { snippet ->
//            if (snippet == null) return@map null
//            FullSnippetAndChar(snippet, currentChar)
//        }
//    }
//    private val text: LiveData<Text?> = currentSnippet.map { it?.snippet?.textId }.distinctUntilChanged()
//            .switchMap { id ->
//                if (id == null) return@switchMap MutableLiveData(null)
//                textsRepo.getTextById(id)
//            }
//    private val sentenceWithParsedInfo = SentenceMediatorLiveData(currentSnippet, wordSelectMode, text)
//    val sentence = sentenceWithParsedInfo.map { it?.sentence }
//
//    /*
//     * Edit Snippet
//     */
//    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
//    private val isInSelectSnippetToEditMode = MutableLiveData<Boolean>(false)
//    private val _snippetToEdit = MutableLiveData<SnippetIdWithStartAndEnd?>(null)
//    val snippetToEdit: LiveData<SnippetIdWithStartAndEnd?> = _snippetToEdit.distinctUntilChanged()
//
//    /*
//     * Word definitions
//     */
//    private val tempSelectedWord = MutableLiveData<TempSelectedWord?>(null)
//    val simpleTempSelectedWord = tempSelectedWord.distinctUntilChanged().map {
//        if (it !is TempSelectedWord.SimpleWord) return@map null
//        if (it.word.isBlank()) null else it.word
//    }
//    private val searchWord = MutableLiveData<String?>(null)
//
//    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
//    private val currentDefinitionIndex = MutableLiveData<Int>(0)
//    private val allDefinitions = object : MediatorLiveData<WordDefinitionsWithErrorStatus?>() {
//        var currentRequester: WordDefinitionRequester? = null
//
//        init {
//            addSource(searchWord.distinctUntilChanged()) { word ->
//                if (word.isNullOrBlank()) {
//                    postValue(null)
//                    return@addSource
//                }
//
//                currentRequester?.cancelParse()
//                /*
//                 * `value =` does not work here:
//                 * IllegalStateException: Cannot invoke setValue on a background thread
//                 * This is presumably because requester.getDefinition() is changing the Dispatcher to IO
//                 */
//                val requester = WordDefinitionRequester(word, {
//                    postValue(WordDefinitionsWithErrorStatus(it))
//                }, {
//                    postValue(WordDefinitionsWithErrorStatus(error = true))
//                })
//                currentRequester = requester
//                currentDefinitionIndex.postValue(0)
//                viewModelScope.launch {
//                    requester.getDefinition()
//                }
//            }
//        }
//    }
//
//    /*
//     * Update functions
//     */
//
//    fun updateWordSelectMode(wordSelectMode: WordSelectMode) {
//        _wordSelectMode.postValue(wordSelectMode)
//        tempSelectedWord.postValue(null)
//        searchWord.postValue(null)
//        currentDefinitionIndex.postValue(0)
//    }
//
//    fun updateCurrentSnippetAndChar(indexInfo: Sentence.IndexInfo): Boolean {
//        Log.i(LOG_TAG, "Set snippet: ${indexInfo.textSnippetId}")
//        currentSnippetId.postValue(SnippetIdAndChar(indexInfo.textSnippetId, indexInfo.startIndex))
//        Log.i(LOG_TAG, "Set snippet complete: ${indexInfo.textSnippetId}")
//        return true
//    }
//
//    fun incrementCurrentDefinitionIndex(): Boolean {
//        val definitions = allDefinitions.value
//        val currentDefinition = currentDefinitionIndex.value ?: 0
//
//        if (definitions?.error == true) {
//            return false
//        }
//
//        val totalDefinitions = definitions?.jishoWordDefinitions?.data?.size
//        if (totalDefinitions == null || totalDefinitions <= currentDefinition + 1) {
//            return false
//        }
//
//        this.currentDefinitionIndex.postValue(currentDefinition + 1)
//        return true
//    }
//
//    fun decrementCurrentDefinitionIndex(): Boolean {
//        val definitions = allDefinitions.value
//        val currentDefinition = currentDefinitionIndex.value ?: 0
//
//        if (definitions?.error == true) {
//            return false
//        }
//
//        val totalDefinitions = definitions?.jishoWordDefinitions?.data?.size
//        if (totalDefinitions == null || currentDefinition < 1) {
//            return false
//        }
//
//        this.currentDefinitionIndex.postValue(currentDefinition - 1)
//        return true
//    }
//
//    fun setSelectSnippetMode(value: Boolean) {
//        isInSelectSnippetToEditMode.postValue(value)
//    }
//
//    fun setSelectWordSelectModeMenuOpen(value: Boolean) {
//        _isWordSelectModeMenuOpen.postValue(value)
//    }
//
//    fun clearEditSnippetInfo() {
//        _snippetToEdit.postValue(null)
//    }
//
//    fun setSelectedWord(value: String?) {
//        tempSelectedWord.postValue(
//                if (value.isNullOrBlank()) null else TempSelectedWord.SimpleWord(value)
//        )
//    }
//
//    fun setSearchWord(value: String?) {
//        // TODO Sanitise word
//        searchWord.postValue(value)
//    }
//
//    /*
//     * Helper Classes
//     */
//
//    private inner class SentenceMediatorLiveData(
//            val currentSnippet_: LiveData<FullSnippetAndChar?>,
//            val wordSelectMode_: LiveData<WordSelectMode>,
//            val text_: LiveData<Text?>,
//    ) : MediatorLiveData<SentenceWithParsedInfo?>() {
////        private val previousSnippets = currentSnippet_.switchMap {
////            it?.snippet.let { currentSnippet ->
////                if (currentSnippet == null) return@switchMap MutableLiveData(listOf())
////                snippetsRepo.getPreviousSnippets(currentSnippet, SURROUNDING_SNIPPETS_TO_RETRIEVE)
////            }
////        }
////        private val nextSnippets = currentSnippet_.switchMap {
////            it?.snippet.let { currentSnippet ->
////                if (currentSnippet == null) return@switchMap MutableLiveData(listOf())
////                snippetsRepo.getNextSnippets(currentSnippet, SURROUNDING_SNIPPETS_TO_RETRIEVE)
////            }
////        }
//
//        init {
//            value = null
//            addSource(currentSnippet_) { generateNewSentence() }
////            addSource(previousSnippets) { generateNewSentence() }
////            addSource(nextSnippets) { generateNewSentence() }
//            addSource(wordSelectMode_) { newWordSelectMode ->
//                if (value == null) {
//                    return@addSource
//                }
//
//                if (newWordSelectMode.isAuto && value?.parseError != true && value?.parsedInfo == null) {
//                    viewModelScope.launch {
//                        value?.sentence?.startParse()
//                    }
//                    return@addSource
//                }
//            }
//        }
//
//        private fun updateCurrentLocationInText(currentSentenceStart: Sentence.IndexInfo, hasNextSentence: Boolean) {
//            val snippetId = if (!hasNextSentence) null else currentSentenceStart.textSnippetId
//            val currentChar = if (!hasNextSentence) null else currentSentenceStart.startIndex
//
//            text_.value?.let { oldText ->
//                val newText = Text(
//                        oldText.id,
//                        oldText.name,
//                        snippetId,
//                        currentChar,
//                        oldText.isComplete || !hasNextSentence
//                )
//                viewModelScope.launch {
//                    textsRepo.update(newText)
//                }
//            } ?: Log.w(LOG_TAG, "Text not found")
//        }
//
//        private fun generateNewSentence() {
//            value?.sentence?.cancelParse()
//            if (currentSnippet_.value == null) return
//
//            var sentence: Sentence? = null
//            try {
//                sentence = Sentence(
//                        currentSnippet_.value?.snippet,
//                        currentSnippet_.value?.currentCharacter,
//                        parserSuccessCallback = {
//                            postValue(SentenceWithParsedInfo(sentence!!, it))
//                        },
//                        parserFailCallback = {
//                            postValue(SentenceWithParsedInfo(sentence!!, parseError = true))
//                        }
//                )
//                updateCurrentLocationInText(sentence.getCurrentSentenceStart(), sentence.getNextSentenceStart() != null)
//            }
//            catch (e: Exception) {
//                postValue(null)
//                return
//            }
//            Log.d(LOG_TAG, "Current sentence start index: ${sentence.getCurrentSentenceStart().startIndex}")
//            postValue(SentenceWithParsedInfo(sentence))
//            if (wordSelectMode_.value!!.isAuto) {
//                viewModelScope.launch {
//                    sentence.startParse()
//                }
//            }
//        }
//    }
//
    data class SentenceWithParsedInfo(
            val sentence: Sentence,
            val parsedInfo: List<ParsedInfo>? = null,
            val parseError: Boolean = false
    )
//
//    data class WordDefinitionsWithErrorStatus(
//            val jishoWordDefinitions: JishoWordDefinitions? = null,
//            val error: Boolean = false
//    )
//
//    data class SnippetIdAndChar(val snippetId: Int, val currentCharacter: Int)
//    data class FullSnippetAndChar(val snippet: TextSnippet, val currentCharacter: Int)
//    data class SnippetIdWithStartAndEnd(val snippetId: Int, val startChar: Int, val endChar: Int?)
//
//    /**
//     * Temporarily store the word currently typed/selected on the screen without searching for it
//     */
//    sealed class TempSelectedWord {
//        data class SimpleWord(val word: String) : TempSelectedWord()
//        data class ParsedWord(val originalWord: String, val parsedInfo: ParsedInfo) : TempSelectedWord()
//    }
}