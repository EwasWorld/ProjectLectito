package com.eywa.projectlectito.readSentence

class Sentence {
    companion object {
        private val stringsToRemoveFromDisplay = setOf('\n')
        private val sentenceBreakStrings = setOf("。").plus(stringsToRemoveFromDisplay.map { it.toString() })
    }

    var textSnippetContent: String? = null
        set(value) {
            field = value
            hasBeenCalculated = false
        }
    private var currentCharacter: Int = 0

    private var hasBeenCalculated = false

    private var currentSentenceStart: Int = 0
    private var nextSentenceStart: Int? = null
    private var previousSentenceStart: Int? = null

    private var currentSentence: String? = null
    private var previousSentence: String? = null

    fun setCurrentCharacter(value: Int?) {
        currentCharacter = value ?: 0
        hasBeenCalculated = false
    }

    private fun calculate() {
        if (hasBeenCalculated) {
            return
        }
        currentSentenceStart = currentCharacter
        nextSentenceStart = null
        previousSentenceStart = null
        currentSentence = null
        previousSentence = null

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

    fun getCurrentSentence(): String? {
        calculate()
        return currentSentence
    }

    fun getPreviousSentence(): String? {
        calculate()
        return previousSentence
    }

    fun hasNextSentence(): Boolean {
        calculate()
        return nextSentenceStart != null
    }

    fun getNextSentenceStart(): Int? {
        calculate()
        return nextSentenceStart
    }

    fun getPreviousSentenceStart(): Int? {
        calculate()
        return previousSentenceStart
    }
}