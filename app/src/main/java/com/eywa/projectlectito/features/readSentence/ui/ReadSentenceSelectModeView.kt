package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsSelectModeViewBinding
import com.eywa.projectlectito.features.readSentence.WordSelectMode
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import kotlinx.android.synthetic.main.rs_select_mode_view.view.*

class ReadSentenceSelectModeView : ConstraintLayout {
    private lateinit var layout: RsSelectModeViewBinding
    private lateinit var viewModel: ReadSentenceMviViewModel

    private var isOpen = false
    var selectMenuOpenedListener: (() -> Unit)? = null

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
            LayoutInflater.from(context).inflate(R.layout.rs_select_mode_view, this, true)
        }
        else {
            layout = RsSelectModeViewBinding.inflate(LayoutInflater.from(context), this, true)
        }

        fab_read_sentence__select_mode_main.setOnClickListener {
            viewModel.handle(ReadSentenceIntent.OnWordSelectModeMenuStateChange(!isOpen))
        }

        listOf(
                ModeButton(
                        fab_read_sentence__select_mode_auto,
                        text_read_sentence__select_mode_auto,
                        WordSelectMode.AUTO
                ),
                ModeButton(
                        fab_read_sentence__select_mode_auto_with_colour,
                        text_read_sentence__select_mode_auto_with_colour,
                        WordSelectMode.AUTO_WITH_COLOUR
                ),
                ModeButton(
                        fab_read_sentence__select_mode_select,
                        text_read_sentence__select_mode_select,
                        WordSelectMode.SELECT
                ),
                ModeButton(
                        fab_read_sentence__select_mode_type,
                        text_read_sentence__select_mode_type,
                        WordSelectMode.TYPE
                )
        ).forEach { button ->
            button.fab.setOnClickListener { onButtonPressed(button.mode) }
            button.text.setOnClickListener { onButtonPressed(button.mode) }
        }
    }

    fun setLifecycleInfo(viewModelStoreOwner: ViewModelStoreOwner, lifecycleOwner: LifecycleOwner) {
        viewModel = ViewModelProvider(viewModelStoreOwner)[ReadSentenceMviViewModel::class.java]
        layout.lifecycleOwner = lifecycleOwner
        layout.viewModel = viewModel

        setupListeners()
    }

    private fun setupListeners() {
        viewModel.viewState.observe(layout.lifecycleOwner!!, {
            val menuOpen = it.isSelectModeMenuOpen
            if (menuOpen == isOpen) return@observe

            isOpen = menuOpen
            if (isOpen) {
                selectMenuOpenedListener?.invoke()
            }
        })
    }

    private fun onButtonPressed(selectMode: WordSelectMode) {
        viewModel.handle(ReadSentenceIntent.SelectedWordIntent.OnWordSelectModeChanged(selectMode))
    }

    data class ModeButton(
            val fab: ImageButton,
            val text: TextView,
            val mode: WordSelectMode
    )
}