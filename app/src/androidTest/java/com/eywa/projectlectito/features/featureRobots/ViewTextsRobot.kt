package com.eywa.projectlectito.features.featureRobots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.eywa.projectlectito.R

class ViewTextsRobot : Robot() {
    override fun checkRobotsScreenIsShown() {
        onView(withId(R.id.button_vt__add_text)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun clickText(textName: String, block: ReadSentenceRobot.() -> Unit): ReadSentenceRobot {
        checkRobotsScreenIsShown()
        onView(withText(textName)).perform(click())
        return ReadSentenceRobot().apply { block() }
    }

    private fun clickMenuItemForText(textName: String, menuItemName: String) {
        checkRobotsScreenIsShown()
        onView(withText(textName)).perform(longClick())
        onView(withText(menuItemName)).perform(click())
    }

    fun clickReadMenuItem(textName: String, block: ReadSentenceRobot.() -> Unit): ReadSentenceRobot {
        clickMenuItemForText(textName, "Read")
        return ReadSentenceRobot().apply { block() }
    }

    fun clickPageBrowserMenuItem(textName: String, block: SnippetBrowserRobot.() -> Unit): SnippetBrowserRobot {
        clickMenuItemForText(textName, "Page browser")
        return SnippetBrowserRobot().apply { block() }
    }

    fun clickFullContentMenuItem(textName: String, block: ReadFullTextRobot.() -> Unit): ReadFullTextRobot {
        clickMenuItemForText(textName, "Full content")
        return ReadFullTextRobot().apply { block() }
    }

    fun clickAddPageMenuItem(textName: String, block: AddSnippetRobot.() -> Unit): AddSnippetRobot {
        clickMenuItemForText(textName, "Add page")
        return AddSnippetRobot().apply { block() }
    }

    fun clickDeleteMenuItem(textName: String) {
        clickMenuItemForText(textName, "Delete")
    }

    fun clickAddTextButton(block: AddTextRobot.() -> Unit): AddTextRobot {
        checkRobotsScreenIsShown()
        onView(withId(R.id.button_vt__add_text)).perform(click())
        return AddTextRobot().apply { block() }
    }

    class AddTextRobot : Robot() {
        override fun checkRobotsScreenIsShown() {
            onView(withId(R.id.input_text_atd__name)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }

        fun inputTextName(textName: String) {
            checkRobotsScreenIsShown()
            onView(withId(R.id.input_text_atd__name)).perform(typeText(textName))
        }

        fun clickOk() {
            checkRobotsScreenIsShown()
            onView(withText("Add")).perform(click())
        }

        fun clickCancel() {
            checkRobotsScreenIsShown()
            onView(withText("Cancel")).perform(click())
        }
    }
}