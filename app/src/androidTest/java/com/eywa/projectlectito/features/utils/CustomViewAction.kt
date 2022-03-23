package com.eywa.projectlectito.features.utils

import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher
import org.hamcrest.Matchers

object CustomViewAction {
    fun clickClickableSpan(textToClick: CharSequence): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return Matchers.instanceOf(TextView::class.java)
            }

            override fun getDescription(): String {
                return "clicking on a ClickableSpan"
            }

            override fun perform(uiController: UiController?, view: View) {
                val textView = view as TextView
                val spannableString = textView.text as SpannableString
                if (spannableString.isEmpty()) {
                    // TextView is empty, nothing to do
                    throw NoMatchingViewException.Builder()
                            .includeViewHierarchy(true)
                            .withRootView(textView)
                            .build()
                }

                // Get the links inside the TextView and check if we find textToClick
                val spans = spannableString.getSpans(
                        0, spannableString.length,
                        ClickableSpan::class.java
                )
                val matchingSpan = spans.find { span ->
                    val start = spannableString.getSpanStart(span)
                    val end = spannableString.getSpanEnd(span)
                    return@find textToClick == spannableString.substring(start, end)
                } ?: throw NoMatchingViewException.Builder()
                        .includeViewHierarchy(true)
                        .withRootView(textView)
                        .build()

                matchingSpan.onClick(textView)
            }
        }
    }
}