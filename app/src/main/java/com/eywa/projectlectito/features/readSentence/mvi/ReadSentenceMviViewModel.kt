package com.eywa.projectlectito.features.readSentence.mvi

import android.app.Application
import androidx.lifecycle.*
import com.eywa.projectlectito.R
import com.eywa.projectlectito.app.App
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.SnippetsRepo
import com.eywa.projectlectito.features.readSentence.WordSelectMode
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceEffect.Toast
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent.*
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.*
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionRequester
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ReadSentenceMviViewModel(application: Application) : AndroidViewModel(application) {
    private var _viewState: MutableLiveData<ReadSentenceViewState> = MutableLiveData(ReadSentenceViewState())
    var viewState: LiveData<ReadSentenceViewState> = _viewState
    val viewEffect = ViewEffect()

    @Inject
    lateinit var db: LectitoRoomDatabase

    init {
        (application as App).appComponent.inject(this)
    }

    private val snippetsRepo = SnippetsRepo(db.textSnippetsDao())

    fun handle(action: ReadSentenceIntent) {
        val currentState = viewState.value ?: ReadSentenceViewState()
        when (action) {
            is SelectedWordIntent -> handleSelectedWordChange(currentState, action)
            is OnWordSelectModeMenuStateChange ->
                _viewState.postValue(currentState.copy(isSelectModeMenuOpen = action.isOpen))
            is WordDefinitionIntent -> handleWordDefinitionChange(currentState, action)
            is SentenceIntent -> handleSentenceChange(currentState, action)
        }
    }

    private fun getSentenceJobOrInitPartial(currentState: ReadSentenceViewState): Job? {
        return when (val sentenceState = currentState.sentenceState) {
            is SentenceState.Error -> null
            is SentenceState.LoadingSentence -> sentenceState.sentenceJob
            is SentenceState.ValidSentence -> sentenceState.sentenceJob
            else -> {
                val job = Job()
                _viewState.postValue(_viewState.value!!.copy(sentenceState = SentenceState.LoadingSentence(job)))
                job
            }
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
        val sentenceState = currentState.sentenceState
        when (action) {
            is SentenceIntent.Initialise -> {
                val job = getSentenceJobOrInitPartial(currentState) ?: return
                job.launch {
                    snippetsRepo.getSnippetInfo(
                            action.textId,
                            action.currentSnippetId,
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
                                            )
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

                        // Clean up old state
                        if (sentenceState.isJobEqualTo(job)) {
                            sentenceState.cleanUp()
                        }
                    }
                }
            }
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
        val currentSelectedWordState = currentState.selectedWordState
        when (action) {
            is SelectedWordIntent.OnWordSelectModeChanged -> {
                val newWordSelectMode = action.wordSelectMode

                val newSelectedWordState =
                        if (newWordSelectMode.isAuto && currentSelectedWordState is SelectedWordState.ParsedState) {
                            currentSelectedWordState.copy(
                                    coloured = newWordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                            )
                        }
                        else {
                            when (newWordSelectMode) {
                                WordSelectMode.SELECT -> SelectedWordState.SelectState(null)
                                WordSelectMode.TYPE -> SelectedWordState.TypeState(null)
                                WordSelectMode.AUTO,
                                WordSelectMode.AUTO_WITH_COLOUR -> SelectedWordState.ParsedState(
                                        null,
                                        null,
                                        newWordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                                )
                            }
                        }
                _viewState.postValue(
                        currentState.copy(
                                selectedWordState = newSelectedWordState,
                                isSelectModeMenuOpen = false
                        )
                )
            }
            is SelectedWordIntent.OnSimpleWordSelected -> _viewState.postValue(
                    currentState.copy(
                            selectedWordState = when (currentSelectedWordState.wordSelectMode) {
                                WordSelectMode.SELECT -> SelectedWordState.SelectState(action.word)
                                WordSelectMode.TYPE -> SelectedWordState.TypeState(action.word)
                                else -> throw IllegalStateException("Invalid simple word selection state")
                            }
                    )
            )
            is SelectedWordIntent.OnParsedWordSelected -> {
                val newState = currentState.copy(
                        selectedWordState = when (currentSelectedWordState.wordSelectMode) {
                            WordSelectMode.AUTO,
                            WordSelectMode.AUTO_WITH_COLOUR -> SelectedWordState.ParsedState(
                                    action.word,
                                    action.parsedInfo,
                                    currentSelectedWordState.wordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                            )
                            else -> throw IllegalStateException("Invalid parsed word selection state")
                        }

                )

                // supplementary symbol (number, punctuation, etc.)
                val isWord = action.parsedInfo.partsOfSpeech[0] != "補助記号"
                if (isWord) {
                    searchForWord(newState)
                }
                else {
                    _viewState.postValue(newState)
                }
            }
            is SelectedWordIntent.OnSubmit -> {
                searchForWord(currentState)
            }
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
        updateWordDefinitionState(_viewState.value!!, wordDefinitionState)
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
        private const val SURROUNDING_SNIPPETS_TO_RETRIEVE = 1
    }
}