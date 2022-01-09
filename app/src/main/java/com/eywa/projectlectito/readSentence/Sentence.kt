package com.eywa.projectlectito.readSentence

import android.util.Log
import com.atilika.kuromoji.unidic.kanaaccent.Tokenizer
import com.eywa.projectlectito.database.snippets.ParsedInfo
import kotlinx.coroutines.*

class Sentence(
        private val parserSuccessCallback: (List<ParsedInfo>) -> Unit,
        private val parserFailCallback: (Throwable) -> Unit
) {
    companion object {
        private const val LOG_TAG = "Sentence"
        private val stringsToRemoveFromDisplay = setOf('\n')
        private val sentenceBreakStrings = setOf("。").plus(stringsToRemoveFromDisplay.map { it.toString() })

        private val tokenizer by lazy { Tokenizer() }
    }

    var textSnippetContent: String? = null
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    private var currentCharacter: Int = 0

    private var hasBeenCalculated = false
    private lateinit var parseJob: Job

    var currentSentenceStart: Int = 0
        private set
    var nextSentenceStart: Int? = null
        private set
    var previousSentenceStart: Int? = null
        private set

    var currentSentence: String? = null
        private set
    var previousSentence: String? = null
        private set

    var parsedInfo: List<ParsedInfo>? = null
        private set

    init {
        reset()
    }

    fun setCurrentCharacter(value: Int?) {
        if (currentCharacter != value ?: 0) {
            currentCharacter = value ?: 0
            reset()
        }
    }

    private fun reset() {
        hasBeenCalculated = false
        initJob()
        calculateSentenceBoundaries()
        startParse()
    }

    private fun initJob() {
        if (::parseJob.isInitialized && (parseJob.isActive || parseJob.isCompleted)) {
            parseJob.cancel(CancellationException("New job created"))
        }

        parseJob = Job()
        parseJob.invokeOnCompletion {
            it?.let { e ->
                if (e is CancellationException) {
                    Log.d(LOG_TAG, "Parse job cancelled")
                    return@invokeOnCompletion
                }

                var message = e.message
                if (message.isNullOrBlank()) {
                    message = "Unknown cancellation error"
                }
                Log.e(LOG_TAG, message)

                parserFailCallback(e)

            }
        }
    }

    private fun calculateSentenceBoundaries() {
        if (hasBeenCalculated) {
            return
        }
        currentSentenceStart = currentCharacter
        nextSentenceStart = null
        previousSentenceStart = null
        currentSentence = null
        previousSentence = null
        parsedInfo = null

        try {
            if (textSnippetContent.isNullOrBlank()) {
                return
            }

            textSnippetContent?.let { textSnippet ->
                if (currentSentenceStart < 0 || currentSentenceStart >= textSnippet.length) {
                    return
                }

                /*
                 * Ensure the current character is the start of a sentence
                 */
                if (sentenceBreakStrings.contains(textSnippet[currentSentenceStart].toString())) {
                    // We're on a sentence end character, check forwards
                    currentSentenceStart = findIndexOfFirstNonBreakChar(currentSentenceStart)
                            // No non-break chars forwards so check backwards
                            ?: findIndexOfLastNonBreakChar(endIndex = currentSentenceStart)
                                    // No non-break chars anywhere so fail
                                    ?: return
                }
                else {
                    // plus(1) because the index is that of a sentence end character
                    currentSentenceStart = findIndexOfLastBreakChar(endIndex = currentSentenceStart)?.plus(1)
                            // If none are found, leave it where it is (we're already on a non-break char)
                            ?: currentSentenceStart
                }

                /*
                 * Find the start of the previous sentence
                 */
                if (currentSentenceStart != 0) {
                    // Ignore any sentence end characters directly before the current character
                    val previousSentenceEnd = findIndexOfLastNonBreakChar(endIndex = currentSentenceStart)
                    if (previousSentenceEnd != null) {
                        // plus(1) because the index is that of a sentence end character
                        previousSentenceStart = findIndexOfLastBreakChar(endIndex = previousSentenceEnd)?.plus(1) ?: 0

                        previousSentence = textSnippet.substring(previousSentenceStart!!, currentSentenceStart)
                                .filterNot { stringsToRemoveFromDisplay.contains(it) }
                    }
                }

                /*
                 * Find the start of the next sentence
                 */
                nextSentenceStart = findIndexOfFirstBreakChar(currentSentenceStart)

                if (nextSentenceStart != null) {
                    // Skip past all sentence ending characters
                    nextSentenceStart = findIndexOfFirstNonBreakChar(nextSentenceStart)
                }

                val newSnippet: String
                if (nextSentenceStart == null) {
                    newSnippet = textSnippet.substring(currentSentenceStart)
                }
                else {
                    newSnippet = textSnippet.substring(currentSentenceStart, nextSentenceStart!!)
                }

                currentSentence = newSnippet.filterNot { stringsToRemoveFromDisplay.contains(it) }
            }
        }
        finally {
            hasBeenCalculated = true
        }
    }

    private fun findIndexOfFirstBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent!!.length

        textSnippetContent?.let { snippet ->
            val substring = snippet.substring(fromIndex, toIndex)

            val firstIndex = substring.indexOfAny(sentenceBreakStrings)
            if (firstIndex == -1) {
                return null
            }
            return firstIndex + fromIndex
        }

        throw IllegalStateException("Snippet is null")
    }

    private fun findIndexOfFirstNonBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent!!.length

        textSnippetContent?.let { snippet ->
            val substring = snippet.substring(fromIndex, toIndex)

            val firstIndex = substring.indexOfFirst { !sentenceBreakStrings.contains(it.toString()) }
            if (firstIndex == -1) {
                return null
            }
            return firstIndex + fromIndex
        }

        throw IllegalStateException("Snippet is null")
    }

    private fun findIndexOfLastBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent!!.length

        textSnippetContent?.let { snippet ->
            val substring = snippet.substring(fromIndex, toIndex)

            val lastIndex = substring.lastIndexOfAny(sentenceBreakStrings)
            if (lastIndex == -1) {
                return null
            }
            return lastIndex + fromIndex
        }

        throw IllegalStateException("Snippet is null")
    }

    private fun findIndexOfLastNonBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent!!.length

        textSnippetContent?.let { snippet ->
            val substring = snippet.substring(fromIndex, toIndex)

            val lastIndex = substring.indexOfLast { !sentenceBreakStrings.contains(it.toString()) }
            if (lastIndex == -1) {
                return null
            }
            return lastIndex + fromIndex
        }

        throw IllegalStateException("Snippet is null")
    }

    private fun startParse() {
        if (currentSentence == null) {
            return
        }

        // TODO Launch the coroutine from the view model
        CoroutineScope(Dispatchers.Default + parseJob).launch {
            Log.d(LOG_TAG, "Parse invoked")

            // Can be a long operation
            val tokens = tokenizer.tokenize(currentSentence)
            Log.d(LOG_TAG, "Finished parsing")

            // Check coroutine hasn't been cancelled
            ensureActive()

            var currentIndex = currentSentenceStart
            parsedInfo = tokens.map { token ->
                val startIndex = currentIndex
                val endIndex = currentIndex + token.surface.length
                val partsOfSpeech = listOfNotNull(
                        token.partOfSpeechLevel1,
                        token.partOfSpeechLevel2,
                        token.partOfSpeechLevel3,
                        token.partOfSpeechLevel4
                )
                var accentType: Int = -1
                try {
                    accentType = Integer.parseInt(token.accentType)
                }
                catch (e: NumberFormatException) {
                }
                currentIndex = endIndex

                ParsedInfo(1, startIndex, 1, endIndex, token.writtenBaseForm, partsOfSpeech, accentType)
            }
            ensureActive()

            Log.d(LOG_TAG, "Calling success callback")
            parserSuccessCallback(parsedInfo!!)
        }
    }
}