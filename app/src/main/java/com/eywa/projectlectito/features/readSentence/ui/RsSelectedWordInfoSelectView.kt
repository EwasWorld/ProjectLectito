package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsSelectedWordInfoSelectBinding
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import kotlinx.android.synthetic.main.rs_selected_word_info_select.view.*

class RsSelectedWordInfoSelectView : ConstraintLayout {
    private lateinit var layout: RsSelectedWordInfoSelectBinding
    private lateinit var viewModel: ReadSentenceMviViewModel

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
            LayoutInflater.from(context).inflate(R.layout.rs_selected_word_info_select, this, true)
        }
        else {
            layout = RsSelectedWordInfoSelectBinding.inflate(LayoutInflater.from(context), this, true)
        }
    }

    fun setLifecycleInfo(viewModelStoreOwner: ViewModelStoreOwner, lifecycleOwner: LifecycleOwner) {
        viewModel = ViewModelProvider(viewModelStoreOwner)[ReadSentenceMviViewModel::class.java]
        layout.lifecycleOwner = lifecycleOwner
        layout.viewModel = viewModel

        setupListeners()
    }

    private fun setupListeners() {
        button_read_sentence__selected_info_select__submit.setOnClickListener {
            TODO()
//            if (it.isNullOrBlank()) {
//            ToastSpamPrevention.displayToast(context, resources.getString(failMessageId))
//            }
        }
    }
}

