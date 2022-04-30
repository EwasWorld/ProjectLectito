package com.eywa.projectlectito.features.readSentence.mvi

import android.app.Application
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.annotation.ColorInt
import androidx.lifecycle.*
import com.eywa.projectlectito.R
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.database.texts.TextsRepo
import com.eywa.projectlectito.features.readSentence.ParsedInfo
import com.eywa.projectlectito.features.readSentence.Sentence
import com.eywa.projectlectito.features.readSentence.WordSelectMode
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceEffect.Toast
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent.*
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent.SentenceIntent.*
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.*
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionRequester
import com.eywa.projectlectito.utils.androidWrappers.UnformattedClickableSpan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.net.UnknownHostException
import javax.inject.Inject

class ReadSentenceMviViewModel(application: Application) : AndroidViewModel(application) {
    private var _viewState: MutableLiveData<ReadSentenceViewState> = MutableLiveData(ReadSentenceViewState())
    var viewState: LiveData<ReadSentenceViewState> = _viewState
    val viewEffect = ViewEffect()

    /**
     * Separated from viewState because it derives from [viewState] but updating it every time [viewState] updates
     * causes issues with selection. Namely, when [viewState] is updated, it generates a new sentence that has no
     * selection, thus clearing the selection immediately.
     */
    val currentSentence = viewState.map { it.getSentenceSpannableString() }.distinctUntilChanged()

    /**
     * Separated from viewState because it derives from [viewState] but updating it every time [viewState] updates
     * causes all sorts of update issues with the current index and view pager heights.
     */
    val wordDefinitionPagerInfo =
            viewState.map { it.wordDefinitionState.asHasWord()?.toViewPagerInfo() }
                    .distinctUntilChanged()

    @Inject
    lateinit var db: LectitoRoomDatabase

    init {
        (application as App).appComponent.inject(this)
    }

    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao())
    private val textsRepo = TextsRepo(db.textsDao(), db.textSnippetsDao())

    fun handle(action: ReadSentenceIntent) {
        val currentState = viewState.value ?: ReadSentenceViewState()
        when (action) {
            is SelectedWordIntent -> handleSelectedWordChange(currentState, action)
            is WordDefinitionIntent -> handleWordDefinitionChange(currentState, action)
            is SentenceIntent -> handleSentenceChange(currentState, action)
        }
    }

    private fun Job.launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            withContext(this@launch) {
                block()
            }
        }
    }

    private fun handleSentenceChange(currentState: ReadSentenceViewState, action: SentenceIntent) {
        when (action) {
            is Initialise -> setNewSentence(currentState, action.textId, action.currentSnippetId)
            OnNextSentenceClicked -> updateSentence(
                    currentState,
                    currentState.sentenceState.asValid()?.nextSentenceStart
            )
            OnPreviousSentenceClicked -> updateSentence(
                    currentState,
                    currentState.sentenceState.asValid()?.previousSentenceStart
            )
            OnEditSentenceClicked -> {
                val validSentence = currentState.sentenceState.asValid()
                val snippets = validSentence?.sentence?.snippetsInCurrentSentence ?: return
                when (snippets.size) {
                    0 -> throw IllegalStateException("No snippets found")
                    1 -> viewEffect.postValue(ReadSentenceEffect.NavigateTo.EditSnippet(snippets[0]))
                    else -> _viewState.postValue(
                            currentState.copy(sentenceState = validSentence.copy(isChooseSnippetToEditMenuOpen = true))
                    )
                }
            }
            OnEditOverlayClicked -> {
                val validSentence = currentState.sentenceState.asValid()
                if (validSentence == null) {
                    viewEffect.postValue(Toast.ResIdToast(R.string.err_read_sentence__no_text_to_edit))
                    return
                }
                _viewState.postValue(
                        currentState.copy(sentenceState = validSentence.copy(isChooseSnippetToEditMenuOpen = false))
                )
            }
            OnViewFullTextClicked -> {
                val firstSnippet = currentState.sentenceState.asValid()
                        ?.sentence?.snippetsInCurrentSentence
                        ?.takeIf { it.isNotEmpty() }
                        ?.get(0)
                        ?: return
                viewEffect.postValue(ReadSentenceEffect.NavigateTo.ReadFullText(firstSnippet))
            }
        }
    }

    private fun updateSentence(currentState: ReadSentenceViewState, newRef: Sentence.IndexInfo?) {
        val sentenceState = currentState.sentenceState
        if (sentenceState !is SentenceState.Valid || newRef == null) {
            viewEffect.postValue(Toast.ResIdToast(R.string.err_read_sentence__no_more_sentences))
            return
        }
        val currentSnippet = sentenceState.snippets[sentenceState.previousSnippetCount]
        val loadNewSentence = currentSnippet.id != newRef.textSnippetId

        if (loadNewSentence) {
            sentenceState.cleanUp()
        }
        viewModelScope.launch {
            textsRepo.update(
                    sentenceState.text.copy(
                            currentSnippetId = newRef.textSnippetId,
                            currentCharacterIndex = newRef.startIndex
                    )
            )
        }
        if (loadNewSentence) {
            setNewSentence(currentState, currentSnippet.textId, newRef.textSnippetId)
        }
    }

    private fun setNewSentence(
            initialState: ReadSentenceViewState,
            textId: Int,
            desiredSnippetId: Int?
    ) {
        val job = Job()
        _viewState.postValue(initialState.copy(sentenceState = SentenceState.Loading(job)))
        job.launch {
            snippetsRepo.getSnippetInfo(
                    textId,
                    desiredSnippetId,
                    SURROUNDING_SNIPPETS_TO_RETRIEVE
            ).asFlow().collect {
                if (!job.isActive) return@collect

                onUpToDateState { currentState ->
                    val sentenceState = currentState.sentenceState
                    if (it != null) {
                        val newSentenceState = SentenceState.Valid(
                                sentenceJob = job,
                                snippets = it.snippets,
                                text = it.text,
                                currentCharacter = it.text.currentCharacterIndex ?: 0,
                                previousSnippetCount = it.prevSnippetsCount
                        )
                        _viewState.postValue(currentState.copy(sentenceState = newSentenceState))

                        val currentSnippetId = newSentenceState.snippets[newSentenceState.previousSnippetCount].id
                        if (it.text.currentSnippetId != currentSnippetId) {
                            job.launch {
                                textsRepo.update(
                                        it.text.copy(currentSnippetId = currentSnippetId, currentCharacterIndex = 0)
                                )
                            }
                        }

                        val initialSentence = initialState.sentenceState.asValid()?.sentence
                        if (newSentenceState.sentence.currentSentence == initialSentence?.currentSentence) {
                            Log.w(LOG_TAG, "Possible issue when moving to a new sentence")
                        }
                        if (currentState.wordSelectionState is WordSelectionState.ParsedMode) {
                            startSentenceParse(currentState, newSentenceState)
                        }
                    }
                    else if (sentenceState is SentenceState.Loading || sentenceState is SentenceState.Valid) {
                        if (!sentenceState.isJobEqualTo(job)) {
                            _viewState.postValue(currentState.copy(sentenceState = SentenceState.Loading(job)))
                        }
                    }
                    else {
                        // TODO Log/toast error
                        _viewState.postValue(currentState.copy(sentenceState = SentenceState.Error))
                    }
                }
            }
        }

        // Clean up old state
        if (initialState.sentenceState.isJobEqualTo(job)) {
            initialState.sentenceState.cleanUp()
        }
    }

    private fun handleWordDefinitionChange(currentState: ReadSentenceViewState, action: WordDefinitionIntent) {
        when (action) {
            WordDefinitionIntent.OnClosePressed -> updateWordDefinitionState(currentState, WordDefinitionState.None)
            is WordDefinitionIntent.OnSubmit -> searchForWord(currentState)
            is WordDefinitionIntent.OnDefinitionPageChanged -> {
                val state = currentState.wordDefinitionState.asHasWord() ?: return
                _viewState.postValue(
                        currentState.copy(wordDefinitionState = state.copy(selectedIndex = action.newPosition))
                )
            }
        }
    }

    private fun handleSelectedWordChange(currentState: ReadSentenceViewState, action: SelectedWordIntent) {
        val currentWordState = currentState.wordSelectionState
        when (action) {
            is SelectedWordIntent.OnWordSelectModeMenuStateChange -> {
                if (action.isOpen) {
                    viewEffect.postValue(ReadSentenceEffect.ClearTextSelection)
                }
                _viewState.postValue(
                        currentState.copy(
                                wordSelectionState = currentState.wordSelectionState.copyAbstract(action.isOpen)
                        )
                )
            }
            is SelectedWordIntent.OnWordSelectModeChanged -> {
                val newWordSelectMode = action.wordSelectMode

                var shouldStartParse = false
                val newSelectedWordState =
                        if (newWordSelectMode == currentWordState.wordSelectMode) {
                            currentWordState.copyAbstract(false)
                        }
                        else if (newWordSelectMode.isAuto && currentWordState is WordSelectionState.ParsedMode) {
                            currentWordState.copy(
                                    coloured = newWordSelectMode == WordSelectMode.AUTO_WITH_COLOUR,
                                    isChangeWordSelectionModeMenuOpen = false
                            )
                        }
                        else {
                            when (newWordSelectMode) {
                                WordSelectMode.MANUAL -> WordSelectionState.ManualMode.TypeMode()
                                WordSelectMode.AUTO,
                                WordSelectMode.AUTO_WITH_COLOUR -> {
                                    shouldStartParse = true
                                    WordSelectionState.ParsedMode(
                                            null,
                                            null,
                                            newWordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                                    )
                                }
                            }
                        }
                _viewState.postValue(currentState.copy(wordSelectionState = newSelectedWordState))
                if (shouldStartParse) {
                    currentState.sentenceState.asValid()?.let { startSentenceParse(currentState, it) }
                }
            }
            is SelectedWordIntent.OnSimpleWordTyped -> {
                if (currentState.wordSelectionState !is WordSelectionState.ManualMode) return
                _viewState.postValue(
                        currentState.copy(
                                wordSelectionState = WordSelectionState.ManualMode.TypeMode(
                                        action.word
                                )
                        )
                )
            }
            is SelectedWordIntent.OnSentenceTextSelected -> {
                val currentMode = currentWordState.asSelectMode()
                if (currentMode != null) {
                    currentMode.takeIf {
                        action.start != it.selectionStart || action.end != it.selectionEnd
                    } ?: return
                }

                val currentSentence = currentState.getSentenceSpannableString()?.toString() ?: return
                val start = action.start.takeIf { it in currentSentence.indices } ?: return
                val end = action.end.takeIf { it in currentSentence.indices } ?: return
                val newSelection = currentSentence.substring(start, end).takeIf { it.isNotBlank() } ?: return
                _viewState.postValue(
                        currentState.copy(
                                wordSelectionState = WordSelectionState.ManualMode.SelectMode(
                                        newSelection,
                                        action.start,
                                        action.end
                                )
                        )
                )
            }
        }
    }

    private fun startSentenceParse(currentState: ReadSentenceViewState, validSentence: SentenceState.Valid) {
        fun updateState(parsedInfo: List<ParsedInfo>?) {
            onUpToDateState { newState ->
                val newValidSentence = newState.sentenceState.asValid() ?: return@onUpToDateState
                val actualSentence = newValidSentence.sentence.currentSentence
                val expectedSentence = currentState.sentenceState.asValid()?.sentence?.currentSentence
                if (actualSentence == expectedSentence) {
                    _viewState.postValue(
                            newState.copy(
                                    sentenceState = newValidSentence.copy(
                                            parseError = parsedInfo == null,
                                            parsedInfo = parsedInfo
                                    )
                            )
                    )
                }
            }
        }

        viewModelScope.launch(validSentence.sentenceJob) {
            validSentence.sentence.startParse(
                    parserSuccessCallback = { updateState(it) },
                    parserFailCallback = {
                        Log.e(LOG_TAG, it.message ?: "")
                        updateState(null)
                    }
            )
        }

    }

    // TODO This is probably wrong - maybe look into flows?
    private fun onUpToDateState(block: (ReadSentenceViewState) -> Unit) {
        var observer: Observer<ReadSentenceViewState>? = null
        observer = Observer<ReadSentenceViewState> {
            block(it)
            _viewState.removeObserver(observer!!)
        }
        viewModelScope.launch(Dispatchers.Main) {
            _viewState.observeForever(observer)
        }
    }

    private fun searchForWord(currentState: ReadSentenceViewState) {
        val searchWord = currentState.wordSelectionState.wordToSearch
        if (searchWord.isNullOrBlank()) {
            viewEffect.postValue(
                    Toast.ResIdToast(
                            currentState.wordSelectionState.nullWordSearchedMessage
                                    ?: throw IllegalStateException("No error message for null word")
                    )
            )
            return
        }

        // TODO Sanitise word
        val requester = WordDefinitionRequester(
                word = searchWord,
                successCallback = { response ->
                    val newState = when {
                        response.meta.status != 200 -> WordDefinitionState.Error()
                        response.data.isEmpty() -> WordDefinitionState.Error(WordDefinitionState.ErrorType.NO_DEFINITIONS)
                        else -> WordDefinitionState.HasWord(response.data, 0)
                    }
                    updateWordDefinitionState(newState)
                },
                failCallback = {
                    val error = when (it) {
                        is UnknownHostException -> WordDefinitionState.Error(WordDefinitionState.ErrorType.NO_INTERNET)
                        else -> WordDefinitionState.Error()
                    }
                    updateWordDefinitionState(error)
                }
        )
        updateWordDefinitionState(WordDefinitionState.Loading(searchWord, requester))
        viewModelScope.launch {
            requester.getDefinition()
        }
    }

    private fun updateWordDefinitionState(wordDefinitionState: WordDefinitionState) {
        onUpToDateState {
            updateWordDefinitionState(it, wordDefinitionState)
        }
    }

    /**
     * Ensures the old word definition is cleaned up properly before applying the new state
     */
    private fun updateWordDefinitionState(
            currentState: ReadSentenceViewState,
            wordDefinitionState: WordDefinitionState
    ) {
        currentState.wordDefinitionState.cancel()
        _viewState.postValue(currentState.copy(wordDefinitionState = wordDefinitionState))
    }

    private fun ReadSentenceViewState.getSentenceSpannableString(): SpannableString? {
        fun SpannableString.setAlternatingColourSpan(span: IntRange, index: Int, @ColorInt color: Int = Color.RED) =
                index.takeIf { it % 2 == 1 }
                        ?.let { setSpan(ForegroundColorSpan(color), span.first, span.last, SPAN_FLAGS) }

        val validSentence = (sentenceState as? SentenceState.Valid) ?: return null
        val wordSelectMode = wordSelectionState
        val content = validSentence.sentence.currentSentence ?: return null

        val spannableString = SpannableString(content)
        if (sentenceState.isChooseSnippetToEditMenuOpen) {
            /*
             * Select a snippet from a sentence which spans multiple snippets
             */
            val snippets = validSentence.sentence.snippetsInCurrentSentence
            snippets.forEachIndexed { index, snippetInfo ->
                spannableString.setSpan(
                        UnformattedClickableSpan {
                            viewEffect.postValue(ReadSentenceEffect.NavigateTo.EditSnippet(snippetInfo))
                        },
                        snippetInfo.currentSentenceStartIndex,
                        snippetInfo.currentSentenceEndIndex,
                        SPAN_FLAGS
                )
                spannableString.setAlternatingColourSpan(
                        snippetInfo.currentSentenceStartIndex..snippetInfo.currentSentenceEndIndex,
                        index
                )
            }
            return spannableString
        }

        if (wordSelectMode !is WordSelectionState.ParsedMode || !sentenceState.isParseComplete) {
            return spannableString
        }

        /*
         * Select a word from a parsed sentence
         */
        validSentence.parsedInfo!!.forEachIndexed { index, parsedInfo ->
            val spanStartIndex = parsedInfo.startCharacterIndex
            val spanEndIndex = parsedInfo.endCharacterIndex

            spannableString.setSpan(
                    UnformattedClickableSpan {
                        onUpToDateState { upToDateState ->
                            val newState = upToDateState.copy(
                                    wordSelectionState = WordSelectionState.ParsedMode(
                                            content.substring(spanStartIndex, spanEndIndex),
                                            parsedInfo,
                                            upToDateState.wordSelectionState.wordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                                    )
                            )
                            _viewState.postValue(newState)

                            // supplementary symbol (number, punctuation, etc.)
                            val isWord = parsedInfo.partsOfSpeech[0] != "補助記号"
                            if (isWord) {
                                searchForWord(newState)
                            }
                        }
                    },
                    spanStartIndex,
                    spanEndIndex,
                    SPAN_FLAGS
            )
            if (wordSelectMode.coloured) {
                spannableString.setAlternatingColourSpan(spanStartIndex..spanEndIndex, index)
            }
        }

        return spannableString
    }

    class ViewEffect {
        private var viewEffect: MutableLiveData<ReadSentenceEffect?> = MutableLiveData(null)

        fun postValue(effect: ReadSentenceEffect) {
            viewEffect.value = effect
            viewEffect.value = null
        }

        fun getViewEffect(): LiveData<ReadSentenceEffect?> {
            return viewEffect
        }
    }

    companion object {
        private const val LOG_TAG = "ReadSentenceMviViewMode"
        private const val SURROUNDING_SNIPPETS_TO_RETRIEVE = 1
        private const val SPAN_FLAGS = Spanned.SPAN_INCLUSIVE_EXCLUSIVE
    }
}