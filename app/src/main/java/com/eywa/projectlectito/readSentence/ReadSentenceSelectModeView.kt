package com.eywa.projectlectito.readSentence

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
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

        fab_read_sentence__select_mode_main.setOnClickListener {
            isOpen = !isOpen
            if (isOpen) {
                selectMenuOpenedListener?.invoke()
            }
        }

        fab_read_sentence__select_mode_auto.setOnClickListener {
            onButtonPressed(ReadSentenceViewModel.WordSelectMode.AUTO)
        }
        fab_read_sentence__select_mode_select.setOnClickListener {
            onButtonPressed(ReadSentenceViewModel.WordSelectMode.SELECT)
        }
        fab_read_sentence__select_mode_type.setOnClickListener {
            onButtonPressed(ReadSentenceViewModel.WordSelectMode.TYPE)
        }
        text_read_sentence__select_mode_auto.setOnClickListener {
            onButtonPressed(ReadSentenceViewModel.WordSelectMode.AUTO)
        }
        text_read_sentence__select_mode_select.setOnClickListener {
            onButtonPressed(ReadSentenceViewModel.WordSelectMode.SELECT)
        }
        text_read_sentence__select_mode_type.setOnClickListener {
            onButtonPressed(ReadSentenceViewModel.WordSelectMode.TYPE)
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

        fab_read_sentence__select_mode_auto.visibility = isMenuOpen.asVisibility()
        fab_read_sentence__select_mode_select.visibility = isMenuOpen.asVisibility()
        fab_read_sentence__select_mode_type.visibility = isMenuOpen.asVisibility()
        text_read_sentence__select_mode_auto.visibility = isMenuOpen.asVisibility()
        text_read_sentence__select_mode_select.visibility = isMenuOpen.asVisibility()
        text_read_sentence__select_mode_type.visibility = isMenuOpen.asVisibility()

        overlays?.forEach {
            it.visibility = isMenuOpen.asVisibility()
        }
    }
}