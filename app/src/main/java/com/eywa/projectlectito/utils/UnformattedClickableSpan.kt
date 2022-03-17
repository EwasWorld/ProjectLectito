package com.eywa.projectlectito.utils

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class UnformattedClickableSpan(private val onClickListener: (View) -> Unit) : ClickableSpan() {
    override fun onClick(widget: View) {
        onClickListener(widget)
    }

    override fun updateDrawState(ds: TextPaint) {
        // Don't underline or highlight the text
    }
}