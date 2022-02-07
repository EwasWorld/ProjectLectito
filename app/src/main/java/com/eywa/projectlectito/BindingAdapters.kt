package com.eywa.projectlectito

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.databinding.BindingAdapter

object BindingAdapters {
    @BindingAdapter(value = ["visibleOrGone", "invertVisibility"], requireAll = false)
    @JvmStatic
    fun View.setVisibleOrGone(show: Boolean?, invert: Boolean? = false) {
        var showFinal = show ?: false
        if (invert == true) {
            showFinal = !showFinal
        }
        visibility = if (showFinal) VISIBLE else GONE
    }

    @BindingAdapter(value = ["visibleOrGone", "invertVisibility"], requireAll = false)
    @JvmStatic
    fun View.setVisibleOrGone(show: String?, invert: Boolean? = false) {
        setVisibleOrGone(!show.isNullOrBlank(), invert)
    }
}