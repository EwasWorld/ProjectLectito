package com.eywa.projectlectito.features.readSentence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsFragmentBinding
import com.eywa.projectlectito.features.editSnippet.EditSnippetFragment
import com.eywa.projectlectito.features.readFullText.ReadFullTextFragment
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceEffect.*
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent.SentenceIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import com.eywa.projectlectito.utils.androidWrappers.ToastSpamPrevention
import kotlinx.android.synthetic.main.rs_fragment.*
import kotlin.math.roundToInt


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
    private lateinit var readSentenceMviViewModel: ReadSentenceMviViewModel

    private var currentSnippetProgressWidth = 0

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

        readSentenceMviViewModel = ViewModelProvider(this)[ReadSentenceMviViewModel::class.java]
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = readSentenceMviViewModel
        layout_read_sentence__word_definition.setLifecycleInfo(this, this)

        layout_read_sentence__selected_word_info_manual.setLifecycleInfo(this, this)
        layout_read_sentence__selected_word_info_parsed.setLifecycleInfo(this, this)
        button_read_sentence__select_mode.setLifecycleInfo(this, this)

        readSentenceMviViewModel.viewEffect.getViewEffect().observe(viewLifecycleOwner, { effect ->
            when (effect) {
                is Toast -> ToastSpamPrevention.displayToast(
                        requireContext(),
                        effect.getMessage(requireContext())
                )
                ClearTextSelection -> text_read_sentence__sentence.clearFocus()
                null -> {
                }
                is NavigateTo.EditSnippet -> {
                    EditSnippetFragment.navigateTo(
                            findNavController(),
                            effect.snippetInfo.snippetId,
                            effect.snippetInfo.snippetStartIndex ?: 0,
                            effect.snippetInfo.snippetEndIndex
                    )
                }
                is NavigateTo.ReadFullText -> {
                    ReadFullTextFragment.navigateTo(
                            findNavController(),
                            args.textId,
                            effect.snippetInfo.snippetId,
                            effect.snippetInfo.snippetStartIndex
                    )
                }
            }
        })

        readSentenceMviViewModel.viewState.observe(viewLifecycleOwner, { state ->
            val snippetPercentProgress = state.sentenceState.asValid()?.snippetPercentageProgress ?: 0.0
            val maxWidth = layout_read_sentence__text_info.width
            val newWidth = (snippetPercentProgress * maxWidth).roundToInt()
            if (currentSnippetProgressWidth != newWidth) {
                overlay_read_sentence__snippet_progress_indicator.layoutParams.width = newWidth
                overlay_read_sentence__snippet_progress_indicator.requestLayout()
                currentSnippetProgressWidth = newWidth
            }
        })

        readSentenceMviViewModel.handle(
                SentenceIntent.Initialise(
                        args.currentSnippetId.takeIf { it != -1 },
                        args.currentCharacter.takeIf { it != -1 },
                        args.textId
                )
        )

        button_read_sentence__next_sentence.setOnClickListener {
            readSentenceMviViewModel.handle(SentenceIntent.OnNextSentenceClicked)
        }
        button_read_sentence__previous_sentence.setOnClickListener {
            readSentenceMviViewModel.handle(SentenceIntent.OnPreviousSentenceClicked)
        }
        button_read_sentence__full_text.setOnClickListener {
            readSentenceMviViewModel.handle(SentenceIntent.OnViewFullTextClicked)
        }
        button_read_sentence__edit_sentence.setOnClickListener {
            readSentenceMviViewModel.handle(SentenceIntent.OnEditSentenceClicked)
        }
        text_read_sentence__sentence.addSelectionChangedListener { selStart, selEnd ->
            readSentenceMviViewModel.handle(
                    ReadSentenceIntent.SelectedWordIntent.OnSentenceTextSelected(
                            selStart,
                            selEnd
                    )
            )
        }
        overlay_read_sentence__select_mode.setOnClickListener {
            readSentenceMviViewModel.handle(ReadSentenceIntent.SelectedWordIntent.OnWordSelectModeMenuStateChange(false))
        }
        overlay_read_sentence__sentence.setOnClickListener {
            readSentenceMviViewModel.handle(SentenceIntent.OnEditOverlayClicked)
        }
    }
}