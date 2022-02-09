package com.eywa.projectlectito.readSentence.ui

import android.content.Context
import android.util.AttributeSet

typealias SelectionChangedListener = (selStart: Int, selEnd: Int) -> Unit

class SelectableTextView : androidx.appcompat.widget.AppCompatTextView {
    private val selectionChangedListeners = mutableSetOf<SelectionChangedListener>()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        selectionChangedListeners.forEach { it.invoke(selStart, selEnd) }
    }

    fun addSelectionChangedListener(listener: SelectionChangedListener) {
        selectionChangedListeners.add(listener)
    }
}

