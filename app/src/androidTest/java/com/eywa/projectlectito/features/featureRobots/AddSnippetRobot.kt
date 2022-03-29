package com.eywa.projectlectito.features.featureRobots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.eywa.projectlectito.R
import org.hamcrest.core.AllOf.allOf

class AddSnippetRobot : Robot() {
    override fun checkRobotsScreenIsShown() {
        onView(withId(R.id.text_add_snippet__snippet_content)).check(matches(isDisplayed()))
    }

    private fun typeTextIntoDetail(topLevelResId: Int, textToType: String) {
        onView(
                allOf(
                        isDescendantOfA(withId(topLevelResId)),
                        withId(R.id.input_asd__value)
                )
        ).perform(replaceText(textToType))
    }

    fun inputChapter(chapter: String) {
        checkRobotsScreenIsShown()
        typeTextIntoDetail(R.id.layout_add_snippet__chapter, chapter)
    }

    fun inputPage(page: String) {
        checkRobotsScreenIsShown()
        typeTextIntoDetail(R.id.layout_add_snippet__page_number, page)
    }

    fun inputContent(content: String) {
        checkRobotsScreenIsShown()
        onView(withId(R.id.text_add_snippet__snippet_content)).perform(replaceText(content))
    }

    fun clickAdd() {
        checkRobotsScreenIsShown()
        onView(withId(R.id.button_add_snippet__submit)).perform(click())
    }

    fun clickAddAndAddAnother() {
        checkRobotsScreenIsShown()
        onView(withId(R.id.button_add_snippet__submit_and_another)).perform(click())
    }
}