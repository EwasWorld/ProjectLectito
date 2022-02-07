package com.eywa.projectlectito.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsSelectedWordInfoParsedBinding
import com.eywa.projectlectito.readSentence.ReadSentenceViewModel
import com.eywa.projectlectito.readSentence.WordSelectMode

class RsSelectedWordInfoParsedView : ConstraintLayout {
    companion object {
        private const val LOG_TAG = "RsSelWordInfoParsedView"
    }

    private lateinit var layout: RsSelectedWordInfoParsedBinding
    private lateinit var readSentenceViewModel: ReadSentenceViewModel

    private var wordSelectMode: WordSelectMode? = null
    private var selectedWord: String? = null

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
            LayoutInflater.from(context).inflate(R.layout.rs_selected_word_info_parsed, this, true)
        }
        else {
            layout = RsSelectedWordInfoParsedBinding.inflate(LayoutInflater.from(context), this, true)
        }
    }

    fun setLifecycleInfo(viewModelStoreOwner: ViewModelStoreOwner, lifecycleOwner: LifecycleOwner) {
        readSentenceViewModel = ViewModelProvider(viewModelStoreOwner)[ReadSentenceViewModel::class.java]
        layout.lifecycleOwner = lifecycleOwner
        layout.viewState = readSentenceViewModel.selectedWordParsedViewState

        setupListeners()
    }

    // TODO Remove
    private fun setupListeners() {
        readSentenceViewModel.selectedWord.observe(layout.lifecycleOwner!!, { newWord ->
            selectedWord = newWord
        })

        readSentenceViewModel.wordSelectMode.observe(layout.lifecycleOwner!!, { newMode ->
            wordSelectMode = newMode
        })
    }
}

