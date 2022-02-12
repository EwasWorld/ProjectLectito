package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.iterator

typealias SelectionChangedListener = (selStart: Int, selEnd: Int) -> Unit

class SelectableTextView : androidx.appcompat.widget.AppCompatTextView {
    private val selectionChangedListeners = mutableSetOf<SelectionChangedListener>()

    constructor(context: Context) : super(context) {
        initialise()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialise()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialise()
    }

    private fun initialise() {
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                return true
            }

            override fun onPrepareActionMode(p0: ActionMode?, menu: Menu?): Boolean {
                if (menu == null) return false

                // Removes all but these items
                val desiredItems = setOf(android.R.id.selectAll, android.R.id.copy)

                val toRemove = mutableSetOf<Int>()
                menu.iterator().forEach {
                    if (!desiredItems.contains(it.itemId)) {
                        toRemove.add(it.itemId)
                    }
                }
                toRemove.forEach {
                    menu.removeItem(it)
                }

                return toRemove.isNotEmpty()
            }

            override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
                return false
            }

            override fun onDestroyActionMode(p0: ActionMode?) {
                return
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        selectionChangedListeners.forEach { it.invoke(selStart, selEnd) }
    }

    fun addSelectionChangedListener(listener: SelectionChangedListener) {
        selectionChangedListeners.add(listener)
    }

}

