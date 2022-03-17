@file:Suppress("unused")

package com.eywa.projectlectito.utils

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.DrawableRes
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

    @BindingAdapter("spannableText")
    @JvmStatic
    fun TextView.setSpannableText(spannableString: SpannableString?) {
        if (spannableString == null
                || spannableString.getSpans(0, spannableString.toString().length, CharacterStyle::class.java).isEmpty()
                || spannableString.getSpans(0, spannableString.toString().length, ClickableSpan::class.java).isEmpty()
        ) {
            text = spannableString ?: ""
        }
        else {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    @BindingAdapter("android:enabled")
    @JvmStatic
    fun View.setEnabled(enabled: Any?) {
        isEnabled = enabled != null
    }

    @BindingAdapter("android:src")
    @JvmStatic
    fun ImageButton.setSrc(@DrawableRes resId: Int?) {
        if (resId == null || resId == ResourcesCompat.ID_NULL) {
            setImageResource(android.R.color.transparent)
        }
        else {
            setImageResource(resId)
        }
    }

    @BindingAdapter("android:contentDescription")
    @JvmStatic
    fun ImageButton.setContentDescription(@StringRes resId: Int?) {
        if (resId == null || resId == ResourcesCompat.ID_NULL) {
            contentDescription = ""
        }
        else {
            contentDescription = resources.getString(resId)
        }
    }
}