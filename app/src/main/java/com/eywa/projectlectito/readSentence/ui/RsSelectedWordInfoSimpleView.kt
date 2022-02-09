package com.eywa.projectlectito.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.eywa.projectlectito.R
import com.eywa.projectlectito.ToastSpamPrevention
import com.eywa.projectlectito.databinding.RsSelectedWordInfoSimpleBinding
import com.eywa.projectlectito.readSentence.ReadSentenceViewModel
import com.eywa.projectlectito.readSentence.WordSelectMode
import kotlinx.android.synthetic.main.rs_selected_word_info_simple.view.*

class RsSelectedWordInfoSimpleView : ConstraintLayout {
    companion object {
        private const val LOG_TAG = "RsSelWordInfoSimpleView"
    }

    private lateinit var layout: RsSelectedWordInfoSimpleBinding
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
            LayoutInflater.from(context).inflate(R.layout.rs_selected_word_info_simple, this, true)
        }
        else {
            layout = RsSelectedWordInfoSimpleBinding.inflate(LayoutInflater.from(context), this, true)
        }
    }

    fun setLifecycleInfo(viewModelStoreOwner: ViewModelStoreOwner, lifecycleOwner: LifecycleOwner) {
        readSentenceViewModel = ViewModelProvider(viewModelStoreOwner)[ReadSentenceViewModel::class.java]
        layout.lifecycleOwner = lifecycleOwner
        layout.viewState = readSentenceViewModel.selectedWordSimpleViewState

        setupListeners()
    }

    private fun setupListeners() {
        readSentenceViewModel.wordSelectMode.observe(layout.lifecycleOwner!!, { newMode ->
            wordSelectMode = newMode
        })

        readSentenceViewModel.selectedWord.observe(layout.lifecycleOwner!!, { newWord ->
            selectedWord = newWord
            val selectedWordIndicatorText = if (newWord.isNullOrBlank()) {
                resources.getString(R.string.read_sentence__select_word_hint)
            }
            else {
                newWord
            }
            text_read_sentence__selected_simple_word.text = selectedWordIndicatorText
        })

        button_read_sentence__submit_word.setOnClickListener {
            when (wordSelectMode) {
                WordSelectMode.SELECT -> {
                    selectedWord.getDefinition(R.string.read_sentence__simple_selected__submit_no_word_select)
                }
                WordSelectMode.TYPE -> {
                    input_text_read_sentence__selected_simple_word.text.toString()
                            .getDefinition(R.string.read_sentence__simple_selected__submit_no_word_type)
                }
                else -> {
                    Log.w(LOG_TAG, "Submit called on bad type")
                    ToastSpamPrevention.displayToast(
                            context,
                            resources.getString(R.string.read_sentence__simple_selected__submit_bad_type)
                    )
                }
            }
        }
    }

    private fun String?.getDefinition(failMessageId: Int) {
        if (this.isNullOrBlank()) {
            ToastSpamPrevention.displayToast(context, resources.getString(failMessageId))
            return
        }

        readSentenceViewModel.setSearchWord(this)
    }
}

