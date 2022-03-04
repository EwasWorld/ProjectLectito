package com.eywa.projectlectito.utils

import android.content.res.Resources
import android.view.View
import androidx.core.content.res.ResourcesCompat

fun Boolean.asVisibility(invisibleInsteadOfGone: Boolean = false) = when {
    this -> View.VISIBLE
    invisibleInsteadOfGone -> View.INVISIBLE
    else -> View.GONE
}

/**
 * @return a new list with element at [index] replaced with [newItem].
 * If the list is empty, returns a new single-element list.
 * If [index] is out of bounds, returns the original list.
 */
fun <T> List<T>.replaceElementAt(index: Int, newItem: T): List<T> {
    return takeIf { isNotEmpty() }
            ?.mapIndexed { i, item -> if (i == index) newItem else item }
            ?: listOf(newItem)
}

fun Resources.getColorByTheme(colourResourceId: Int, theme: Resources.Theme): Int {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        this.getColor(colourResourceId, theme)
    }
    else {
        ResourcesCompat.getColor(this, colourResourceId, theme)
    }
}

const val JAPANESE_LIST_DELIMINATOR = "・"

class Common {
}