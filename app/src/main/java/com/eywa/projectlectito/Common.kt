package com.eywa.projectlectito

import android.view.View

fun Boolean.asVisibility(invisibleInsteadOfGone: Boolean = false) = when {
    this -> View.VISIBLE
    invisibleInsteadOfGone -> View.INVISIBLE
    else -> View.GONE
}

class Common {
}