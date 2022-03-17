package com.eywa.projectlectito.features.readSentence.mvi

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
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
import com.eywa.projectlectito.utils.UnformattedClickableSpan
import kotlinx.coroutines.Job

data class ReadSentenceViewState(
        val sentenceState: SentenceState = SentenceState.NoSentence,
        val wordDefinitionState: WordDefinitionState = WordDefinitionState.NoWord,
        val selectedWordState: SelectedWordState = SelectedWordState.SelectState(),
        val isSelectModeMenuOpen: Boolean = false,
        val isChoosingSnippetToEdit: Boolean = false,
        val snippetToEditClickableSpan: (Sentence.SnippetInfo) -> ClickableSpan? = { null },
        val parsedWordClickedListener: ((String, ParsedInfo) -> Unit)? = null,
) {
    fun isSentenceSelectable() = selectedWordState is SelectedWordState.SelectState || isChoosingSnippetToEdit

    fun getSentence(): SpannableString? {
        fun SpannableString.setAlternatingColourSpan(span: IntRange, index: Int, @ColorInt color: Int = Color.RED) =
                index.takeIf { it % 2 == 1 }
                        ?.let { setSpan(ForegroundColorSpan(color), span.first, span.last, SPAN_FLAGS) }

        val validSentence = (sentenceState as? SentenceState.ValidSentence) ?: return null
        val wordSelectMode = selectedWordState
        val content = validSentence.sentence.currentSentence ?: return null

        val spannableString = SpannableString(content)
        if (isChoosingSnippetToEdit) {
            /*
             * Select a snippet from a sentence which spans multiple snippets
             */
            val snippets = validSentence.sentence.snippetsInCurrentSentence
            snippets.forEachIndexed { index, snippetInfo ->

//                TODO Span click
//                _snippetToEdit.postValue(
//                        ReadSentenceViewModel.SnippetIdWithStartAndEnd(
//                                snippetInfo.snippetId,
//                                snippetInfo.snippetStartIndex ?: 0,
//                                snippetInfo.snippetEndIndex
//                        )
//                )
//                isInSelectSnippetToEditMode.postValue(false)

                spannableString.setSpan(
                        snippetToEditClickableSpan(snippetInfo)!!,
                        snippetInfo.currentSentenceStartIndex,
                        snippetInfo.currentSentenceEndIndex,
                        SPAN_FLAGS
                )
                spannableString.setAlternatingColourSpan(
                        snippetInfo.currentSentenceStartIndex..snippetInfo.currentSentenceEndIndex,
                        index
                )
            }
            return spannableString
        }

        if (wordSelectMode !is SelectedWordState.ParsedState || !sentenceState.isParseComplete) {
            return spannableString
        }

        /*
         * Select a word from a parsed sentence
         */
        validSentence.parsedInfo!!.forEachIndexed { index, parsedInfo ->
            val spanStartIndex = parsedInfo.startCharacterIndex
            val spanEndIndex = parsedInfo.endCharacterIndex

            spannableString.setSpan(
                    UnformattedClickableSpan {
                        parsedWordClickedListener?.invoke(
                                content.substring(spanStartIndex, spanEndIndex),
                                parsedInfo
                        )
                    },
                    spanStartIndex,
                    spanEndIndex,
                    SPAN_FLAGS
            )
            if (wordSelectMode.coloured) {
                spannableString.setAlternatingColourSpan(spanStartIndex..spanEndIndex, index)
            }
        }

        return spannableString
    }

    sealed class SentenceState {
        object NoSentence : SentenceState()
        object Error : SentenceState()
        data class LoadingSentence(val sentenceJob: Job) : SentenceState() {
            override fun cleanUp() {
                sentenceJob.cancel()
            }

            override fun isJobEqualTo(job: Job) = sentenceJob == job
        }

        data class ValidSentence(
                val sentenceJob: Job,
                val text: Text,
                val currentCharacter: Int? = 0,
                val snippets: List<TextSnippet>,
                val previousSnippetCount: Int = 0,
                val parsedInfo: List<ParsedInfo>? = null,
                val parseError: Boolean = false
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
        fun asValidSentence() = getDataOrNull<ValidSentence>()
    }

    sealed class SelectedWordState(
            val wordSelectMode: WordSelectMode,
            val wordToSearch: String?,
            @StringRes val nullWordSearchedMessage: Int? = null
    ) {
        data class SelectState(
                val selectedWord: String?,
                val selectionStart: Int?,
                val selectionEnd: Int?
        ) : SelectedWordState(
                WordSelectMode.SELECT,
                selectedWord,
                R.string.read_sentence__simple_selected__submit_no_word_select
        ) {
            constructor() : this(null, null, null)
        }

        data class TypeState(val typedWord: String? = null) : SelectedWordState(
                WordSelectMode.TYPE,
                typedWord,
                R.string.read_sentence__simple_selected__submit_no_word_type
        )

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
        private const val SPAN_FLAGS = Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        private inline fun <reified S> Any?.getDataOrNull(): S? {
            return this?.takeIf { it is S } as? S
        }
    }
}
