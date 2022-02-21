package com.eywa.projectlectito.features.readSentence.mvi

import android.content.Context
import androidx.annotation.StringRes

sealed class ReadSentenceEffect {
    sealed class Toast : ReadSentenceEffect() {
        data class StringToast(private val message: String) : Toast() {
            override fun getMessage(context: Context): String = message
        }

        data class ResIdToast(@StringRes private val messageId: Int) : Toast() {
            override fun getMessage(context: Context): String = context.resources.getString(messageId)
        }

        abstract fun getMessage(context: Context): String
    }
}
