package com.eywa.projectlectito.features.readSentence.mvi

import com.eywa.projectlectito.features.readSentence.WordSelectMode

data class ReadSentenceViewState(
        val selectedWordState: SelectedWordState = SelectedWordState.SelectState(null),
        val isSelectModeMenuOpen: Boolean = false
) {
    sealed class SelectedWordState(val wordSelectMode: WordSelectMode) {
        data class SelectState(val selectedWord: String?) : SelectedWordState(WordSelectMode.SELECT)
        data class TypeState(val selectedWord: String?) : SelectedWordState(WordSelectMode.TYPE)
        data class ParsedState(
                val originalWord: String?,
                val dictionaryForm: String?,
                val partsOfSpeech: String?,
                val pitchAccent: String?,
                val coloured: Boolean
        ) : SelectedWordState(if (!coloured) WordSelectMode.AUTO else WordSelectMode.AUTO_WITH_COLOUR)

        private inline fun <reified S : SelectedWordState> getDataOrNull(): S? {
            return takeIf { it is S } as? S
        }

        fun getAsSelectState() = getDataOrNull<SelectState>()
        fun getAsTypeState() = getDataOrNull<TypeState>()
        fun getAsParsedState() = getDataOrNull<ParsedState>()
    }
}
