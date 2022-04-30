package com.eywa.projectlectito.features.readSentence.mvi

import android.content.res.Resources
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
        val wordSelectionState: WordSelectionState = WordSelectionState.ManualMode.TypeMode(),
        /**
         * Not currently working :(
         */
        val isWordDefinitionExpanded: Boolean = true
) {
    fun isSentenceSelectable() =
            wordSelectionState is WordSelectionState.ManualMode && !wordSelectionState.isChangeWordSelectionModeMenuOpen

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
            val snippetPercentageProgress = sentence.getSnippetPercentageProgress()
            val startOfSnippetBannerText = sentence.getNewSnippetStarted()?.getPageStringWithName()

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

        sealed class ManualMode(
                wordToSearch: String?
        ) : WordSelectionState(
                WordSelectMode.MANUAL,
                wordToSearch,
                R.string.read_sentence__simple_selected__submit_manual_no_word
        ) {
            data class SelectMode(
                    val selectedWord: String?,
                    val selectionStart: Int?,
                    val selectionEnd: Int?,
                    override val isChangeWordSelectionModeMenuOpen: Boolean = false
            ) : ManualMode(selectedWord) {
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
            ) : ManualMode(typedWord) {
                override fun copyAbstract(isChangeWordSelectionModeMenuOpen: Boolean?): WordSelectionState {
                    return copy(
                            isChangeWordSelectionModeMenuOpen = isChangeWordSelectionModeMenuOpen
                                    ?: this.isChangeWordSelectionModeMenuOpen
                    )
                }
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

        fun asManualMode() = getDataOrNull<ManualMode>()
        fun asSelectMode() = getDataOrNull<ManualMode.SelectMode>()
        fun asParsedMode() = getDataOrNull<ParsedMode>()

        abstract fun copyAbstract(isChangeWordSelectionModeMenuOpen: Boolean? = null): WordSelectionState
    }

    sealed class WordDefinitionState {
        object None : WordDefinitionState()

        // TODO loading states

        data class Error(private val errorType: ErrorType = ErrorType.UNKNOWN) : WordDefinitionState() {
            override fun getMessage(resources: Resources): String {
                return resources.getString(this.errorType.messageId)
            }

            override fun hasMessage() = true
        }

        enum class ErrorType(@StringRes val messageId: Int) {
            NO_INTERNET(R.string.err_read_sentence__definition_fetch_internet_error),
            NO_DEFINITIONS(R.string.err_read_sentence__definition_fetch_no_defs_error),
            UNKNOWN(R.string.err_read_sentence__definition_fetch_error)
        }

        data class Loading(val word: String, val requester: WordDefinitionRequester) : WordDefinitionState() {
            override fun cancel() {
                requester.cancelParse()
            }

            override fun getMessage(resources: Resources): String {
                return resources.getString(R.string.read_sentence__loading_definition).format(word)
            }

            override fun hasMessage() = true
        }

        data class HasWord(
                private val allDefinitions: List<JishoWordDefinitions.JishoEntry>,
                val selectedIndex: Int
        ) : WordDefinitionState() {
            fun toViewPagerInfo() = ViewPagerInfo(allDefinitions)
            val size = allDefinitions.size

            /**
             * Helper class to isolate allDefinitions from other info about the current definition
             */
            data class ViewPagerInfo(private val allDefinitions: List<JishoWordDefinitions.JishoEntry>) {
                val size = allDefinitions.size
                fun getDataForIndex(index: Int) = JishoEntryForDisplay(allDefinitions[index])
            }

            class JishoEntryForDisplay(data: JishoWordDefinitions.JishoEntry) {
                val word = data.let { definition ->
                    definition.japanese[0].word.takeIf { !it.isNullOrBlank() } ?: definition.slug
                }
                val reading = data.japanese[0].reading
                val isCommon = data.is_common
                val jlpt = data.jlpt.joinToString(",")
                val tags = data.tags.joinToString(",")
                val otherForms = data.let { definition ->
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
                val senses = data.senses
            }
        }


        open fun cancel() = run { }

        /**
         * Returns true if this state has a general message to display
         * (no definition message is handled by [ReadSentenceViewState.WordDefinitionState.hasNoDefinition])
         * (not used by [ReadSentenceViewState.WordDefinitionState.HasWord])
         *
         * If this is true then [ReadSentenceViewState.WordDefinitionState.getMessage] must be overridden
         */
        open fun hasMessage() = false

        /**
         * A general message that this state wants to display
         *
         * @throws NotImplementedError if [ReadSentenceViewState.WordDefinitionState.hasMessage] is true
         */
        open fun getMessage(resources: Resources) = if (hasMessage()) {
            throw NotImplementedError("General message not implemented for " + this::class.simpleName)
        }
        else {
            ""
        }

        fun asHasWord() = getDataOrNull<HasWord>()
        fun hasNoDefinition() = asHasWord() == null && this !is Error
    }

    companion object {
        private inline fun <reified S> Any?.getDataOrNull(): S? {
            return this?.takeIf { it is S } as? S
        }
    }
}
