package com.eywa.projectlectito.readSentence

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.eywa.projectlectito.R
import com.eywa.projectlectito.asVisibility
import kotlinx.android.synthetic.main.read_sentence_select_mode_view.view.*

class ReadSentenceSelectModeView : ConstraintLayout {
    private var isOpen = false
        set(value) {
            field = value
            setMenuOpen(value)
        }
    private lateinit var buttons: List<ModeButton>

    var overlays: List<View>? = null
        set(value) {
            field = value
            setMenuOpen(isOpen)
            overlays?.forEach {
                it.setOnClickListener {
                    isOpen = false
                }
            }
        }
    var selectModeChangedListener: ((ReadSentenceViewModel.WordSelectMode) -> Unit)? = null
    var selectMenuOpenedListener: (() -> Unit)? = null

    constructor(context: Context) : super(context) {
        initialise(context, null)
    }

    constructor(
            context: Context,
            attrs: AttributeSet
    ) : super(context, attrs) {
        initialise(context, attrs)
    }

    constructor(
            context: Context,
            attrs: AttributeSet,
            defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initialise(context, attrs)
    }

    private fun initialise(context: Context, attrs: AttributeSet?) {
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.read_sentence_select_mode_view, this, true) as ConstraintLayout

        buttons = listOf(
                ModeButton(
                        fab_read_sentence__select_mode_auto,
                        text_read_sentence__select_mode_auto,
                        ReadSentenceViewModel.WordSelectMode.AUTO
                ),
                ModeButton(
                        fab_read_sentence__select_mode_auto_with_colour,
                        text_read_sentence__select_mode_auto_with_colour,
                        ReadSentenceViewModel.WordSelectMode.AUTO_WITH_COLOUR
                ),
                ModeButton(
                        fab_read_sentence__select_mode_select,
                        text_read_sentence__select_mode_select,
                        ReadSentenceViewModel.WordSelectMode.SELECT
                ),
                ModeButton(
                        fab_read_sentence__select_mode_type,
                        text_read_sentence__select_mode_type,
                        ReadSentenceViewModel.WordSelectMode.TYPE
                )
        )

        fab_read_sentence__select_mode_main.setOnClickListener {
            isOpen = !isOpen
            if (isOpen) {
                selectMenuOpenedListener?.invoke()
            }
        }

        buttons.forEach { button ->
            button.fab.setOnClickListener { onButtonPressed(button.mode) }
            button.text.setOnClickListener { onButtonPressed(button.mode) }
        }
    }

    fun setInitialState(selectMode: ReadSentenceViewModel.WordSelectMode) {
        if (fab_read_sentence__select_mode_main.contentDescription.isNullOrBlank()) {
            fab_read_sentence__select_mode_main.setImageResource(selectMode.iconId)
            fab_read_sentence__select_mode_main.contentDescription = resources.getString(selectMode.iconDescriptionId)
        }
    }

    private fun onButtonPressed(selectMode: ReadSentenceViewModel.WordSelectMode) {
        fab_read_sentence__select_mode_main.setImageResource(selectMode.iconId)
        fab_read_sentence__select_mode_main.contentDescription = resources.getString(selectMode.iconDescriptionId)
        isOpen = false
        selectModeChangedListener?.invoke(selectMode)
    }

    private fun setMenuOpen(isMenuOpen: Boolean) {
        if (isOpen != isMenuOpen) {
            isOpen = isMenuOpen
            // setter of isOpen will call this function again with the new value
            return
        }

        buttons.forEach { button ->
            button.fab.visibility = isMenuOpen.asVisibility()
            button.text.visibility = isMenuOpen.asVisibility()
        }

        overlays?.forEach {
            it.visibility = isMenuOpen.asVisibility()
        }
    }

    data class ModeButton(
            val fab: ImageButton,
            val text: TextView,
            val mode: ReadSentenceViewModel.WordSelectMode
    )
}