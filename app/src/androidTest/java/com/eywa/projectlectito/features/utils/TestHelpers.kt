package com.eywa.projectlectito.features.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun <T> waitFor(maxWaitTimeMilli: Long, action: () -> T): T {
    val startTime = System.currentTimeMillis()
    while (true) {
        runBlocking { delay(100) }
        try {
            return action()
        }
        catch (e: Throwable) {
            if (System.currentTimeMillis() - startTime > maxWaitTimeMilli) {
                throw e
            }
        }
    }
}