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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.eywa.projectlectito.*
import com.eywa.projectlectito.editSnippet.EditSnippetFragment
import com.eywa.projectlectito.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.wordDefinitions.WordDefinitionDetailView
import kotlinx.android.synthetic.main.rs_frag.*
import kotlinx.android.synthetic.main.rs_frag.layout_read_sentence__jisho_info
import kotlinx.android.synthetic.main.rs_selected_word_info_parsed.*
import kotlinx.android.synthetic.main.rs_selected_word_info_simple.*
import kotlinx.android.synthetic.main.rs_word_definition.*


class ReadSentenceFragment : Fragment() {
    companion object {
        private const val japaneseListDelimiter = "・"

        fun navigateTo(
                navController: NavController,
                textId: Int,
                currentSnippetId: Int? = null,
                currentCharacter: Int? = null
        ) {
            val bundle = Bundle()
            bundle.putInt("textId", textId)
            bundle.putInt("currentSnippetId", currentSnippetId ?: -1)
            bundle.putInt("currentCharacter", currentCharacter ?: -1)
            navController.navigate(R.id.readSentenceFragment, bundle)
        }
    }

    private val args: ReadSentenceFragmentArgs by navArgs()

    private lateinit var readSentenceViewModel: ReadSentenceViewModel

    private var allDefinitionsForWord: JishoWordDefinitions? = null
    private var currentDefinitionIndex: Int? = null

    private var sentence: ReadSentenceViewModel.SentenceWithInfo? = null
    private var currentSelectionStart: Int? = null
    private var wordSelectMode: WordSelectMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.rs_frag, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.read_sentence__title)

        readSentenceViewModel = ViewModelProvider(this)[ReadSentenceViewModel::class.java]
        layout_read_sentence__selected_word_simple_info.setLifecycleInfo(this, this)
        layout_read_sentence__selected_word_parsed_info.setLifecycleInfo(this, this)

        readSentenceViewModel.allSnippets.observe(viewLifecycleOwner, {
            val test = it
            Log.d("ReadSentenceFragment", "Text snippets found: ${test.size}")
        })

        if (args.currentSnippetId != -1) {
            readSentenceViewModel.updateCurrentCharacter(
                    Sentence.IndexInfo(
                            if (args.currentCharacter != -1) args.currentCharacter else 0,
                            args.currentSnippetId
                    )
            )
        }
        else {
            readSentenceViewModel.getFirstSnippetForText(args.textId).observe(viewLifecycleOwner, { firstSnippetId ->
                readSentenceViewModel.updateCurrentCharacter(Sentence.IndexInfo(0, firstSnippetId))
            })
        }
        readSentenceViewModel.currentSnippetInfo.observe(viewLifecycleOwner, {
            text_read_sentence__chapter_page.text = it ?: ""
        })
        readSentenceViewModel.sentence.observe(viewLifecycleOwner, {
            sentence = it
            displaySentence(it)
            button_read_sentence__edit_sentence.isEnabled = it?.sentence?.currentSentence != null
        })
        readSentenceViewModel.wordSelectMode.observe(viewLifecycleOwner, { selectMode ->
            if (selectMode != wordSelectMode) {
                wordSelectMode = selectMode
                sentence?.let { displaySentence(it) }
                val isInSelectionMode = selectMode == WordSelectMode.SELECT
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

        button_read_sentence__select_mode.overlays = listOf(overlay_read_sentence__select_mode)
        button_read_sentence__select_mode.selectModeChangedListener = {
            readSentenceViewModel.updateWordSelectMode(it)
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
            readSentenceViewModel.searchWord.postValue(null)
        }

        button_read_sentence__edit_sentence.setOnClickListener { editSentenceButtonAction() }

        text_read_sentence__sentence.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                readSentenceViewModel.selectedWord.postValue(getSelectedText())
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
        if (wordSelectMode != WordSelectMode.SELECT) return null
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
        if (wordSelectMode?.isAuto == true) {
            text_read_sentence__sentence.setTextIsSelectable(false)
            sentenceWithInfo.parsedInfo?.let { it ->
                text_read_sentence__sentence.setTextSpans(it.getAsSpannedString(currentSentence))
                isSentenceSet = true
            }
        }
        if (!isSentenceSet) {
            text_read_sentence__sentence.text = currentSentence
            text_read_sentence__sentence.clearTextSpans()
            text_read_sentence__sentence.setTextIsSelectable(wordSelectMode == WordSelectMode.SELECT)
        }
        showSelectedWordInfoViews()

        image_read_sentence__parse_complete.visibility =
                (sentenceWithInfo.parsedInfo != null && !sentenceWithInfo.parseError).asVisibility()
        image_read_sentence__parse_failed.visibility = sentenceWithInfo.parseError.asVisibility()
    }

    private fun showSelectedWordInfoViews() {
        val isAutoSelect = wordSelectMode?.isAuto == true

        // Parsed info
        layout_read_sentence__selected_word_parsed_info.visibility = isAutoSelect.asVisibility()
    }

    private fun List<ParsedInfo>.getAsSpannedString(currentSentence: String): SpannableString {
        val spannedString = SpannableString(currentSentence)
        this.forEachIndexed { index, parsedInfo ->
            val spanStartIndex = parsedInfo.startCharacterIndex
            val spanEndIndex = parsedInfo.endCharacterIndex

            val span = object : ClickableSpan() {
                override fun onClick(p0: View) {
                    readSentenceViewModel.selectedWord.postValue(
                            currentSentence.substring(
                                    spanStartIndex,
                                    spanEndIndex
                            )
                    )
                    readSentenceViewModel.selectedParsedInfo.postValue(parsedInfo)

                    // supplementary symbol (number, punctuation, etc.)
                    val isWord = parsedInfo.partsOfSpeech[0] != "補助記号"
                    if (isWord) {
                        readSentenceViewModel.searchWord.postValue(parsedInfo.dictionaryForm)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    // Don't underline or highlight the text
                }
            }
            spannedString.setSpan(span, spanStartIndex, spanEndIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

            if (wordSelectMode == WordSelectMode.AUTO_WITH_COLOUR) {
                if (index % 2 == 1) {
                    spannedString.setSpan(
                            ForegroundColorSpan(Color.RED),
                            spanStartIndex,
                            spanEndIndex,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }
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
                    wordSelectMode?.noDefinitionStringId ?: R.string.read_sentence__no_definition_select
            )
            text_read_sentence__no_definition.text =
                    resources.getString(R.string.read_sentence__no_definition).format(toShowDefinition)
        }
    }

    private fun editSentenceButtonAction() {
        if (!overlay_read_sentence__sentence.hasOnClickListeners()) {
            overlay_read_sentence__sentence.setOnClickListener {
                displaySentence(sentence)
                overlay_read_sentence__sentence.visibility = false.asVisibility()
            }
        }

        sentence?.sentence?.let { currentSentence ->
            val snippets = currentSentence.snippetsInCurrentSentence

            if (snippets.isEmpty()) {
                return
            }
            if (snippets.size == 1) {
                EditSnippetFragment.navigateTo(
                        requireView().findNavController(),
                        snippets.first().snippetId,
                        snippets.first().snippetStartIndex ?: 0,
                        snippets.first().snippetEndIndex,
                )
                return
            }

            /*
             * Setup select snippet spans
             */
            currentSentence.currentSentence ?: return

            text_read_sentence__sentence.text = ""
            text_read_sentence__sentence.setTextIsSelectable(false)
            val spannableString = SpannableString(currentSentence.currentSentence)
            snippets.forEachIndexed { index, snippetInfo -> spannableString.setSpan(index, snippetInfo) }
            text_read_sentence__sentence.setTextSpans(spannableString)

            overlay_read_sentence__sentence.visibility = true.asVisibility()
        }
    }

    private fun SpannableString.setSpan(index: Int, snippetInfo: Sentence.SnippetInfo) {
        if (index % 2 == 1) {
            this.setSpan(
                    ForegroundColorSpan(Color.RED),
                    snippetInfo.currentSentenceStartIndex,
                    snippetInfo.currentSentenceEndIndex,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        this.setSpan(
                object : ClickableSpan() {
                    override fun onClick(p0: View) {
                        EditSnippetFragment.navigateTo(
                                requireView().findNavController(),
                                snippetInfo.snippetId,
                                snippetInfo.snippetStartIndex ?: 0,
                                snippetInfo.snippetEndIndex
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        // Don't underline or highlight the text
                    }
                },
                snippetInfo.currentSentenceStartIndex,
                snippetInfo.currentSentenceEndIndex,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }

    private fun TextView.setTextSpans(spannableString: SpannableString) {
        this.text = spannableString
        this.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun TextView.clearTextSpans() {
        this.movementMethod = null
    }
}