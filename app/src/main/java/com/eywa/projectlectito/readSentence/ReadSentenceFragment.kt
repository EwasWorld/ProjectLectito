package com.eywa.projectlectito.readSentence

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.eywa.projectlectito.*
import com.eywa.projectlectito.databinding.RsFragmentBinding
import com.eywa.projectlectito.editSnippet.EditSnippetFragment
import kotlinx.android.synthetic.main.rs_fragment.*
import kotlinx.android.synthetic.main.rs_selected_word_info_parsed.*
import kotlinx.android.synthetic.main.rs_selected_word_info_simple.*
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

    private var sentence: ReadSentenceViewModel.SentenceWithInfo? = null
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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewState = readSentenceViewModel.mainViewState
        layout_read_sentence__selected_word_simple_info.setLifecycleInfo(this, this)
        layout_read_sentence__selected_word_parsed_info.setLifecycleInfo(this, this)
        layout_read_sentence__word_definition.setLifecycleInfo(this, this)
        button_read_sentence__select_mode.setLifecycleInfo(this, this)

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

        button_read_sentence__edit_sentence.setOnClickListener { editSentenceButtonAction() }
        readSentenceViewModel.editSnippetId.observe(viewLifecycleOwner, {
            it?.let { snippetToEdit ->
                EditSnippetFragment.navigateTo(
                        requireView().findNavController(),
                        snippetToEdit.id,
                        snippetToEdit.startChar,
                        snippetToEdit.endChar,
                )
                readSentenceViewModel.clearEditSnippetInfo()
            }
        })

        text_read_sentence__sentence.addSelectionChangedListener { selStart, selEnd ->
            if (wordSelectMode != WordSelectMode.SELECT) return@addSelectionChangedListener
            if (!text_read_sentence__sentence.hasSelection()) return@addSelectionChangedListener

            val selectedText = sentence?.sentence?.substring(selStart, selEnd)
            if (selectedText.isNullOrBlank()) {
                return@addSelectionChangedListener
            }

            readSentenceViewModel.setSelectedWord(selectedText)
        }
    }

    private fun editSentenceButtonAction() {
        sentence?.sentence?.let { currentSentence ->
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