package com.eywa.projectlectito.features.featureRobots

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.eywa.projectlectito.R
import com.eywa.projectlectito.features.utils.CustomViewAction
import com.eywa.projectlectito.features.utils.waitFor
import com.eywa.projectlectito.features.utils.waitForDisplay

class ReadSentenceRobot : Robot() {
    override fun checkRobotsScreenIsShown() {
        onView(withId(R.id.text_read_sentence__sentence)).waitForDisplay()
    }

    private fun changeWordSelectionMode(@IdRes desiredModeButtonId: Int) {
        checkRobotsScreenIsShown()
        onView(withId(R.id.button_read_sentence__select_mode)).waitForDisplay().perform(click())
        onView(withId(desiredModeButtonId)).waitForDisplay().perform(click())
    }

    fun setSelectModeAuto() {
        changeWordSelectionMode(R.id.fab_read_sentence__select_mode_auto)
    }

    fun setSelectModeAutoColoured() {
        changeWordSelectionMode(R.id.fab_read_sentence__select_mode_auto_with_colour)
    }

    fun setSelectModeManual() {
        changeWordSelectionMode(R.id.fab_read_sentence__select_mode_manual)
    }

    fun clickViewFullText(): ReadFullTextRobot {
        checkRobotsScreenIsShown()
        onView(withId(R.id.button_read_sentence__full_text)).perform(click())
        return ReadFullTextRobot()
    }

    fun clickEdit(textToClick: String?) {
        TODO()
    }

    fun clickParsedWord(spanText: String) {
        waitFor(1000) {
            onView(withId(R.id.text_read_sentence__sentence))
                    .perform(CustomViewAction.clickClickableSpan(spanText))
        }
    }
}