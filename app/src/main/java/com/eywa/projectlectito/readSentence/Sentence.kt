package com.eywa.projectlectito.readSentence

import android.util.Log
import com.atilika.kuromoji.unidic.kanaaccent.Tokenizer
import com.eywa.projectlectito.database.snippets.TextSnippet
import kotlinx.coroutines.*
import kotlin.math.abs

class Sentence(
        private val currentSnippet: TextSnippet?,
        /**
         * Current location in [currentSnippet]
         */
        currentCharacter: Int? = 0,
        private val previousSnippets: List<TextSnippet>? = null,
        private val nextSnippets: List<TextSnippet>? = null,
        private val parserSuccessCallback: (List<ParsedInfo>) -> Unit,
        private val parserFailCallback: (Throwable) -> Unit
) {
    companion object {
        private const val LOG_TAG = "Sentence"

        private const val CURRENT_SNIPPET_RELATIVE_ID = 0

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

    private var currentSentenceStart = RelativeIndexInfo(currentCharacter ?: 0, CURRENT_SNIPPET_RELATIVE_ID)
    private var nextSentenceStart: RelativeIndexInfo? = null
    private var previousSentenceStart: RelativeIndexInfo? = null

    /**
     * Details of the snippets in this sentence
     * Triple<start char in first snippet, end char in last snippet (null for entire last snippet), all snippet ids)
     */
    val snippetsInCurrentSentence: List<SnippetInfo>
        get() {
            var currentRelativeId = currentSentenceStart.relativeSnippet
            val allSnippets = mutableListOf<SnippetInfo>()
            var currentSentenceIndex = 0
            do {
                val snippet = currentRelativeId++.getSnippetFromId() ?: break
                val actualId = snippet.id

                val start = currentSentenceIndex
                currentSentenceIndex += if (allSnippets.isNotEmpty()) {
                    snippet.content
                }
                else {
                    snippet.content.substring(currentSentenceStart.startIndex)
                }.prepareForDisplay().length

                var end = currentSentenceIndex
                if (end > currentSentence!!.length) {
                    end = currentSentence!!.length
                }

                allSnippets.add(SnippetInfo(actualId, start, end, null, null))
            } while (nextSentenceStart?.relativeSnippet != null && currentRelativeId <= nextSentenceStart?.relativeSnippet!!)

            if (allSnippets.isNotEmpty()) {
                allSnippets.first().snippetStartIndex = currentSentenceStart.startIndex
                allSnippets.last().snippetEndIndex = nextSentenceStart?.startIndex
            }

            return allSnippets
        }

    data class SnippetInfo(
            val snippetId: Int,
            val currentSentenceStartIndex: Int,
            val currentSentenceEndIndex: Int,
            var snippetStartIndex: Int?,
            var snippetEndIndex: Int?
    )

    fun getCurrentSentenceStart() = currentSentenceStart.toIndexInfo()
    fun getNextSentenceStart() = nextSentenceStart?.toIndexInfo()
    fun getPreviousSentenceStart() = previousSentenceStart?.toIndexInfo()

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
        require(currentSnippet != null) { "Current snippet is blank" }

        // Calculate boundaries
        setCurrentSentenceStart()
        setPreviousSentenceStart()
        nextSentenceStart = findNextSentenceStart(currentSentenceStart.relativeSnippet, currentSentenceStart.startIndex)

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
     * - [currentSnippet] is empty
     * - [currentSentenceStart] is larger than [currentSnippet.length]
     *
     * Throws an error if:
     * - [currentSentenceStart] != [CURRENT_SNIPPET_RELATIVE_ID]
     * - [currentSentenceStart] < 0
     * - No non-stop characters on or after [currentSentenceStart]
     */
    private fun setCurrentSentenceStart() {
        require(currentSentenceStart.relativeSnippet == CURRENT_SNIPPET_RELATIVE_ID) {
            "Initial start must be in current snippet"
        }
        require(currentSentenceStart.startIndex >= 0) { "Sentence start cannot be < 0" }

        val currentSnippetContent = currentSnippet?.content
        if (!currentSnippetContent.isNullOrBlank() && currentSentenceStart.startIndex < currentSnippetContent.length
                && !sentenceStops.contains(currentSnippetContent[currentSentenceStart.startIndex])
        ) {
            currentSentenceStart =
                    findPreviousSentenceStart(currentSentenceStart.relativeSnippet, currentSentenceStart.startIndex)
                            ?: currentSentenceStart
            return
        }

        /*
         * Check forwards if currently on a sentence end char
         */
        if (!currentSnippetContent.isNullOrBlank() && currentSentenceStart.startIndex < currentSnippetContent.length) {
            val startIndex = FindType.FIRST_NON_STOP.find(currentSentenceStart.startIndex)
            if (startIndex != null) {
                currentSentenceStart = RelativeIndexInfo(startIndex, CURRENT_SNIPPET_RELATIVE_ID)
                return
            }
        }

        // No non-stop chars so check the next snippet
        var snippet = currentSentenceStart.relativeSnippet + 1
        while (true) {
            val snippetContent = snippet.getSnippetFromId()?.content ?: break
            if (snippetContent.isBlank()) {
                snippet += 1
                continue
            }
            val startIndex = FindType.FIRST_NON_STOP.find(relativeSnippetId = snippet)
            if (startIndex != null) {
                currentSentenceStart = RelativeIndexInfo(startIndex, snippet + 1)
            }
            snippet += 1
        }
        throw IllegalArgumentException("No non-stop chars after current character")
    }

    private fun findPreviousSentenceStart(relativeSnippetId: Int, endIndex: Int? = null): RelativeIndexInfo? {
        val currentContent = relativeSnippetId.getSnippetFromId()?.content ?: return null
        if (currentContent.isBlank()) {
            return findPreviousSentenceStart(relativeSnippetId - 1)
        }

        // Check that this has non-stop characters
        var lastNonStopChar: Int? = FindType.LAST_NON_STOP.find(
                endIndex = endIndex?.plus(1),
                relativeSnippetId = relativeSnippetId
        ) ?: return findPreviousSentenceStart(relativeSnippetId - 1)

        // Is there a hard stop?
        var hardStop = FindType.LAST_STOP.find(
                startIndex = lastNonStopChar,
                endIndex = endIndex?.plus(1),
                stopChars = hardSentenceStops,
                relativeSnippetId = relativeSnippetId
        )
        if (hardStop != null) {
            return null
        }

        // Find the last stop character
        val lastStopCharacter = FindType.LAST_STOP.find(
                endIndex = lastNonStopChar,
                relativeSnippetId = relativeSnippetId
        ) ?: return findPreviousSentenceStart(relativeSnippetId - 1)
                ?: RelativeIndexInfo(0, relativeSnippetId)

        // Is that the last stop character in the string?
        lastNonStopChar = FindType.LAST_NON_STOP.find(
                endIndex = lastStopCharacter,
                relativeSnippetId = relativeSnippetId
        )
        if (lastNonStopChar != null) {
            return RelativeIndexInfo(lastStopCharacter + 1, relativeSnippetId)
        }

        // Is there a hard stop?
        hardStop = FindType.LAST_STOP.find(
                endIndex = lastStopCharacter + 1,
                stopChars = hardSentenceStops,
                relativeSnippetId = relativeSnippetId
        )
        if (hardStop != null) {
            return RelativeIndexInfo(lastStopCharacter + 1, relativeSnippetId)
        }

        return findPreviousSentenceStart(relativeSnippetId - 1)
                ?: RelativeIndexInfo(lastStopCharacter + 1, relativeSnippetId)
    }

    private fun setPreviousSentenceStart() {
        val previousEnd = previousSentenceEnd(currentSentenceStart.relativeSnippet, currentSentenceStart.startIndex - 1)
                ?: return
        previousSentenceStart = findPreviousSentenceStart(previousEnd.relativeSnippet, previousEnd.startIndex)
    }

    private fun previousSentenceEnd(relativeSnippetId: Int, index: Int? = null): RelativeIndexInfo? {
        val currentContent = relativeSnippetId.getSnippetFromId()?.content ?: return null

        if (currentContent.isBlank() || (index != null && index < 0)) {
            return previousSentenceEnd(relativeSnippetId - 1)
        }

        val lastNonStop = FindType.LAST_NON_STOP.find(
                endIndex = index,
                relativeSnippetId = relativeSnippetId
        ) ?: return previousSentenceEnd(relativeSnippetId - 1)

        return RelativeIndexInfo(lastNonStop, relativeSnippetId)
    }

    private fun findNextSentenceStart(relativeSnippetId: Int, index: Int? = null): RelativeIndexInfo? {
        val currentContent = relativeSnippetId.getSnippetFromId()?.content ?: return null
        if (currentContent.isBlank()) {
            return findNextSentenceStart(relativeSnippetId + 1)
        }

        // Check that not at the end of snippet
        if (index != null && index >= currentContent.length) {
            return findNextSentenceStart(relativeSnippetId + 1)
        }

        // Find the end of the current sentence
        val sentenceEnd = FindType.FIRST_STOP.find(
                startIndex = index,
                relativeSnippetId = relativeSnippetId
        ) ?: return findNextSentenceStart(relativeSnippetId + 1)

        val nextSentenceStart = FindType.FIRST_NON_STOP.find(
                startIndex = sentenceEnd,
                relativeSnippetId = relativeSnippetId
        )
        if (nextSentenceStart != null) {
            return RelativeIndexInfo(nextSentenceStart, relativeSnippetId)
        }

        // No more non stops in this snippet, check for hard stop
        FindType.FIRST_STOP.find(
                startIndex = sentenceEnd,
                stopChars = hardSentenceStops,
                relativeSnippetId = relativeSnippetId
        ) ?: return findNextSentenceStart(relativeSnippetId + 1)

        return findFirstNonStopCharacter(relativeSnippetId + 1)
    }

    private fun findFirstNonStopCharacter(relativeSnippetId: Int?): RelativeIndexInfo? {
        val currentContent = relativeSnippetId?.getSnippetFromId()?.content ?: return null
        if (currentContent.isBlank()) return findFirstNonStopCharacter(relativeSnippetId + 1)
        val firstNonStop = FindType.FIRST_NON_STOP.find(
                startIndex = 0,
                relativeSnippetId = relativeSnippetId
        ) ?: return findFirstNonStopCharacter(relativeSnippetId + 1)
        return RelativeIndexInfo(firstNonStop, relativeSnippetId)
    }

    private fun extractSentence(start: RelativeIndexInfo, end: RelativeIndexInfo?): String {
        if (end != null && start.relativeSnippet == end.relativeSnippet) {
            return start.relativeSnippet.getSnippetFromId()?.content!!.substring(start.startIndex, end.startIndex)
                    .prepareForDisplay()
        }

        var tempSentence =
                start.relativeSnippet.getSnippetFromId()?.content!!.substring(start.startIndex).prepareForDisplay()
        var middleSnippet = start.relativeSnippet + 1
        while (middleSnippet != end?.relativeSnippet) {
            val middleSnippetContent = middleSnippet.getSnippetFromId()?.content ?: break
            tempSentence += middleSnippetContent.prepareForDisplay()
            middleSnippet++
        }
        if (end != null) {
            tempSentence += end.relativeSnippet.getSnippetFromId()?.content!!.substring(0, end.startIndex)
                    .prepareForDisplay()
        }
        return tempSentence
    }

    private fun String?.prepareForDisplay() = this?.filterNot { nonDisplayCharacters.contains(it) }?.trim() ?: ""

    private fun FindType.find(
            startIndex: Int? = null,
            endIndex: Int? = null,
            stopChars: Set<Char> = sentenceStops,
            relativeSnippetId: Int = CURRENT_SNIPPET_RELATIVE_ID
    ): Int? {
        val snippet = relativeSnippetId.getSnippetFromId()?.content

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
        // TODO CLEANUP Do I need this guard?
        if (parseJob.isActive || parseJob.isCompleted) {
            parseJob.cancel(CancellationException("New job created"))
        }
    }

    fun substring(start: Int, end: Int): String? {
        return currentSentence?.substring(start, end)
    }

    private data class RelativeIndexInfo(val startIndex: Int, val relativeSnippet: Int)

    private fun RelativeIndexInfo.toIndexInfo() = IndexInfo(startIndex, relativeSnippet.getSnippetFromId()!!.id)
    data class IndexInfo(val startIndex: Int, val textSnippetId: Int)

    private fun Int.getSnippetFromId() = when {
        this == CURRENT_SNIPPET_RELATIVE_ID -> currentSnippet
        this < CURRENT_SNIPPET_RELATIVE_ID -> previousSnippets?.reversed()?.elementAtOrNull(abs(this) - 1)
        else -> nextSnippets?.elementAtOrNull(this - 1)
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