package com.eywa.projectlectito.features.utils

import android.view.View
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

fun ViewInteraction.matches(matcher: Matcher<View>) = check(ViewAssertions.matches(matcher))
fun ViewInteraction.isDisplayed() = matches(ViewMatchers.isDisplayed())
fun ViewInteraction.waitForDisplay(maxWaitTimeMilli: Long = 1000): ViewInteraction =
        waitFor(maxWaitTimeMilli) { isDisplayed() }