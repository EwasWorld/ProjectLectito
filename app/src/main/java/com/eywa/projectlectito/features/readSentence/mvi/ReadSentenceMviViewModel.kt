package com.eywa.projectlectito.features.readSentence.mvi

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
    val currentSentence = viewState.map { it.getSentence() }.distinctUntilChanged()

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
            is OnWordSelectModeMenuStateChange -> {
                if (action.isOpen) {
                    viewEffect.postValue(ReadSentenceEffect.ClearTextSelection)
                }
                _viewState.postValue(currentState.copy(isSelectModeMenuOpen = action.isOpen))
            }
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
                    currentState.sentenceState.asValidSentence()?.nextSentenceStart
            )
            OnPreviousSentenceClicked -> updateSentence(
                    currentState,
                    currentState.sentenceState.asValidSentence()?.previousSentenceStart
            )
            OnEditSentenceClicked -> {
                val snippets =
                        currentState.sentenceState.asValidSentence()?.sentence?.snippetsInCurrentSentence ?: return
                when (snippets.size) {
                    0 -> throw IllegalStateException("No snippets found")
                    1 -> viewEffect.postValue(ReadSentenceEffect.NavigateTo.EditSnippet(snippets[0]))
                    else -> _viewState.postValue(currentState.copy(isChoosingSnippetToEdit = true))
                }
            }
            OnEditOverlayClicked -> _viewState.postValue(currentState.copy(isChoosingSnippetToEdit = false))
            OnViewFullTextClicked -> {
                val firstSnippet = currentState.sentenceState.asValidSentence()
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
        if (sentenceState !is SentenceState.ValidSentence || newRef == null) {
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
            currentState: ReadSentenceViewState,
            textId: Int,
            currentSnippetId: Int?
    ) {
        val sentenceState = currentState.sentenceState
        val job = Job()
        _viewState.postValue(_viewState.value!!.copy(sentenceState = SentenceState.LoadingSentence(job)))
        job.launch {
            snippetsRepo.getSnippetInfo(
                    textId,
                    currentSnippetId,
                    SURROUNDING_SNIPPETS_TO_RETRIEVE
            ).asFlow().collect {
                if (!job.isActive) return@collect

                if (it != null) {
                    _viewState.postValue(
                            currentState.copy(
                                    sentenceState = SentenceState.ValidSentence(
                                            sentenceJob = job,
                                            snippets = it.snippets,
                                            text = it.text,
                                            currentCharacter = it.text.currentCharacterIndex ?: 0,
                                            previousSnippetCount = it.prevSnippetsCount
                                    ),
                                    parsedWordClickedListener = { word, parsedInfo ->
                                        onUpToDateState { upToDateState ->
                                            val newState = upToDateState.copy(
                                                    selectedWordState = SelectedWordState.ParsedState(
                                                            word,
                                                            parsedInfo,
                                                            upToDateState.selectedWordState.wordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
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
                                    onSnippetToEditClicked = { snippetInfo ->
                                        viewEffect.postValue(ReadSentenceEffect.NavigateTo.EditSnippet(snippetInfo))
                                    }
                            )
                    )
                }
                else if (sentenceState is SentenceState.LoadingSentence
                        || sentenceState is SentenceState.ValidSentence
                ) {
                    if (!sentenceState.isJobEqualTo(job)) {
                        _viewState.postValue(
                                _viewState.value!!.copy(sentenceState = SentenceState.LoadingSentence(job))
                        )
                    }
                }
                else {
                    // TODO Log/toast error
                    _viewState.postValue(_viewState.value!!.copy(sentenceState = SentenceState.Error))
                }
            }
        }

        // Clean up old state
        if (sentenceState.isJobEqualTo(job)) {
            sentenceState.cleanUp()
        }
    }

    private fun handleWordDefinitionChange(currentState: ReadSentenceViewState, action: WordDefinitionIntent) {
        val currentWordDefinitionState = currentState.wordDefinitionState
        when (action) {
            WordDefinitionIntent.OnClosePressed -> updateWordDefinitionState(currentState, WordDefinitionState.NoWord)
            WordDefinitionIntent.OnNextPressed -> {
                currentWordDefinitionState.getAsHasWord()!!.nextEntry()
                        ?.let { next -> updateWordDefinitionState(currentState, next) }
                        ?: run {
                            viewEffect.postValue(Toast.ResIdToast(R.string.err_read_sentence__no_more_definitions))
                        }
            }
            WordDefinitionIntent.OnPreviousPressed -> {
                currentWordDefinitionState.getAsHasWord()!!.previousEntry()
                        ?.let { prev -> updateWordDefinitionState(currentState, prev) }
                        ?: run {
                            viewEffect.postValue(Toast.ResIdToast(R.string.err_read_sentence__no_more_definitions))
                        }
            }
        }
    }

    private fun handleSelectedWordChange(currentState: ReadSentenceViewState, action: SelectedWordIntent) {
        val currentWordState = currentState.selectedWordState
        when (action) {
            is SelectedWordIntent.OnWordSelectModeChanged -> {
                val newWordSelectMode = action.wordSelectMode

                var shouldStartParse = false
                val newSelectedWordState =
                        if (newWordSelectMode == currentWordState.wordSelectMode) {
                            currentWordState
                        }
                        else if (newWordSelectMode.isAuto && currentWordState is SelectedWordState.ParsedState) {
                            currentWordState.copy(
                                    coloured = newWordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                            )
                        }
                        else {
                            when (newWordSelectMode) {
                                WordSelectMode.SELECT -> SelectedWordState.SelectState()
                                WordSelectMode.TYPE -> SelectedWordState.TypeState()
                                WordSelectMode.AUTO,
                                WordSelectMode.AUTO_WITH_COLOUR -> {
                                    shouldStartParse = true
                                    SelectedWordState.ParsedState(
                                            null,
                                            null,
                                            newWordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                                    )
                                }
                            }
                        }
                _viewState.postValue(
                        currentState.copy(
                                selectedWordState = newSelectedWordState,
                                isSelectModeMenuOpen = false
                        )
                )
                if (shouldStartParse) {
                    currentState.sentenceState.asValidSentence()?.let { startSentenceParse(currentState, it) }
                }
            }
            is SelectedWordIntent.OnSimpleWordSelected -> {
                _viewState.postValue(currentState.copy(selectedWordState = SelectedWordState.TypeState(action.word)))
            }
            is SelectedWordIntent.OnSubmit -> {
                searchForWord(currentState)
            }
            is SelectedWordIntent.OnSpanSelected -> {
                currentWordState.getAsSelectState()?.takeIf {
                    action.start != it.selectionStart || action.end != it.selectionEnd
                } ?: return

                val newSelection = currentState.getSentence()?.toString()?.substring(action.start, action.end)
                        .takeIf { !it.isNullOrBlank() } ?: return
                _viewState.postValue(
                        currentState.copy(
                                selectedWordState = SelectedWordState.SelectState(
                                        newSelection,
                                        action.start,
                                        action.end
                                )
                        )
                )
            }
        }
    }

    private fun startSentenceParse(currentState: ReadSentenceViewState, validSentence: SentenceState.ValidSentence) {
        fun updateState(parsedInfo: List<ParsedInfo>?) {
            onUpToDateState { newState ->
                val newValidSentence = newState.sentenceState.asValidSentence() ?: return@onUpToDateState
                val actualSentence = newValidSentence.sentence.currentSentence
                val expectedSentence = currentState.sentenceState.asValidSentence()?.sentence?.currentSentence
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
        val searchWord = currentState.selectedWordState.wordToSearch
        if (searchWord.isNullOrBlank()) {
            viewEffect.postValue(
                    Toast.ResIdToast(
                            currentState.selectedWordState.nullWordSearchedMessage
                                    ?: throw IllegalStateException("No error message for null word")
                    )
            )
            return
        }

        // TODO Sanitise word
        val requester = WordDefinitionRequester(
                word = searchWord,
                successCallback = { response ->
                    val newState = if (response.meta.status != 200) {
                        WordDefinitionState.Error
                    }
                    else {
                        WordDefinitionState.HasWord(response.data, 0)
                    }
                    updateWordDefinitionState(newState)
                },
                failCallback = {
                    updateWordDefinitionState(WordDefinitionState.Error)
                }
        )
        updateWordDefinitionState(WordDefinitionState.LoadingWord(requester))
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
    }
}