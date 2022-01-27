package com.eywa.projectlectito.readSentence

import android.util.Log
import com.atilika.kuromoji.unidic.kanaaccent.Tokenizer
import kotlinx.coroutines.*

class Sentence(
        private val textSnippetContent: String?,
        /**
         * Current location in [textSnippetContent]
         */
        currentCharacter: Int? = 0,
        private val previousSnippet: String? = "",
        private val nextSnippet: String? = "",
        private val parserSuccessCallback: (List<ParsedInfo>) -> Unit,
        private val parserFailCallback: (Throwable) -> Unit
) {
    companion object {
        private const val LOG_TAG = "Sentence"
        private val nonDisplayCharacters = setOf('\n')

        /**
         * Don't count these as either stop or non-stop characters. They don't indicate text and don't indicate a
         * sentence stop
         */
        private val ignoreCharacters = setOf(' ', '　', '「', '」', '"', '”', ',', '、')

        /**
         * Break a sentence unless it's the start or end of a file, in which case allow the sentence to flow between
         * snippets
         */
        private val softSentenceStops = setOf('\n')

        /**
         * Always breaks a sentence
         */
        private val hardSentenceStops = setOf('.', '。', '?', '？')
        private val sentenceStops = hardSentenceStops.plus(softSentenceStops)

        private val tokenizer by lazy { Tokenizer() }
    }

    private lateinit var parseJob: Job

    /**
     * Can be in any [SnippetLocation]
     */
    var currentSentenceStart = IndexInfo(currentCharacter ?: 0, SnippetLocation.CURRENT)
        private set

    /**
     * Must be in [SnippetLocation.CURRENT] or [SnippetLocation.NEXT]
     */
    var nextSentenceStart: IndexInfo? = null
        private set

    /**
     * Must be in [SnippetLocation.CURRENT] or [SnippetLocation.PREVIOUS]
     */
    var previousSentenceStart: IndexInfo? = null
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
        require(!textSnippetContent.isNullOrBlank()) { "Content is null or blank" }

        // Calculate boundaries
        setCurrentSentenceStart()
        setPreviousSentenceStart()
        nextSentenceStart = findNextSentenceStart(currentSentenceStart)

        // Set sentences
        previousSentenceStart?.let { prevStart ->
            previousSentence = extractSentence(prevStart, currentSentenceStart)
        }
        currentSentence = extractSentence(currentSentenceStart, nextSentenceStart)
    }

    /**
     * Ensures [currentSentenceStart] is at the start of a sentence.
     *
     * Searches backwards unless:
     * - [currentSentenceStart] is currently on a [sentenceStops]
     * - [textSnippetContent] is empty
     * - [currentSentenceStart] is larger than [textSnippetContent.length]
     *
     * Throws an error if:
     * - [currentSentenceStart] != [SnippetLocation.CURRENT]
     * - [currentSentenceStart] < 0
     * - No non-stop characters on or after [currentSentenceStart]
     */
    private fun setCurrentSentenceStart() {
        require(currentSentenceStart.snippet == SnippetLocation.CURRENT) {
            "Current start must be in current snippet"
        }
        require(currentSentenceStart.index >= 0) { "Sentence start cannot be < 0" }

        if (!textSnippetContent.isNullOrBlank() && currentSentenceStart.index < textSnippetContent.length
                && !sentenceStops.contains(textSnippetContent[currentSentenceStart.index])
        ) {
            currentSentenceStart =
                    findPreviousSentenceStart(currentSentenceStart.snippet, currentSentenceStart.index)
                            ?: currentSentenceStart
            return
        }

        /*
         * Check forwards if currently on a sentence end char
         */
        if (!textSnippetContent.isNullOrBlank() && currentSentenceStart.index < textSnippetContent.length) {
            val startIndex = FindType.FIRST_NON_STOP.find(currentSentenceStart.index)
            if (startIndex != null) {
                currentSentenceStart = IndexInfo(startIndex, SnippetLocation.CURRENT)
                return
            }
        }

        // No non-stop chars so check the next snippet
        var snippet = currentSentenceStart.snippet.getNext()
        while (snippet != null) {
            val startIndex = FindType.FIRST_NON_STOP.find(snippetLocation = snippet)
            if (startIndex != null) {
                currentSentenceStart = IndexInfo(startIndex, SnippetLocation.NEXT)
            }
            snippet = snippet.getNext()
        }
        throw IllegalArgumentException("No non-stop chars after current character")
    }

    private fun findPreviousSentenceStart(snippetLocation: SnippetLocation?, endIndex: Int? = null): IndexInfo? {
        if (snippetLocation == null) return null

        if (snippetLocation.get().isNullOrBlank()) {
            return findPreviousSentenceStart(snippetLocation.getPrevious())
        }

        // Check that this has non-stop characters
        var lastNonStopChar: Int? = FindType.LAST_NON_STOP.find(
                endIndex = endIndex?.plus(1),
                snippetLocation = snippetLocation
        ) ?: return findPreviousSentenceStart(snippetLocation.getPrevious())

        // Is there a hard stop?
        var hardStop = FindType.LAST_STOP.find(
                startIndex = lastNonStopChar,
                endIndex = endIndex?.plus(1),
                stopChars = hardSentenceStops,
                snippetLocation = snippetLocation
        )
        if (hardStop != null) {
            return null
        }

        // Find the last stop character
        val lastStopCharacter = FindType.LAST_STOP.find(
                endIndex = lastNonStopChar,
                snippetLocation = snippetLocation
        ) ?: return findPreviousSentenceStart(snippetLocation.getPrevious())
                ?: IndexInfo(0, snippetLocation)

        // Is that the last stop character in the string?
        lastNonStopChar = FindType.LAST_NON_STOP.find(
                endIndex = lastStopCharacter,
                snippetLocation = snippetLocation
        )
        if (lastNonStopChar != null) {
            return IndexInfo(lastStopCharacter + 1, snippetLocation)
        }

        // Is there a hard stop?
        hardStop = FindType.LAST_STOP.find(
                endIndex = lastStopCharacter + 1,
                stopChars = hardSentenceStops,
                snippetLocation = snippetLocation
        )
        if (hardStop != null) {
            return IndexInfo(lastStopCharacter + 1, snippetLocation)
        }

        return findPreviousSentenceStart(snippetLocation.getPrevious())
                ?: IndexInfo(lastStopCharacter + 1, snippetLocation)
    }

    private fun setPreviousSentenceStart() {
        val previousEnd = previousSentenceEnd(currentSentenceStart.snippet, currentSentenceStart.index - 1)
                ?: return
        previousSentenceStart = findPreviousSentenceStart(previousEnd.snippet, previousEnd.index)
    }

    private fun previousSentenceEnd(snippet: SnippetLocation?, index: Int? = null): IndexInfo? {
        if (snippet == null) return null

        if (index != null && index < 0) {
            return previousSentenceEnd(snippet.getPrevious())
        }
        if (snippet.get().isNullOrBlank()) {
            return previousSentenceEnd(snippet.getPrevious())
        }

        val lastNonStop = FindType.LAST_NON_STOP.find(
                endIndex = index,
                snippetLocation = snippet
        ) ?: return previousSentenceEnd(snippet.getPrevious())

        return IndexInfo(lastNonStop, snippet)
    }

    private fun findNextSentenceStart(searchForwardFrom: IndexInfo): IndexInfo? {
        val nextSnippet = searchForwardFrom.snippet.getNext()

        // Check that not at the end of snippet
        if (searchForwardFrom.index >= searchForwardFrom.snippet.get()?.length ?: -1) {
            if (nextSnippet == null) return null
            return findNextSentenceStart(IndexInfo(0, nextSnippet))
        }

        // Find the end of the current sentence
        val sentenceEnd = FindType.FIRST_STOP.find(
                startIndex = searchForwardFrom.index,
                snippetLocation = searchForwardFrom.snippet
        )
        if (sentenceEnd == null) {
            if (nextSnippet == null) return null
            return findNextSentenceStart(IndexInfo(0, nextSnippet))
        }

        val nextSentenceStart = FindType.FIRST_NON_STOP.find(
                startIndex = sentenceEnd,
                snippetLocation = searchForwardFrom.snippet
        )
        if (nextSentenceStart != null) {
            return IndexInfo(nextSentenceStart, searchForwardFrom.snippet)
        }

        // No more non stops in this snippet, check for hard stop
        val hardStop = FindType.FIRST_STOP.find(
                startIndex = sentenceEnd,
                stopChars = hardSentenceStops,
                snippetLocation = searchForwardFrom.snippet
        )
        if (hardStop == null) {
            if (nextSnippet == null) return null
            return findNextSentenceStart(IndexInfo(0, nextSnippet))
        }
        return findFirstNonStopCharacter(nextSnippet)
    }

    private fun findFirstNonStopCharacter(snippet: SnippetLocation?): IndexInfo? {
        if (snippet == null) return null
        val firstNonStop = FindType.FIRST_NON_STOP.find(
                startIndex = 0,
                snippetLocation = snippet
        ) ?: return findFirstNonStopCharacter(snippet.getNext())
        return IndexInfo(firstNonStop, snippet)
    }

    private fun extractSentence(start: IndexInfo, end: IndexInfo?): String {
        if (end != null && start.snippet == end.snippet) {
            return start.snippet.get()!!.substring(start.index, end.index).prepareForDisplay()
        }

        var tempSentence = start.snippet.get()!!.substring(start.index).prepareForDisplay()
        var middleSnippet = start.snippet.getNext()
        while (middleSnippet != null && middleSnippet != end?.snippet) {
            tempSentence += middleSnippet.get()?.prepareForDisplay() ?: ""
            middleSnippet = middleSnippet.getNext()
        }
        if (end != null) {
            tempSentence += end.snippet.get()!!.substring(0, end.index).prepareForDisplay()
        }
        return tempSentence
    }

    private fun String?.prepareForDisplay() = this?.filterNot { nonDisplayCharacters.contains(it) }?.trim() ?: ""

    private fun FindType.find(
            startIndex: Int? = null,
            endIndex: Int? = null,
            stopChars: Set<Char> = sentenceStops,
            snippetLocation: SnippetLocation = SnippetLocation.CURRENT
    ): Int? {
        val snippet = snippetLocation.get()

        require(!snippet.isNullOrBlank()) { "Content is null or blank" }
        require(startIndex == null || startIndex >= 0 && startIndex < snippet.length) {
            "Start index not within bounds of snippet"
        }

        val fromIndex = startIndex ?: 0
        val toIndex = endIndex ?: snippet.length

        val substring = snippet.substring(fromIndex, toIndex)

        val lastIndex = this.findFunction(substring, stopChars)
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

            var currentIndex = 0
            val parsedInfo = tokens.map { token ->
                val startIndex = currentIndex
                val endIndex = currentIndex + token.surface.length
                val partsOfSpeech = listOfNotNull(
                        token.partOfSpeechLevel1,
                        token.partOfSpeechLevel2,
                        token.partOfSpeechLevel3,
                        token.partOfSpeechLevel4
                )
                var accentType: Int? = null
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

    fun substring(start: Int, end: Int): String? {
        return currentSentence?.substring(start, end)
    }

    private fun SnippetLocation.get(): String? {
        return when (this) {
            SnippetLocation.CURRENT -> textSnippetContent
            SnippetLocation.PREVIOUS -> previousSnippet
            SnippetLocation.NEXT -> nextSnippet
        }
    }

    data class IndexInfo(val index: Int, val snippet: SnippetLocation)

    enum class SnippetLocation {
        PREVIOUS, CURRENT, NEXT;

        fun getPrevious() = when (this) {
            PREVIOUS -> null
            CURRENT -> PREVIOUS
            NEXT -> CURRENT
        }

        fun getNext() = when (this) {
            PREVIOUS -> CURRENT
            CURRENT -> NEXT
            NEXT -> null
        }
    }

    private enum class FindType(val findFunction: (String, Set<Char>) -> Int) {
        FIRST_STOP({ substring, stopChars -> substring.indexOfFirst { stopChars.contains(it) } }
        ),
        FIRST_NON_STOP({ substring, stopChars ->
            substring.indexOfFirst { !stopChars.contains(it) && !ignoreCharacters.contains(it) }
        }),
        LAST_STOP({ substring, stopChars ->
            substring.lastIndexOfAny(stopChars.joinToString("").toCharArray())
        }),
        LAST_NON_STOP({ substring, stopChars ->
            substring.indexOfLast { !stopChars.contains(it) && !ignoreCharacters.contains(it) }
        })
    }
}