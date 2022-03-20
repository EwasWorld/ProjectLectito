package com.eywa.projectlectito.features.readSentence.mvi

import androidx.annotation.StringRes
import com.eywa.projectlectito.R
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.features.readSentence.ParsedInfo
import com.eywa.projectlectito.features.readSentence.Sentence
import com.eywa.projectlectito.features.readSentence.WordSelectMode
import com.eywa.projectlectito.features.readSentence.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionRequester
import com.eywa.projectlectito.utils.JAPANESE_LIST_DELIMINATOR
import kotlinx.coroutines.Job

data class ReadSentenceViewState(
        val sentenceState: SentenceState = SentenceState.None,
        val wordDefinitionState: WordDefinitionState = WordDefinitionState.None,
        val wordSelectionState: WordSelectionState = WordSelectionState.SelectMode()
) {
    fun isSentenceSelectable() =
            wordSelectionState is WordSelectionState.SelectMode && !wordSelectionState.isChangeWordSelectionModeMenuOpen

    sealed class SentenceState {
        object None : SentenceState()
        object Error : SentenceState()
        data class Loading(val sentenceJob: Job) : SentenceState() {
            override fun cleanUp() {
                sentenceJob.cancel()
            }

            override fun isJobEqualTo(job: Job) = sentenceJob == job
        }

        data class Valid(
                val sentenceJob: Job,
                val text: Text,
                val currentCharacter: Int? = 0,
                val snippets: List<TextSnippet>,
                val previousSnippetCount: Int = 0,
                val parsedInfo: List<ParsedInfo>? = null,
                val parseError: Boolean = false,
                val isChooseSnippetToEditMenuOpen: Boolean = false
        ) : SentenceState() {
            private val currentSnippet = snippets[previousSnippetCount]
            val sentence = Sentence(
                    currentSnippet,
                    currentCharacter,
                    snippets.take(previousSnippetCount),
                    // -1 for current snippet
                    snippets.takeLast(snippets.size - previousSnippetCount - 1)
            )
            val isParseComplete = !parseError && parsedInfo != null
            val isParseFailed = parseError
            val getTextName = text.name
            val chapterPage = currentSnippet.getChapterPageString()
            val previousSentence = sentence.previousSentence
            val nextSentenceStart = sentence.getNextSentenceStart()
            val previousSentenceStart = sentence.getPreviousSentenceStart()

            override fun cleanUp() {
                sentenceJob.cancel()
            }

            override fun isJobEqualTo(job: Job) = sentenceJob == job
        }

        open fun cleanUp() = run { }
        open fun isJobEqualTo(job: Job) = false
        fun asValid() = getDataOrNull<Valid>()
    }

    sealed class WordSelectionState(
            val wordSelectMode: WordSelectMode,
            val wordToSearch: String?,
            @StringRes val nullWordSearchedMessage: Int? = null
    ) {
        /**
         * Defaults to false as changing word selection state should only happen while the menu is open.
         * On changing to a new state, the menu should be closed.
         */
        abstract val isChangeWordSelectionModeMenuOpen: Boolean

        data class SelectMode(
                val selectedWord: String?,
                val selectionStart: Int?,
                val selectionEnd: Int?,
                override val isChangeWordSelectionModeMenuOpen: Boolean = false
        ) : WordSelectionState(
                WordSelectMode.SELECT,
                selectedWord,
                R.string.read_sentence__simple_selected__submit_no_word_select
        ) {
            constructor() : this(null, null, null)

            override fun copyAbstract(isChangeWordSelectionModeMenuOpen: Boolean?): WordSelectionState {
                return copy(
                        isChangeWordSelectionModeMenuOpen = isChangeWordSelectionModeMenuOpen
                                ?: this.isChangeWordSelectionModeMenuOpen
                )
            }
        }

        data class TypeMode(
                val typedWord: String? = null,
                override val isChangeWordSelectionModeMenuOpen: Boolean = false
        ) : WordSelectionState(
                WordSelectMode.TYPE,
                typedWord,
                R.string.read_sentence__simple_selected__submit_no_word_type
        ) {
            override fun copyAbstract(isChangeWordSelectionModeMenuOpen: Boolean?): WordSelectionState {
                return copy(
                        isChangeWordSelectionModeMenuOpen = isChangeWordSelectionModeMenuOpen
                                ?: this.isChangeWordSelectionModeMenuOpen
                )
            }
        }

        data class ParsedMode(
                val originalWord: String?,
                val parsedInfo: ParsedInfo?,
                val coloured: Boolean,
                override val isChangeWordSelectionModeMenuOpen: Boolean = false
        ) : WordSelectionState(if (!coloured) WordSelectMode.AUTO else WordSelectMode.AUTO_WITH_COLOUR, originalWord) {
            val dictionaryForm = parsedInfo?.dictionaryForm?.takeIf { it.isNotBlank() && it != originalWord }
            val partsOfSpeech = parsedInfo?.partsOfSpeech
                    ?.filterNot { pos -> pos.isBlank() || pos == "*" }
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(JAPANESE_LIST_DELIMINATOR)
            val pitchAccent = parsedInfo?.pitchAccentPattern?.toString()

            override fun copyAbstract(isChangeWordSelectionModeMenuOpen: Boolean?): WordSelectionState {
                return copy(
                        isChangeWordSelectionModeMenuOpen = isChangeWordSelectionModeMenuOpen
                                ?: this.isChangeWordSelectionModeMenuOpen
                )
            }
        }

        fun asSelectMode() = getDataOrNull<SelectMode>()
        fun asTypeMode() = getDataOrNull<TypeMode>()
        fun asParsedMode() = getDataOrNull<ParsedMode>()

        abstract fun copyAbstract(isChangeWordSelectionModeMenuOpen: Boolean? = null): WordSelectionState
    }

    sealed class WordDefinitionState {
        object None : WordDefinitionState()

        // TODO loading and errors
        object Error : WordDefinitionState()

        data class Loading(val requester: WordDefinitionRequester) : WordDefinitionState() {
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
                        ?.mapNotNull {
                            when {
                                !it.word.isNullOrBlank() && it.reading.isNotBlank() -> "${it.word}[${it.reading}]"
                                it.word.isNullOrBlank() -> it.reading
                                it.reading.isBlank() -> it.word
                                else -> null
                            }
                        }
                        ?.joinToString(JAPANESE_LIST_DELIMINATOR) { it }
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

        fun asHasWord() = getDataOrNull<HasWord>()
        fun asError() = getDataOrNull<Error>()
        fun hasNoDefinition() = asHasWord() == null && asError() == null
    }

    companion object {
        private inline fun <reified S> Any?.getDataOrNull(): S? {
            return this?.takeIf { it is S } as? S
        }
    }
}
