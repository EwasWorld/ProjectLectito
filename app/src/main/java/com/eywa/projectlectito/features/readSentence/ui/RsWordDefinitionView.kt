package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsWordDefinitionBinding
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import com.eywa.projectlectito.features.readSentence.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionDetailView
import kotlinx.android.synthetic.main.rs_word_definition.view.*

class RsWordDefinitionView : ConstraintLayout {
    companion object {
        private const val LOG_TAG = "RsWordDefinitionView"
    }

    private lateinit var layout: RsWordDefinitionBinding
    private lateinit var viewModel: ReadSentenceMviViewModel

    private var currentDefinition: JishoWordDefinitions.JishoEntry? = null

    constructor(context: Context) : super(context) {
        initialise(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialise(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialise(context)
    }

    private fun initialise(context: Context) {
        if (isInEditMode) {
            LayoutInflater.from(context).inflate(R.layout.rs_word_definition, this, true)
        }
        else {
            layout = RsWordDefinitionBinding.inflate(LayoutInflater.from(context), this, true)
        }
    }

    fun setLifecycleInfo(viewModelStoreOwner: ViewModelStoreOwner, lifecycleOwner: LifecycleOwner) {
        viewModel = ViewModelProvider(viewModelStoreOwner)[ReadSentenceMviViewModel::class.java]
        layout.lifecycleOwner = lifecycleOwner
        layout.viewModel = viewModel

        setupListeners()
    }

    private fun setupListeners() {
        viewModel.viewState.observe(layout.lifecycleOwner!!, { viewState ->
            currentDefinition = viewState.wordDefinitionState.asHasWord()?.getCurrentDefinition()
            displayDefinition()
        })

        button_read_sentence__next_definition.setOnClickListener {
            viewModel.handle(ReadSentenceIntent.WordDefinitionIntent.OnNextPressed)
        }
        button_read_sentence__previous_definition.setOnClickListener {
            viewModel.handle(ReadSentenceIntent.WordDefinitionIntent.OnPreviousPressed)
        }
        button_read_sentence__close_definition.setOnClickListener {
            viewModel.handle(ReadSentenceIntent.WordDefinitionIntent.OnClosePressed)
        }
    }

    private fun displayDefinition() {
        currentDefinition?.let { definition ->
            layout_read_sentence__english_definitions.removeAllViews()
            var index = 1
            for (item in definition.senses) {
                val wordDefinitionView = WordDefinitionDetailView(context)
                layout_read_sentence__english_definitions.addView(wordDefinitionView)

                wordDefinitionView.updateDefinition(item.english_definitions.joinToString("; "))
                wordDefinitionView.updatePartsOfSpeech(item.parts_of_speech.joinToString("; "))
                wordDefinitionView.updateTags(item.tags.joinToString("; "))
                wordDefinitionView.updateIndex(index++)
            }
        }
    }
}

