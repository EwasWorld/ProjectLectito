@file:Suppress("unused")

package com.eywa.projectlectito

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
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

    @BindingAdapter(value = ["visibleOrGone", "invertVisibility"], requireAll = false)
    @JvmStatic
    fun View.setVisibleOrGone(show: Any?, invert: Boolean? = false) {
        setVisibleOrGone(show != null, invert)
    }

    @BindingAdapter("android:text")
    @JvmStatic
    fun TextView.setText(@StringRes resId: Int?) {
        if (resId == null || resId == ResourcesCompat.ID_NULL) {
            text = ""
        }
        else {
            setText(resId)
        }
    }
}