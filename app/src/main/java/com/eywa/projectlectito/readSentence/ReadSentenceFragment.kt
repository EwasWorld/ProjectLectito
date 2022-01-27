package com.eywa.projectlectito.readSentence

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.eywa.projectlectito.*
import com.eywa.projectlectito.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.wordDefinitions.WordDefinitionDetailView
import kotlinx.android.synthetic.main.read_sentence_fragment.*


class ReadSentenceFragment : Fragment() {
    companion object {
        private const val japaneseListDelimiter = "・"
    }

    // TODO Set up arguments
    // private val args: NewScoreFragmentArgs by navArgs()

    private lateinit var readSentenceViewModel: ReadSentenceViewModel

    private var allDefinitionsForWord: JishoWordDefinitions? = null
    private var currentDefinitionIndex: Int? = null

    private var sentence: ReadSentenceViewModel.SentenceWithInfo? = null
    private var currentSelectionStart: Int? = null
    private var wordSelectMode: ReadSentenceViewModel.WordSelectMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.read_sentence_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.read_sentence__title)

        readSentenceViewModel = ViewModelProvider(this)[ReadSentenceViewModel::class.java]
        readSentenceViewModel.allSnippets.observe(viewLifecycleOwner, {
            val test = it
            Log.d("ReadSentenceFragment", test.size.toString())
        })

        // TODO Get from arguments
        readSentenceViewModel.textSnippetId.postValue(1)
        readSentenceViewModel.currentSnippetInfo.observe(viewLifecycleOwner, {
            text_read_sentence__chapter_page.text = it ?: ""
        })
        readSentenceViewModel.sentence.observe(viewLifecycleOwner, {

            sentence = it
            displaySentence(it)
        })
        readSentenceViewModel.wordSelectMode.observe(viewLifecycleOwner, { selectMode ->
            if (selectMode != wordSelectMode) {
                wordSelectMode = selectMode
                sentence?.let { displaySentence(it) }
                val isInSelectionMode = selectMode == ReadSentenceViewModel.WordSelectMode.SELECT
                text_read_sentence__sentence.setTextIsSelectable(isInSelectionMode)
                if (isInSelectionMode) {
                    text_read_sentence__sentence.clearFocus()
                }
                button_read_sentence__select_mode.setInitialState(selectMode)
                displayDefinition()
            }
        })
        readSentenceViewModel.textName.observe(viewLifecycleOwner, {
            text_read_sentence__current_text_title.text = it ?: ""
        })

        readSentenceViewModel.definitions.observe(viewLifecycleOwner, {
            // TODO Handle loading and errors
            allDefinitionsForWord = it?.jishoWordDefinitions
            displayDefinition()
        })
        readSentenceViewModel.currentDefinition.observe(viewLifecycleOwner, {
            currentDefinitionIndex = it
            displayDefinition()
        })

        button_read_sentence__select_mode.overlays = listOf(overlay_read_sentence_1, overlay_read_sentence_2)
        button_read_sentence__select_mode.selectModeChangedListener = {
            readSentenceViewModel.wordSelectMode.postValue(it)
            readSentenceViewModel.selectedWord.postValue(null)
        }
        button_read_sentence__select_mode.selectMenuOpenedListener = {
            text_read_sentence__sentence.clearFocus()
        }

        button_read_sentence__next_sentence.setOnClickListener {
            sentence?.let {
                val nextSentenceStart = it.sentence.getNextSentenceStart()
                if (nextSentenceStart == null) {
                    ToastSpamPrevention.displayToast(
                            requireContext(),
                            resources.getString(R.string.err_read_sentence__no_more_sentences)
                    )
                    return@setOnClickListener
                }
                readSentenceViewModel.updateCurrentCharacter(nextSentenceStart)
            }
        }

        button_read_sentence__previous_sentence.setOnClickListener {
            sentence?.let {
                val previousSentenceStart = it.sentence.getPreviousSentenceStart()
                if (previousSentenceStart == null) {
                    ToastSpamPrevention.displayToast(
                            requireContext(),
                            resources.getString(R.string.err_read_sentence__no_more_sentences)
                    )
                    return@setOnClickListener
                }
                readSentenceViewModel.updateCurrentCharacter(previousSentenceStart)
            }
        }

        button_read_sentence__next_definition.setOnClickListener {
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

        button_read_sentence__previous_definition.setOnClickListener {
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

        button_read_sentence__close_definition.setOnClickListener {
            readSentenceViewModel.selectedWord.postValue(null)
        }

        button_read_sentence__submit_word.setOnClickListener {
            // TODO Sanitise
            @Suppress("non_exhaustive_when")
            when (wordSelectMode) {
                ReadSentenceViewModel.WordSelectMode.SELECT -> {
                    getSelectedText()?.let { text ->
                        readSentenceViewModel.selectedWord.postValue(text)
                    }
                }
                ReadSentenceViewModel.WordSelectMode.TYPE -> {
                    val text = input_text_read_sentence__selected_simple_word.text?.toString()
                    if (text.isNullOrBlank()) return@setOnClickListener
                    readSentenceViewModel.selectedWord.postValue(text)
                }
            }
        }

        text_read_sentence__sentence.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                text_read_sentence__selected_simple_word.text = getSelectedText() ?: ""
                return true
            }

            override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
                return false
            }

            override fun onDestroyActionMode(p0: ActionMode?) {
                return
            }
        }
    }

    fun getSelectedText(): String? {
        if (wordSelectMode != ReadSentenceViewModel.WordSelectMode.SELECT) return null
        if (!text_read_sentence__sentence.hasSelection()) return null

        val start = text_read_sentence__sentence.selectionStart
        currentSelectionStart = start
        val selectedText = sentence?.sentence?.substring(start, text_read_sentence__sentence.selectionEnd)
        if (selectedText.isNullOrBlank()) {
            return null
        }
        return selectedText
    }

    /**
     * Calculates the starts and ends of sentences then sets [text_read_sentence__sentence] and [text_read_sentence__context].
     */
    private fun displaySentence(sentenceWithInfo: ReadSentenceViewModel.SentenceWithInfo?) {
        val currentSentence = sentenceWithInfo?.sentence?.currentSentence

        if (currentSentence == null) {
            text_read_sentence__sentence.text = ""
            text_read_sentence__context.text = ""
            button_read_sentence__previous_sentence.isEnabled = false
            button_read_sentence__next_sentence.isEnabled = false
            // TODO Clear selected word bar
            return
        }

        val previousSentence = sentenceWithInfo.sentence.previousSentence
        if (previousSentence == null) {
            text_read_sentence__context.text = ""
        }
        else {
            text_read_sentence__context.text = previousSentence
        }
        text_read_sentence__context.visibility = (previousSentence != null).asVisibility()
        button_read_sentence__previous_sentence.isEnabled = previousSentence != null

        button_read_sentence__next_sentence.isEnabled = sentenceWithInfo.sentence.getNextSentenceStart() != null

        var isSentenceSet = false
        text_read_sentence__sentence.text = ""
        if (wordSelectMode == ReadSentenceViewModel.WordSelectMode.AUTO) {
            text_read_sentence__sentence.setTextIsSelectable(false)
            sentenceWithInfo.parsedInfo?.let { it ->
                text_read_sentence__sentence.text = it.getAsSpannedString(currentSentence)
                text_read_sentence__sentence.movementMethod = LinkMovementMethod.getInstance()
                isSentenceSet = true
            }
        }
        if (!isSentenceSet) {
            text_read_sentence__sentence.text = currentSentence
            text_read_sentence__sentence.movementMethod = null
            text_read_sentence__sentence.setTextIsSelectable(wordSelectMode == ReadSentenceViewModel.WordSelectMode.SELECT)
        }
        showSelectedWordInfoViews()

        image_read_sentence__parse_complete.visibility =
                (sentenceWithInfo.parsedInfo != null && !sentenceWithInfo.parseError).asVisibility()
        image_read_sentence__parse_failed.visibility = sentenceWithInfo.parseError.asVisibility()
    }

    private fun showSelectedWordInfoViews() {
        val isAutoSelect = wordSelectMode == ReadSentenceViewModel.WordSelectMode.AUTO

        // Parsed info
        layout_read_sentence__selected_word_parsed_info.visibility = isAutoSelect.asVisibility()

        // Simple word display and submit
        layout_read_sentence__selected_word_simple_info.visibility = (!isAutoSelect).asVisibility()
        button_read_sentence__submit_word.visibility = (!isAutoSelect).asVisibility()

        if (!isAutoSelect) {
            input_text_read_sentence__selected_simple_word.visibility =
                    (wordSelectMode == ReadSentenceViewModel.WordSelectMode.TYPE).asVisibility()
            text_read_sentence__selected_simple_word.visibility =
                    (wordSelectMode == ReadSentenceViewModel.WordSelectMode.SELECT).asVisibility()
        }
    }

    private fun List<ParsedInfo>.getAsSpannedString(currentSentence: String): SpannableString {
        val spannedString = SpannableString(currentSentence)
        this.forEachIndexed { index, parsedInfo ->
            val spanStartIndex = parsedInfo.startCharacterIndex
            val spanEndIndex = parsedInfo.endCharacterIndex

            val span = object : ClickableSpan() {
                override fun onClick(p0: View) {
                    val originalWord = currentSentence.substring(spanStartIndex, spanEndIndex)
                    val showDictionary = originalWord != parsedInfo.dictionaryForm
                    text_read_sentence__selected_parsed_word.text = originalWord
                    if (showDictionary) {
                        text_read_sentence__dictionary_form.text = parsedInfo.dictionaryForm
                    }
                    text_read_sentence__dictionary_form.visibility = showDictionary.asVisibility()
                    text_read_sentence__selected_separator_1.visibility = showDictionary.asVisibility()

                    text_read_sentence__parts_of_speech.text = parsedInfo.partsOfSpeech
                            .filterNot { it.isBlank() || it == "*" }
                            .joinToString(japaneseListDelimiter)

                    val showPitchAccent = parsedInfo.pitchAccentPattern != null
                    if (showPitchAccent) {
                        text_read_sentence__pitch_accent.text = parsedInfo.pitchAccentPattern.toString()
                    }
                    text_read_sentence__pitch_accent.visibility = showPitchAccent.asVisibility()
                    text_read_sentence__selected_separator_2.visibility = showPitchAccent.asVisibility()

                    // supplementary symbol (number, punctuation, etc.)
                    val isWord = parsedInfo.partsOfSpeech[0] != "補助記号"
                    if (isWord) {
                        readSentenceViewModel.selectedWord.postValue(parsedInfo.dictionaryForm)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    // Don't underline or highlight the text
                }
            }
            spannedString.setSpan(span, spanStartIndex, spanEndIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

            // TODO Remove colouring
            if (index % 2 == 1)
                spannedString.setSpan(
                        ForegroundColorSpan(Color.RED),
                        spanStartIndex,
                        spanEndIndex,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
        }
        return spannedString
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

        button_read_sentence__previous_definition.isEnabled = currentDefinitionIndex != 0
        button_read_sentence__next_definition.isEnabled =
                currentDefinitionIndex!! + 1 < allDefinitionsForWord!!.data.size

        var word = currentDefinition.japanese[0].word
        if (word.isNullOrBlank()) {
            word = currentDefinition.slug
        }
        var reading: String? = currentDefinition.japanese[0].reading
        if (reading == word) {
            reading = null
        }
        text_read_sentence__word.text = word
        if (reading != null) {
            text_read_sentence__reading.text = reading
        }
        text_read_sentence__reading.visibility = (reading != null).asVisibility()

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
        val hasOtherForms = currentDefinition.japanese.size > 1
        if (hasOtherForms) {
            text_read_sentence__other_forms.text = currentDefinition.japanese
                    .subList(1, currentDefinition.japanese.size)
                    .joinToString(japaneseListDelimiter) { "${it.word}[${it.reading}]" }
        }
        text_read_sentence__other_forms_label.visibility = hasOtherForms.asVisibility()
        text_read_sentence__other_forms.visibility = hasOtherForms.asVisibility()

        showWordDefinition(true)
    }

    private fun showWordDefinition(isDisplayed: Boolean) {
        text_read_sentence__no_definition.visibility = (!isDisplayed).asVisibility()
        layout_read_sentence__jisho_info.visibility = isDisplayed.asVisibility()

        if (!isDisplayed) {
            val toShowDefinition = resources.getString(
                    when (wordSelectMode) {
                        ReadSentenceViewModel.WordSelectMode.AUTO -> R.string.read_sentence__no_definition_auto
                        ReadSentenceViewModel.WordSelectMode.SELECT -> R.string.read_sentence__no_definition_select
                        ReadSentenceViewModel.WordSelectMode.TYPE -> R.string.read_sentence__no_definition_type
                        null -> R.string.read_sentence__no_definition_select
                    }
            )

            text_read_sentence__no_definition.text =
                    resources.getString(R.string.read_sentence__no_definition).format(toShowDefinition)
        }
    }
}