package com.eywa.projectlectito.features.readSentence.mvi

import com.eywa.projectlectito.features.readSentence.ParsedInfo
import com.eywa.projectlectito.features.readSentence.WordSelectMode
import com.eywa.projectlectito.features.readSentence.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionRequester
import com.eywa.projectlectito.utils.JAPANESE_LIST_DELIMINATOR

data class ReadSentenceViewState(
        val selectedWordState: SelectedWordState = SelectedWordState.SelectState(null),
        val isSelectModeMenuOpen: Boolean = false,
        val wordDefinitionState: WordDefinitionState = WordDefinitionState.NoWord
) {
    sealed class SelectedWordState(val wordSelectMode: WordSelectMode, val wordToSearch: String?) {
        data class SelectState(val selectedWord: String?) : SelectedWordState(WordSelectMode.SELECT, selectedWord)

        data class TypeState(val typedWord: String?) : SelectedWordState(WordSelectMode.TYPE, typedWord)

        data class ParsedState(
                val originalWord: String?,
                val parsedInfo: ParsedInfo?,
                val coloured: Boolean
        ) : SelectedWordState(if (!coloured) WordSelectMode.AUTO else WordSelectMode.AUTO_WITH_COLOUR, originalWord) {
            val dictionaryForm = parsedInfo?.dictionaryForm?.takeIf { it.isNotBlank() && it != originalWord }
            val partsOfSpeech = parsedInfo?.partsOfSpeech
                    ?.filterNot { pos -> pos.isBlank() || pos == "*" }
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(JAPANESE_LIST_DELIMINATOR)
            val pitchAccent = parsedInfo?.pitchAccentPattern?.toString()
        }

        fun getAsSelectState() = getDataOrNull<SelectState>()
        fun getAsTypeState() = getDataOrNull<TypeState>()
        fun getAsParsedState() = getDataOrNull<ParsedState>()
    }

    sealed class WordDefinitionState {
        // TODO loading and errors

        object Error : WordDefinitionState()

        object NoWord : WordDefinitionState()

        data class LoadingWord(val requester: WordDefinitionRequester) : WordDefinitionState() {
            override fun cancel() {
                requester.cancelParse()
            }
        }

        data class HasWord(
                private val allDefinitions: List<JishoWordDefinitions.JishoEntry>,
                private val currentIndex: Int
        ) : WordDefinitionState() {
            val word = allDefinitions[currentIndex].let { definition ->
                definition.japanese[0].word.takeIf { !it.isNullOrBlank() } ?: definition.slug
            }
            val reading = allDefinitions[currentIndex].japanese[0].reading
            val isCommon = allDefinitions[currentIndex].is_common
            val jlpt = allDefinitions[currentIndex].jlpt.joinToString(",")
            val tags = allDefinitions[currentIndex].tags.joinToString(",")
            val otherForms = allDefinitions[currentIndex].let { definition ->
                // TODO Stop this from getting too long?
                definition.japanese
                        .takeIf { it.size > 1 }
                        ?.subList(1, definition.japanese.size)
                        ?.joinToString(JAPANESE_LIST_DELIMINATOR) { "${it.word}[${it.reading}]" }
            }

            fun getCurrentDefinition() = allDefinitions[currentIndex]
            override fun hasPreviousEntry() = currentIndex > 0
            override fun hasNextEntry() = currentIndex != 0 && allDefinitions.size < currentIndex + 1
            fun nextEntry() = if (hasNextEntry()) copy(currentIndex = currentIndex + 1) else null
            fun previousEntry() = if (hasPreviousEntry()) copy(currentIndex = currentIndex - 1) else null
        }

        open fun hasPreviousEntry() = false
        open fun hasNextEntry() = false

        open fun cancel() = run { }

        fun getAsHasWord() = getDataOrNull<HasWord>()
        fun getAsError() = getDataOrNull<Error>()
        fun isNoneOrLoading() = (getDataOrNull<NoWord>() ?: getDataOrNull<LoadingWord>()) != null
    }

    companion object {
        private inline fun <reified S> Any?.getDataOrNull(): S? {
            return this?.takeIf { it is S } as? S
        }
    }
}
