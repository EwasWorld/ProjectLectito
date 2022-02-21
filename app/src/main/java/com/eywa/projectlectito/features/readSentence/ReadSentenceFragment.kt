package com.eywa.projectlectito.features.readSentence

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.eywa.projectlectito.*
import com.eywa.projectlectito.databinding.RsFragmentBinding
import com.eywa.projectlectito.features.editSnippet.EditSnippetFragment
import com.eywa.projectlectito.features.readFullText.ReadFullTextFragment
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import com.eywa.projectlectito.utils.ToastSpamPrevention
import kotlinx.android.synthetic.main.rs_fragment.*
import kotlinx.android.synthetic.main.rs_selected_word_info_parsed.*
import kotlinx.android.synthetic.main.rs_word_definition.*


class ReadSentenceFragment : Fragment() {
    companion object {
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

    private lateinit var binding: RsFragmentBinding
    private lateinit var readSentenceViewModel: ReadSentenceViewModel
    private lateinit var readSentenceMviViewModel: ReadSentenceMviViewModel

    private var sentence: Sentence? = null
    private var wordSelectMode: WordSelectMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.rs_fragment, container, false)
        if (!view.isInEditMode) {
            binding = RsFragmentBinding.bind(view)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.read_sentence__title)

        readSentenceViewModel = ViewModelProvider(this)[ReadSentenceViewModel::class.java]
        readSentenceMviViewModel = ViewModelProvider(this)[ReadSentenceMviViewModel::class.java]
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewState = readSentenceViewModel.mainViewState
        layout_read_sentence__word_definition.setLifecycleInfo(this, this)

        layout_read_sentence__selected_word_info_select.setLifecycleInfo(this, this)
        layout_read_sentence__selected_word_info_type.setLifecycleInfo(this, this)
        layout_read_sentence__selected_word_info_parsed.setLifecycleInfo(this, this)
        button_read_sentence__select_mode.setLifecycleInfo(this, this)

        if (args.currentSnippetId != -1) {
            readSentenceViewModel.updateCurrentSnippetAndChar(
                    Sentence.IndexInfo(
                            if (args.currentCharacter != -1) args.currentCharacter else 0,
                            args.currentSnippetId
                    )
            )
        }
        else {
            readSentenceViewModel.getFirstSnippetForText(args.textId).observe(viewLifecycleOwner, { firstSnippetId ->
                readSentenceViewModel.updateCurrentSnippetAndChar(Sentence.IndexInfo(0, firstSnippetId))
            })
        }

        readSentenceViewModel.sentence.observe(viewLifecycleOwner, { sentence = it })
        readSentenceViewModel.wordSelectMode.observe(viewLifecycleOwner, { selectMode ->
            wordSelectMode = selectMode
            text_read_sentence__sentence.clearFocus()
        })

        overlay_read_sentence__select_mode.setOnClickListener {
            readSentenceViewModel.setSelectWordSelectModeMenuOpen(false)
        }
        button_read_sentence__select_mode.selectMenuOpenedListener = {
            text_read_sentence__sentence.clearFocus()
        }

        button_read_sentence__next_sentence.setOnClickListener {
            sentence?.let {
                val nextSentenceStart = it.getNextSentenceStart()
                if (nextSentenceStart == null) {
                    ToastSpamPrevention.displayToast(
                            requireContext(),
                            resources.getString(R.string.err_read_sentence__no_more_sentences)
                    )
                    return@setOnClickListener
                }
                readSentenceViewModel.updateCurrentSnippetAndChar(nextSentenceStart)
            }
        }

        button_read_sentence__previous_sentence.setOnClickListener {
            sentence?.let {
                val previousSentenceStart = it.getPreviousSentenceStart()
                if (previousSentenceStart == null) {
                    ToastSpamPrevention.displayToast(
                            requireContext(),
                            resources.getString(R.string.err_read_sentence__no_more_sentences)
                    )
                    return@setOnClickListener
                }
                readSentenceViewModel.updateCurrentSnippetAndChar(previousSentenceStart)
            }
        }

        button_read_sentence__full_text.setOnClickListener {
            val firstSnippet = sentence?.snippetsInCurrentSentence?.get(0)
            ReadFullTextFragment.navigateTo(
                    findNavController(),
                    args.textId,
                    firstSnippet?.snippetId,
                    firstSnippet?.snippetStartIndex
            )
        }

        button_read_sentence__edit_sentence.setOnClickListener { editSentenceButtonAction() }
        readSentenceViewModel.snippetToEdit.observe(viewLifecycleOwner, {
            it?.let { snippetToEdit ->
                EditSnippetFragment.navigateTo(
                        findNavController(),
                        snippetToEdit.snippetId,
                        snippetToEdit.startChar,
                        snippetToEdit.endChar,
                )
                readSentenceViewModel.clearEditSnippetInfo()
            }
        })

        text_read_sentence__sentence.addSelectionChangedListener { selStart, selEnd ->
            if (wordSelectMode != WordSelectMode.SELECT) return@addSelectionChangedListener
            if (!text_read_sentence__sentence.hasSelection()) return@addSelectionChangedListener

            val selectedText = sentence?.substring(selStart, selEnd)
            if (selectedText.isNullOrBlank()) {
                return@addSelectionChangedListener
            }

            readSentenceViewModel.setSelectedWord(selectedText)
            readSentenceMviViewModel.handle(ReadSentenceIntent.SelectedWordIntent.OnSimpleWordSelected(selectedText))
        }
    }

    private fun editSentenceButtonAction() {
        sentence?.let { currentSentence ->
            when (currentSentence.snippetsInCurrentSentence.size) {
                0 -> {
                    ToastSpamPrevention.displayToast(
                            requireContext(),
                            resources.getString(R.string.read_sentence__no_content_to_edit)
                    )
                    return
                }
                1 -> {
                    val firstSnippet = currentSentence.snippetsInCurrentSentence.first()
                    EditSnippetFragment.navigateTo(
                            requireView().findNavController(),
                            firstSnippet.snippetId,
                            firstSnippet.snippetStartIndex ?: 0,
                            firstSnippet.snippetEndIndex,
                    )
                    return
                }
                else -> readSentenceViewModel.setSelectSnippetMode(true)
            }
        }
    }
}