package com.eywa.projectlectito.features.utils

import androidx.test.core.app.ActivityScenario
import com.eywa.projectlectito.app.MainActivity
import com.eywa.projectlectito.features.featureRobots.ViewTextsRobot

class ActivityScenarioRobot(block: ViewTextsRobot.() -> Unit) {
    private var activityScenario: ActivityScenario<MainActivity>? = null

    init {
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        ViewTextsRobot().apply { block() }
    }
}