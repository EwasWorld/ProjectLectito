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
import com.eywa.projectlectito.features.readSentence.ReadSentenceViewModel
import com.eywa.projectlectito.features.readSentence.wordDefinitions.JishoWordDefinitions
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionDetailView
import com.eywa.projectlectito.utils.ToastSpamPrevention
import kotlinx.android.synthetic.main.rs_word_definition.view.*

class RsWordDefinitionView : ConstraintLayout {
    companion object {
        private const val LOG_TAG = "RsWordDefinitionView"
    }

    private lateinit var layout: RsWordDefinitionBinding
    private lateinit var readSentenceViewModel: ReadSentenceViewModel

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
        readSentenceViewModel = ViewModelProvider(viewModelStoreOwner)[ReadSentenceViewModel::class.java]
        layout.lifecycleOwner = lifecycleOwner
        layout.viewState = readSentenceViewModel.wordDefinitionViewState

        setupListeners()
    }

    private fun setupListeners() {
        readSentenceViewModel.wordDefinitionViewState.currDefinition.observe(layout.lifecycleOwner!!, {
            // TODO Handle loading and errors
            currentDefinition = it
            displayDefinition()
        })

        button_read_sentence__next_definition.setOnClickListener {
            val success = readSentenceViewModel.incrementCurrentDefinitionIndex()
            if (!success) {
                ToastSpamPrevention.displayToast(
                        context,
                        resources.getString(R.string.err_read_sentence__no_more_definitions)
                )
            }
        }
        button_read_sentence__previous_definition.setOnClickListener {
            val success = readSentenceViewModel.decrementCurrentDefinitionIndex()
            if (!success) {
                ToastSpamPrevention.displayToast(
                        context,
                        resources.getString(R.string.err_read_sentence__no_more_definitions)
                )
            }
        }

        button_read_sentence__close_definition.setOnClickListener {
            readSentenceViewModel.setSearchWord(null)
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

