package com.eywa.projectlectito.features.readSentence.mvi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.eywa.projectlectito.features.readSentence.WordSelectMode
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent.*
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.SelectedWordState
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.WordDefinitionState
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionRequester
import kotlinx.coroutines.launch

class ReadSentenceMviViewModel(application: Application) : AndroidViewModel(application) {
    private var _viewState: MutableLiveData<ReadSentenceViewState> = MutableLiveData(ReadSentenceViewState())
    var viewState: LiveData<ReadSentenceViewState> = _viewState

    fun handle(action: ReadSentenceIntent) {
        val currentState = viewState.value ?: ReadSentenceViewState()
        when (action) {
            is SelectedWordIntent -> handleSelectedWordChange(currentState, action)
            is OnWordSelectModeMenuStateChange ->
                _viewState.postValue(currentState.copy(isSelectModeMenuOpen = action.isOpen))
            is WordDefinitionIntent -> handleWordDefinitionChange(currentState, action)
        }
    }

    private fun handleWordDefinitionChange(currentState: ReadSentenceViewState, action: WordDefinitionIntent) {
        val currentWordDefinitionState = currentState.wordDefinitionState
        when (action) {
            WordDefinitionIntent.OnClosePressed -> updateWordDefinitionState(currentState, WordDefinitionState.NoWord)
            WordDefinitionIntent.OnNextPressed -> {
                currentWordDefinitionState.getAsHasWord()!!.nextEntry()
                        ?.let { next -> updateWordDefinitionState(currentState, next) }
                        ?: TODO("Notify user no next")
            }
            WordDefinitionIntent.OnPreviousPressed -> {
                currentWordDefinitionState.getAsHasWord()!!.previousEntry()
                        ?.let { prev -> updateWordDefinitionState(currentState, prev) }
                        ?: TODO("Notify user no previous")
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
                searchForWord(
                        currentState.copy(
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
                )
            }
            is SelectedWordIntent.OnSubmit -> {
                searchForWord(currentState)
            }
        }
    }

    private fun searchForWord(currentState: ReadSentenceViewState) {
        val searchWord = currentState.selectedWordState.wordToSearch
        if (searchWord == null) {
            TODO("Notify user that there's no word selected")
            return
        }

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
}