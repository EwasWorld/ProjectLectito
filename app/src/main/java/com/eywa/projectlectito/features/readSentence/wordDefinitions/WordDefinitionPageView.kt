package com.eywa.projectlectito.features.readSentence.wordDefinitions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsWordDefinitionPageBinding
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.WordDefinitionState

class WordDefinitionPageView(private val data: WordDefinitionState.HasWord.JishoEntryForDisplay) : Fragment() {
    private lateinit var binding: RsWordDefinitionPageBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.rs_word_definition_page, container, false)
        binding = RsWordDefinitionPageBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val readSentenceMviViewModel = ViewModelProvider(this)[ReadSentenceMviViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewState = data
        binding.viewModel = readSentenceMviViewModel

        val definitionsLinearLayout =
                view.findViewById<LinearLayout>(R.id.layout_read_sentence__english_definitions)
        definitionsLinearLayout.removeAllViews()
        var index = 1
        for (item in data.senses) {
            val wordDefinitionView = WordDefinitionDetailView(requireContext())
            definitionsLinearLayout.addView(wordDefinitionView)

            wordDefinitionView.updateDefinition(item.english_definitions.joinToString("; "))
            wordDefinitionView.updatePartsOfSpeech(item.parts_of_speech.joinToString("; "))
            wordDefinitionView.updateTags(item.tags.joinToString("; "))
            wordDefinitionView.updateIndex(index++)
        }
    }
}