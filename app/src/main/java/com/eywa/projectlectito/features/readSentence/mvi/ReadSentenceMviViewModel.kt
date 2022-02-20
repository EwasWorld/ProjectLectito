package com.eywa.projectlectito.features.readSentence.mvi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.eywa.projectlectito.features.readSentence.WordSelectMode
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent.OnWordSelectModeMenuStateChange
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent.SelectedWordIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.SelectedWordState
import com.eywa.projectlectito.utils.JAPANESE_LIST_DELIMINATOR

class ReadSentenceMviViewModel(application: Application) : AndroidViewModel(application) {
    private var _viewState: MutableLiveData<ReadSentenceViewState> = MutableLiveData(ReadSentenceViewState())
    var viewState: LiveData<ReadSentenceViewState> = _viewState

    fun handle(action: ReadSentenceIntent) {
        val currentState = viewState.value ?: ReadSentenceViewState()
        val newState = when (action) {
            is SelectedWordIntent -> handleSelectedWordChange(currentState, action)
            is OnWordSelectModeMenuStateChange -> currentState.copy(isSelectModeMenuOpen = action.isOpen)
        }
        _viewState.postValue(newState)
    }

    private fun handleSelectedWordChange(
            currentState: ReadSentenceViewState,
            action: SelectedWordIntent
    ): ReadSentenceViewState {
        val currentSelectedWordState = currentState.selectedWordState
        return when (action) {
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
                                        null,
                                        null,
                                        newWordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                                )
                            }
                        }
                currentState.copy(
                        selectedWordState = newSelectedWordState,
                        isSelectModeMenuOpen = false
                )
            }
            is SelectedWordIntent.OnSimpleWordSelected -> currentState.copy(
                    selectedWordState = when (currentSelectedWordState.wordSelectMode) {
                        WordSelectMode.SELECT -> SelectedWordState.SelectState(action.word)
                        WordSelectMode.TYPE -> SelectedWordState.TypeState(action.word)
                        else -> throw IllegalStateException("Invalid simple word selection state")
                    }
            )
            is SelectedWordIntent.OnParsedWordSelected -> currentState.copy(
                    selectedWordState = when (currentSelectedWordState.wordSelectMode) {
                        WordSelectMode.AUTO,
                        WordSelectMode.AUTO_WITH_COLOUR -> SelectedWordState.ParsedState(
                                action.word,
                                action.parsedInfo.dictionaryForm.takeIf { it.isNotBlank() && it != action.word },
                                action.parsedInfo.partsOfSpeech
                                        .filterNot { pos -> pos.isBlank() || pos == "*" }
                                        .takeIf { it.isNotEmpty() }
                                        ?.joinToString(JAPANESE_LIST_DELIMINATOR),
                                action.parsedInfo.pitchAccentPattern?.toString(),
                                currentSelectedWordState.wordSelectMode == WordSelectMode.AUTO_WITH_COLOUR
                        )
                        else -> throw IllegalStateException("Invalid parsed word selection state")
                    }
            )
        }
    }
}