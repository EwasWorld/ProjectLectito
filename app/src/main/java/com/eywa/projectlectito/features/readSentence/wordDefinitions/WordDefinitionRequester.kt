package com.eywa.projectlectito.features.readSentence.wordDefinitions

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import okhttp3.*
import okio.IOException
import ru.gildor.coroutines.okhttp.await

class WordDefinitionRequester(
        private val word: String,
        private val successCallback: (JishoWordDefinitions) -> Unit,
        private val failCallback: (Throwable) -> Unit
) {
    companion object {
        private const val LOG_TAG = "JishoReturnData"
    }

    private lateinit var requestWordDefinitionJob: Job

    init {
        initJob()
    }

    private fun initJob() {
        requestWordDefinitionJob = Job()
        requestWordDefinitionJob.invokeOnCompletion {
            it?.let { e ->
                if (e is CancellationException) {
                    Log.d(LOG_TAG, "Word definition job cancelled")
                    return@invokeOnCompletion
                }

                var message = e.message
                if (message.isNullOrBlank()) {
                    message = "Unknown cancellation error"
                }
                Log.e(LOG_TAG, message)

                failCallback(e)
            }
        }
    }

    suspend fun getDefinition() {
        withContext(Dispatchers.IO + requestWordDefinitionJob) {
            Log.d(LOG_TAG, "Word retrieval invoked. $word")
            val request =
                    // TODO Sanitise
                    Request.Builder().url("https://jisho.org/api/v1/search/words?keyword=$word")
                            .build()
            val httpClient = OkHttpClient()
            val response = httpClient.newCall(request).await()

            Log.d(LOG_TAG, "Request returned")
            ensureActive()
            @Suppress("BlockingMethodInNonBlockingContext")
            // Caused because .string() throws an IO exception
            // Suppress because IO context can handle blocking calls in a coroutine
            val body = response.body?.string() ?: throw IOException("Empty response body")

            try {
                Log.d(LOG_TAG, "Start JSON parsing")
                val finalData = GsonBuilder().create().fromJson(body, JishoWordDefinitions::class.java)
                        ?: throw IllegalStateException("Error parsing dataset")
                Log.d(LOG_TAG, "Finish JSON parsing")
                ensureActive()
                successCallback(finalData)
            }
            catch (e: JsonSyntaxException) {
                throw IllegalStateException("Invalid JSON format: ${e.message}")
            }
        }
    }

    fun cancelParse() {
        // TODO CLEANUP Do I need this guard?
        if (requestWordDefinitionJob.isActive) {
            requestWordDefinitionJob.cancel(CancellationException("New job created"))
        }
    }
}