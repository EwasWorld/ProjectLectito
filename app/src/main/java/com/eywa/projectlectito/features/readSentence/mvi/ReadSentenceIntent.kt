package com.eywa.projectlectito.features.readSentence.mvi

import com.eywa.projectlectito.features.readSentence.ParsedInfo
import com.eywa.projectlectito.features.readSentence.WordSelectMode

sealed class ReadSentenceIntent {
    sealed class SelectedWordIntent : ReadSentenceIntent() {
        data class OnWordSelectModeChanged(val wordSelectMode: WordSelectMode) : SelectedWordIntent()
        data class OnSimpleWordSelected(val word: String?) : SelectedWordIntent()
        data class OnParsedWordSelected(val word: String, val parsedInfo: ParsedInfo) : SelectedWordIntent()
        object OnSubmit : SelectedWordIntent()
    }

    data class OnWordSelectModeMenuStateChange(val isOpen: Boolean) : ReadSentenceIntent()

    sealed class WordDefinitionIntent : ReadSentenceIntent() {
        object OnPreviousPressed : WordDefinitionIntent()
        object OnNextPressed : WordDefinitionIntent()
        object OnClosePressed : WordDefinitionIntent()
    }
}