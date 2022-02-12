package com.eywa.projectlectito.utils

import android.content.res.Resources
import android.view.View
import androidx.core.content.res.ResourcesCompat

fun Boolean.asVisibility(invisibleInsteadOfGone: Boolean = false) = when {
    this -> View.VISIBLE
    invisibleInsteadOfGone -> View.INVISIBLE
    else -> View.GONE
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