package com.eywa.projectlectito.readSentence

import android.app.Application
import android.graphics.Color
import android.text.*
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.*
import com.eywa.projectlectito.JAPANESE_LIST_DELIMINATOR
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.TextsRepo
import com.eywa.projectlectito.readSentence.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.readSentence.wordDefinitions.WordDefinitionRequester
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val textSnippetId = MutableLiveData<Int?>(null)
    fun getFirstSnippetForText(textId: Int) = snippetsRepo.getFirstSnippetId(textId)

    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
    private val currentCharacter = MutableLiveData<Int>(0)

    @Suppress("unchecked_cast") // Explicit cast required else compiler error
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

    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
    private val selectSnippetMode = MutableLiveData<Boolean>(false)
    private val editSnippetIdMutable = MutableLiveData<SnippetToEdit?>(null)
    val editSnippetId: LiveData<SnippetToEdit?> = editSnippetIdMutable


    /*
     * Current sentence info
     */
    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
    private val selectWordSelectModeMenuOpenMutable = MutableLiveData<Boolean>(false)
    val selectWordSelectModeMenuOpen = selectWordSelectModeMenuOpenMutable

    @Suppress("RemoveExplicitTypeArguments") // Explicit type because it should be non-nullable
    private val wordSelectModeMutable = MutableLiveData<WordSelectMode>(WordSelectMode.SELECT)
    val wordSelectMode: LiveData<WordSelectMode> = wordSelectModeMutable.distinctUntilChanged()
    val selectedParsedInfo = MutableLiveData<ParsedInfo?>(null)
    val sentence: LiveData<SentenceWithInfo?> =
            SentenceMediatorLiveData(
                    currentSnippet,
                    previousSnippets,
                    nextSnippets,
                    currentCharacter.distinctUntilChanged(),
                    wordSelectMode
            )

    /*
     * Word definitions
     */
    private val selectedWordMutable = MutableLiveData<String?>(null)
    val selectedWord = selectedWordMutable.distinctUntilChanged()
    private val searchWordMutable = MutableLiveData<String?>(null)
    val searchWord = searchWordMutable.distinctUntilChanged()
    val currentDefinition = MutableLiveData<Int>(0)
    val definitions: LiveData<WordDefinitionsWithInfo?> = object : MediatorLiveData<WordDefinitionsWithInfo?>() {
        var currentRequester: WordDefinitionRequester? = null

        init {
            addSource(searchWordMutable.distinctUntilChanged()) { word ->
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

    fun updateWordSelectMode(wordSelectMode: WordSelectMode) {
        wordSelectModeMutable.postValue(wordSelectMode)
        selectedWordMutable.postValue(null)
        searchWordMutable.postValue(null)
        selectedParsedInfo.postValue(null)
        currentDefinition.postValue(0)
    }

    fun updateCurrentCharacter(indexInfo: Sentence.IndexInfo): Boolean {
        // TODO Mediator will update twice, only want it to update once
        currentCharacter.postValue(indexInfo.startIndex)
        textSnippetId.postValue(indexInfo.textSnippetId)
        return true
    }

    fun incrementCurrentDefinitionIndex(): Boolean {
        val definitions = definitions.value
        val currentDefinition = currentDefinition.value ?: 0

        if (definitions?.error == true) {
            return false
        }

        val totalDefinitions = definitions?.jishoWordDefinitions?.data?.size
        if (totalDefinitions == null || totalDefinitions <= currentDefinition + 1) {
            return false
        }

        this.currentDefinition.postValue(currentDefinition + 1)
        return true
    }

    fun decrementCurrentDefinitionIndex(): Boolean {
        val definitions = definitions.value
        val currentDefinition = currentDefinition.value ?: 0

        if (definitions?.error == true) {
            return false
        }

        val totalDefinitions = definitions?.jishoWordDefinitions?.data?.size
        if (totalDefinitions == null || currentDefinition < 1) {
            return false
        }

        this.currentDefinition.postValue(currentDefinition - 1)
        return true
    }

    fun setSelectSnippetMode(value: Boolean) {
        selectSnippetMode.postValue(value)
    }

    fun setSelectWordSelectModeMenuOpen(value: Boolean) {
        selectWordSelectModeMenuOpenMutable.postValue(value)
    }

    fun clearEditSnippetInfo() {
        editSnippetIdMutable.postValue(null)
    }

    fun setSelectedWord(value: String?) {
        selectedWordMutable.postValue(value)
    }

    fun setSearchWord(value: String?) {
        // TODO Sanitise word
        searchWordMutable.postValue(value)
    }

    data class SnippetToEdit(
            val id: Int,
            val startChar: Int,
            val endChar: Int?,
    )

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
            addSource(currentCharacter) {
                generateNewSentence()
            }
            addSource(wordSelectMode) { newWordSelectMode ->
                if (value == null) {
                    return@addSource
                }

                if (newWordSelectMode.isAuto && value?.parseError != true && value?.parsedInfo == null) {
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
                        currentSnippet.value,
                        currentCharacter.value,
                        previousSnippets.value,
                        nextSnippets.value,
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
            Log.d(LOG_TAG, "Current sentence start index: ${sentence.getCurrentSentenceStart().startIndex}")
            value = SentenceWithInfo(sentence)
            if (wordSelectMode.value!!.isAuto) {
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

    /*
     * Data binding states
     */
    val mainViewState = MainViewState()
    val selectedWordSimpleViewState = SelectedWordInfoSimpleViewState()
    val selectedWordParsedViewState = SelectedWordInfoParsedViewState()
    val wordDefinitionViewState = WordDefinitionViewState()
    val wordSelectModeViewState = WordSelectModeViewState()

    inner class MainViewState {
        val hasValidSentence = sentence.map { it?.sentence?.currentSentence != null }
        val isInSelectSnippetMode: LiveData<Boolean> = selectSnippetMode
        val parseComplete = sentence.map { it?.parseError != true && it?.parsedInfo != null }
        val parseFailed = sentence.map { it?.parseError == true }
        val textName = currentSnippet.switchMap { snippet ->
            if (snippet == null) return@switchMap MutableLiveData(null)
            textsRepo.getTextById(snippet.textId).map { it.name }
        }.distinctUntilChanged()
        val chapterPage = currentSnippet.map { it?.getChapterPageString() ?: "" }
        val previousSentence = sentence.map { it?.sentence?.previousSentence }
        val hasNextSentence = sentence.map { it?.sentence?.getNextSentenceStart() != null }
        val isContentSelectable: LiveData<Boolean> = object : MediatorLiveData<Boolean>() {
            init {
                addSource(wordSelectMode) { update() }
                addSource(isInSelectSnippetMode) { update() }
            }

            private fun update() {
                postValue(wordSelectMode.value!! == WordSelectMode.SELECT || isInSelectSnippetMode.value!!)
            }
        }
        val editOverlayClickedListener = View.OnClickListener { setSelectSnippetMode(false) }
        val isSelectWordSelectModeMenuOpen = selectWordSelectModeMenuOpen

        val content: LiveData<SpannableString?> = object : MediatorLiveData<SpannableString?>() {
            init {
                addSource(sentence) { update() }
                addSource(wordSelectMode) { update() }
                addSource(isInSelectSnippetMode) { update() }
                addSource(parseComplete) { update() }
            }

            private fun update() {
                val sentence = sentence.value
                val wordSelectMode = wordSelectMode.value!!
                val isInSelectSnippetMode = isInSelectSnippetMode.value!!
                val content = sentence?.sentence?.currentSentence

                if (content == null) {
                    postValue(null)
                    return
                }

                val spannableString = SpannableString(content)

                if (isInSelectSnippetMode) {
                    val snippets = sentence.sentence.snippetsInCurrentSentence
                    snippets.forEachIndexed { index, snippetInfo -> spannableString.setSpan(index, snippetInfo) }
                    postValue(spannableString)
                    return
                }

                if (!wordSelectMode.isAuto || sentence.parseError || sentence.parsedInfo == null) {
                    postValue(spannableString)
                    return
                }

                val useColor = wordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                sentence.parsedInfo.forEachIndexed { index, parsedInfo ->
                    val color = if (useColor && index % 2 == 1) Color.RED else null
                    spannableString.setSpan(parsedInfo, content, color)
                }

                postValue(spannableString)
                return
            }

            private fun SpannableString.setSpan(index: Int, snippetInfo: Sentence.SnippetInfo) {
                if (index % 2 == 1) {
                    setSpan(
                            ForegroundColorSpan(Color.RED),
                            snippetInfo.currentSentenceStartIndex,
                            snippetInfo.currentSentenceEndIndex,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }

                setSpan(
                        object : ClickableSpan() {
                            override fun onClick(p0: View) {
                                editSnippetIdMutable.postValue(
                                        SnippetToEdit(
                                                snippetInfo.snippetId,
                                                snippetInfo.snippetStartIndex ?: 0,
                                                snippetInfo.snippetEndIndex
                                        )
                                )
                                selectSnippetMode.postValue(false)
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                // Don't underline or highlight the text
                            }
                        },
                        snippetInfo.currentSentenceStartIndex,
                        snippetInfo.currentSentenceEndIndex,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }

            private fun SpannableString.setSpan(
                    parsedInfo: ParsedInfo,
                    currentSentence: String,
                    @ColorInt color: Int? = null
            ) {
                val spanStartIndex = parsedInfo.startCharacterIndex
                val spanEndIndex = parsedInfo.endCharacterIndex

                setSpan(
                        object : ClickableSpan() {
                            override fun onClick(p0: View) {
                                selectedWordMutable.postValue(currentSentence.substring(spanStartIndex, spanEndIndex))
                                selectedParsedInfo.postValue(parsedInfo)

                                // supplementary symbol (number, punctuation, etc.)
                                val isWord = parsedInfo.partsOfSpeech[0] != "補助記号"
                                if (isWord) {
                                    searchWordMutable.postValue(parsedInfo.dictionaryForm)
                                }
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                // Don't underline or highlight the text
                            }
                        },
                        spanStartIndex,
                        spanEndIndex,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )

                if (color != null) {
                    setSpan(
                            ForegroundColorSpan(color),
                            spanStartIndex,
                            spanEndIndex,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    inner class SelectedWordInfoSimpleViewState {
        val showView: LiveData<Boolean> = wordSelectMode.map {
            it == WordSelectMode.SELECT || it == WordSelectMode.TYPE
        }
        val selectVisibility: LiveData<Boolean> = wordSelectMode.map { it == WordSelectMode.SELECT }
        val typeVisibility: LiveData<Boolean> = wordSelectMode.map { it == WordSelectMode.TYPE }
    }

    inner class SelectedWordInfoParsedViewState {
        val showView: LiveData<Boolean> = wordSelectMode.map { it.isAuto }
        val originalWord: LiveData<String?> = selectedWordMutable
        val dictionaryForm: LiveData<String?> = object : MediatorLiveData<String?>() {
            init {
                addSource(selectedParsedInfo) { update() }
                addSource(originalWord) { update() }
            }

            private fun update() {
                val parsedInfo = selectedParsedInfo.value
                if (parsedInfo?.dictionaryForm.isNullOrBlank() || parsedInfo!!.dictionaryForm == originalWord.value) {
                    postValue(null)
                    return
                }
                postValue(parsedInfo.dictionaryForm)
            }
        }
        val partsOfSpeech: LiveData<String?> = selectedParsedInfo.map { parsedInfo ->
            parsedInfo?.partsOfSpeech
                    ?.filterNot { it.isBlank() || it == "*" }
                    ?.joinToString(JAPANESE_LIST_DELIMINATOR)
        }
        val pitchAccent: LiveData<String?> = selectedParsedInfo.map { parsedInfo ->
            parsedInfo?.pitchAccentPattern?.toString()
        }
    }

    inner class WordDefinitionViewState {
        val currDefinition: LiveData<JishoWordDefinitions.JishoEntry?> = object :
                MediatorLiveData<JishoWordDefinitions.JishoEntry?>() {
            init {
                addSource(definitions) { update() }
                addSource(currentDefinition) { update() }
            }

            private fun update() {
                val definitions = definitions.value
                val currentIndex = currentDefinition.value ?: 0

                if (definitions == null || definitions.error || definitions.jishoWordDefinitions == null) {
                    postValue(null)
                    return
                }

                val currentDefinition = definitions.jishoWordDefinitions.data[currentIndex]
                if (currentDefinition.japanese.isNullOrEmpty()) {
                    postValue(null)
                    return
                }

                postValue(currentDefinition)
            }
        }
        val notFoundString: LiveData<Int?> = object : MediatorLiveData<Int?>() {
            init {
                addSource(definitions) { update() }
                addSource(wordSelectMode) { update() }
            }

            private fun update() {
                if (definitions.value != null) {
                    postValue(null)
                    return
                }
                postValue(wordSelectMode.value!!.noDefinitionStringId)
            }
        }
        val previousDefinitionButtonEnabled: LiveData<Boolean> = currentDefinition.map { it > 0 }
        val nextDefinitionButtonEnabled: LiveData<Boolean> = object : MediatorLiveData<Boolean>() {
            init {
                addSource(definitions) { update() }
                addSource(currentDefinition) { update() }
            }

            private fun update() {
                val definitions = definitions.value
                val currentIndex = currentDefinition.value ?: 0

                if (definitions == null || definitions.error || definitions.jishoWordDefinitions == null) {
                    postValue(false)
                    return
                }

                postValue(currentIndex + 1 < definitions.jishoWordDefinitions.data.size)
            }
        }
        val word: LiveData<String?> = currDefinition.map { definition ->
            if (definition == null) {
                return@map null
            }
            var newWord = definition.japanese[0].word
            if (newWord.isNullOrBlank()) {
                newWord = definition.slug
            }
            return@map newWord
        }
        val reading: LiveData<String?> = currDefinition.map { it?.japanese?.get(0)?.reading }
        val isCommon: LiveData<Boolean> = currDefinition.map { it?.is_common ?: false }
        val jlpt: LiveData<String?> = currDefinition.map { it?.jlpt?.joinToString(",") }
        val tags: LiveData<String?> = currDefinition.map { it?.tags?.joinToString(",") }
        val otherForms: LiveData<String?> = currDefinition.map { definition ->
            if (definition?.japanese?.size ?: 0 <= 1) {
                return@map null
            }
            // TODO Stop this from getting too long?
            return@map definition!!.japanese
                    .subList(1, definition.japanese.size)
                    .joinToString(JAPANESE_LIST_DELIMINATOR) { "${it.word}[${it.reading}]" }
        }
    }

    inner class WordSelectModeViewState {
        val showMenu = selectWordSelectModeMenuOpen
        val currentModeIcon = wordSelectMode.map { it.iconId }
        val currentModeContentDescription = wordSelectMode.map { it.iconDescriptionId }
    }
}