package com.eywa.projectlectito.readSentence

import android.util.Log
import com.atilika.kuromoji.unidic.kanaaccent.Tokenizer
import com.eywa.projectlectito.database.snippets.ParsedInfo
import kotlinx.coroutines.*

class Sentence(
        private val textSnippetContent: String?,
        currentCharacter: Int? = 0,
        private val parserSuccessCallback: (List<ParsedInfo>) -> Unit,
        private val parserFailCallback: (Throwable) -> Unit
) {
    companion object {
        private const val LOG_TAG = "Sentence"
        private val stringsToRemoveFromDisplay = setOf('\n')
        private val sentenceBreakStrings = setOf("。").plus(stringsToRemoveFromDisplay.map { it.toString() })

        private val tokenizer by lazy { Tokenizer() }
    }

    private lateinit var parseJob: Job

    var currentSentenceStart = currentCharacter ?: 0
        private set
    var nextSentenceStart: Int? = null
        private set
    var previousSentenceStart: Int? = null
        private set

    var currentSentence: String? = null
        private set
    var previousSentence: String? = null
        private set

    init {
        initJob()
        calculateSentenceBoundaries()
    }

    private fun initJob() {
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
        if (textSnippetContent.isNullOrBlank()) {
            return
        }

        if (currentSentenceStart < 0 || currentSentenceStart >= textSnippetContent.length) {
            return
        }

        /*
         * Ensure the current character is the start of a sentence
         */
        if (sentenceBreakStrings.contains(textSnippetContent[currentSentenceStart].toString())) {
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

                previousSentence = textSnippetContent.substring(previousSentenceStart!!, currentSentenceStart)
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
            newSnippet = textSnippetContent.substring(currentSentenceStart)
        }
        else {
            newSnippet = textSnippetContent.substring(currentSentenceStart, nextSentenceStart!!)
        }

        currentSentence = newSnippet.filterNot { stringsToRemoveFromDisplay.contains(it) }
    }

    private fun findIndexOfFirstBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent.length

        val substring = textSnippetContent.substring(fromIndex, toIndex)

        val firstIndex = substring.indexOfAny(sentenceBreakStrings)
        if (firstIndex == -1) {
            return null
        }
        return firstIndex + fromIndex
    }

    private fun findIndexOfFirstNonBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent.length

        val substring = textSnippetContent.substring(fromIndex, toIndex)

        val firstIndex = substring.indexOfFirst { !sentenceBreakStrings.contains(it.toString()) }
        if (firstIndex == -1) {
            return null
        }
        return firstIndex + fromIndex
    }

    private fun findIndexOfLastBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent.length

        val substring = textSnippetContent.substring(fromIndex, toIndex)

        val lastIndex = substring.lastIndexOfAny(sentenceBreakStrings)
        if (lastIndex == -1) {
            return null
        }
        return lastIndex + fromIndex
    }

    private fun findIndexOfLastNonBreakChar(startIndex: Int? = null, endIndex: Int? = null): Int? {
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: textSnippetContent.length

        val substring = textSnippetContent.substring(fromIndex, toIndex)

        val lastIndex = substring.indexOfLast { !sentenceBreakStrings.contains(it.toString()) }
        if (lastIndex == -1) {
            return null
        }
        return lastIndex + fromIndex
    }

    suspend fun startParse() {
        withContext(parseJob) {
            Log.d(LOG_TAG, "Parse invoked")

            if (currentSentence.isNullOrBlank()) {
                return@withContext
            }

            // Can be a long operation
            val tokens = tokenizer.tokenize(currentSentence)
            Log.d(LOG_TAG, "Finished parsing")

            // Check coroutine hasn't been cancelled
            ensureActive()

            var currentIndex = currentSentenceStart
            val parsedInfo = tokens.map { token ->
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

            Log.d(LOG_TAG, "Calling parser success callback")
            parserSuccessCallback(parsedInfo)
        }
    }

    fun cancelParse() {
        // TODO Do I need this guard?
        if (parseJob.isActive || parseJob.isCompleted) {
            parseJob.cancel(CancellationException("New job created"))
        }
    }
}