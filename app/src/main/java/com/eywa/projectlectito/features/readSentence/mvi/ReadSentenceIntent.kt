package com.eywa.projectlectito.features.readSentence.mvi

import com.eywa.projectlectito.features.readSentence.WordSelectMode

sealed class ReadSentenceIntent {
    sealed class SentenceIntent : ReadSentenceIntent() {
        data class Initialise(
                val currentSnippetId: Int?,
                val currentCharacter: Int?,
                val textId: Int
        ) : SentenceIntent()

        object OnNextSentenceClicked : SentenceIntent()
        object OnPreviousSentenceClicked : SentenceIntent()
        object OnEditSentenceClicked : SentenceIntent()
        object OnEditOverlayClicked : SentenceIntent()
        object OnViewFullTextClicked : SentenceIntent()
    }

    sealed class SelectedWordIntent : ReadSentenceIntent() {
        data class OnWordSelectModeChanged(val wordSelectMode: WordSelectMode) : SelectedWordIntent()
        data class OnSpanSelected(val start: Int, val end: Int) : SelectedWordIntent()
        data class OnSimpleWordSelected(val word: String?) : SelectedWordIntent()
        object OnSubmit : SelectedWordIntent()
    }

    data class OnWordSelectModeMenuStateChange(val isOpen: Boolean) : ReadSentenceIntent()

    sealed class WordDefinitionIntent : ReadSentenceIntent() {
        object OnPreviousPressed : WordDefinitionIntent()
        object OnNextPressed : WordDefinitionIntent()
        object OnClosePressed : WordDefinitionIntent()
    }
}