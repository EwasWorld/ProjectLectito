package com.eywa.projectlectito.readSentence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.eywa.projectlectito.*
import kotlinx.android.synthetic.main.read_sentence_fragment.*

class ReadSentenceFragment : Fragment() {
    companion object {
        private const val japaneseListDelimiter = "・"
        private val stringsToRemoveFromDisplay = setOf('\n')
        private val sentenceBreakStrings = setOf("。").plus(stringsToRemoveFromDisplay.map { it.toString() })
    }

    // TODO Set up arguments
    // private val args: NewScoreFragmentArgs by navArgs()

    private var textSnippet = ""
    private var currentCharacter: Int? = null
    private var currentSentenceEnd: Int? = null
    private var previousSentenceStart: Int? = null

    private var allDefinitionsForWord: JishoReturnData? = null
    private var currentDefinitionIndex: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.read_sentence_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.read_sentence__title)

        val readSentenceViewModel = ViewModelProvider(this)[ReadSentenceViewModel::class.java]
        // TODO Get from arguments
        readSentenceViewModel.textSnippetId.postValue(1)
        readSentenceViewModel.allSnippets.observe(viewLifecycleOwner, {
            displaySentence()
        })
        readSentenceViewModel.textSnippet.observe(viewLifecycleOwner, {
            textSnippet = it?.content ?: ""
            it?.let { snippet ->
                val chapter = if (snippet.chapterId != null) "第%d章".format(snippet.chapterId) else ""
                val page = "%dページ".format(snippet.pageReference)
                text_read_sentence__chapter_page.text = "%s%s".format(chapter, page)
            }
            displaySentence()
        })
        readSentenceViewModel.currentCharacter.observe(viewLifecycleOwner, {
            currentCharacter = it
            displaySentence()
        })
        readSentenceViewModel.textName.observe(viewLifecycleOwner, {
            text_read_sentence__current_text_title.text = it ?: ""
        })

        readSentenceViewModel.definitions.observe(viewLifecycleOwner, {
            allDefinitionsForWord = it
            displayDefinition()
        })
        readSentenceViewModel.currentDefinition.observe(viewLifecycleOwner, {
            currentDefinitionIndex = it
            displayDefinition()
        })

        button_read_sentence___next_sentence.setOnClickListener {
            if (currentSentenceEnd == null) {
                ToastSpamPrevention.displayToast(
                        requireContext(),
                        resources.getString(R.string.err_read_sentence__no_more_sentences)
                )
                return@setOnClickListener
            }

            var nextSentence = currentSentenceEnd!!
            while (sentenceBreakStrings.contains(textSnippet[nextSentence].toString())) {
                nextSentence++
            }
            readSentenceViewModel.currentCharacter.postValue(nextSentence)
        }

        button_read_sentence___previous_sentence.setOnClickListener {
            if (previousSentenceStart == null) {
                ToastSpamPrevention.displayToast(
                        requireContext(),
                        resources.getString(R.string.err_read_sentence__no_more_sentences)
                )
                return@setOnClickListener
            }
            readSentenceViewModel.currentCharacter.postValue(previousSentenceStart)
        }

        button_read_sentence___next_definition.setOnClickListener {
            val totalDefinitions = allDefinitionsForWord?.data?.size
            if (totalDefinitions == null || (currentDefinitionIndex != null && totalDefinitions <= currentDefinitionIndex!! + 1)) {
                ToastSpamPrevention.displayToast(
                        requireContext(),
                        resources.getString(R.string.err_read_sentence__no_more_definitions)
                )
                return@setOnClickListener
            }
            readSentenceViewModel.currentDefinition.postValue(currentDefinitionIndex!! + 1)
        }

        button_read_sentence___previous_definition.setOnClickListener {
            val totalDefinitions = allDefinitionsForWord?.data?.size
            if (totalDefinitions == null || currentDefinitionIndex == null || currentDefinitionIndex!! == 0) {
                ToastSpamPrevention.displayToast(
                        requireContext(),
                        resources.getString(R.string.err_read_sentence__no_more_definitions)
                )
                return@setOnClickListener
            }
            readSentenceViewModel.currentDefinition.postValue(currentDefinitionIndex!! - 1)
        }
    }

    /**
     * Calculates the starts and ends of sentences then sets [text_read_sentence__sentence] and [text_read_sentence__context].
     * TODO Span across two snippets for the start and end of a snippet
     */
    private fun displaySentence() {
        if (textSnippet.isBlank()) {
            hideAllSentenceContents()
            return
        }
        val currentSentenceStart = currentCharacter ?: 0

        if (currentSentenceStart < 0 || currentSentenceStart >= textSnippet.length) {
            hideAllSentenceContents()
            return
        }

        /*
         * Find the start of the previous sentence
         */
        previousSentenceStart = null

        // Ignore any sentence end characters directly before the current character
        var previousSentenceEnd = currentSentenceStart - 1
        while (previousSentenceEnd >= 0 && sentenceBreakStrings.contains(textSnippet[previousSentenceEnd].toString())) {
            previousSentenceEnd--
        }

        // Search backwards for a sentence end character
        if (previousSentenceEnd > 0) {
            previousSentenceStart = textSnippet.substring(0, previousSentenceEnd + 1)
                    .findLastAnyOf(sentenceBreakStrings)?.first?.plus(1) ?: 0
            if (previousSentenceStart!! >= previousSentenceEnd) {
                previousSentenceStart = null
            }
        }

        if (previousSentenceStart != null) {
            text_read_sentence__context.text = textSnippet.substring(previousSentenceStart!!, currentSentenceStart)
                    .filterNot { stringsToRemoveFromDisplay.contains(it) }
        }
        else {
            text_read_sentence__context.text = ""
        }
        text_read_sentence__context.visibility = (previousSentenceStart != null).asVisibility()
        button_read_sentence___previous_sentence.isEnabled = previousSentenceStart != null

        /*
         * Find the end of the current sentence
         */
        currentSentenceEnd = textSnippet.findAnyOf(sentenceBreakStrings, currentSentenceStart)?.first
        button_read_sentence___next_sentence.isEnabled = currentSentenceEnd != null

        val newSnippet = if (currentSentenceEnd == null) {
            textSnippet.substring(currentSentenceStart)
        }
        else {
            while (sentenceBreakStrings.contains(textSnippet[currentSentenceEnd!!].toString())) {
                currentSentenceEnd = currentSentenceEnd!! + 1
            }
            textSnippet.substring(currentSentenceStart, currentSentenceEnd!!)
        }

        text_read_sentence__sentence.text = newSnippet.filterNot { stringsToRemoveFromDisplay.contains(it) }
    }

    private fun hideAllSentenceContents() {
        text_read_sentence__sentence.text = ""
        text_read_sentence__context.text = ""
        button_read_sentence___previous_sentence.isEnabled = false
        button_read_sentence___next_sentence.isEnabled = false
    }

    private fun displayDefinition() {
        if (allDefinitionsForWord == null) {
            showWordDefinition(false)
            return
        }
        if (currentDefinitionIndex == null) {
            currentDefinitionIndex = 0
        }

        val currentDefinition = allDefinitionsForWord!!.data[currentDefinitionIndex!!]
        if (currentDefinition.japanese.isNullOrEmpty()) {
            showWordDefinition(false)
            throw IllegalArgumentException("Jisho data contains no japanese entries")
        }
        if (currentDefinition.japanese.isNullOrEmpty()) {
            showWordDefinition(false)
            throw IllegalArgumentException("Jisho data contains no senses entries")
        }

        button_read_sentence___previous_definition.isEnabled = currentDefinitionIndex != 0
        button_read_sentence___next_definition.isEnabled =
                currentDefinitionIndex!! + 1 < allDefinitionsForWord!!.data.size

        text_read_sentence__reading.text = currentDefinition.japanese[0].reading
        text_read_sentence__word.text = currentDefinition.japanese[0].word
        text_read_sentence__is_common.visibility = currentDefinition.is_common.asVisibility()
        text_read_sentence__jlpt.text = currentDefinition.jlpt.joinToString(",")
        text_read_sentence__tags.text = currentDefinition.tags.joinToString(", ")

        layout_read_sentence__english_definitions.removeAllViews()
        for (indexedItem in currentDefinition.senses.withIndex()) {
            val item = indexedItem.value

            val wordDefinitionView = WordDefinitionDetailView(requireContext())
            layout_read_sentence__english_definitions.addView(wordDefinitionView)

            wordDefinitionView.updateDefinition(item.english_definitions.joinToString("; "))
            wordDefinitionView.updatePartsOfSpeech(item.parts_of_speech.joinToString("; "))
            wordDefinitionView.updateTags(item.tags.joinToString("; "))
            wordDefinitionView.updateIndex(indexedItem.index + 1)
        }

        // First item is used when displaying the main word
        text_read_sentence__other_forms.text = currentDefinition.japanese
                .subList(1, currentDefinition.japanese.size)
                .joinToString(japaneseListDelimiter) { "${it.word}[${it.reading}]" }
        showWordDefinition(true)
    }

    private fun showWordDefinition(isDisplayed: Boolean) {
        text_read_sentence__no_definition.visibility = (!isDisplayed).asVisibility()
        layout_read_sentence__jisho_info.visibility = isDisplayed.asVisibility()
    }
}