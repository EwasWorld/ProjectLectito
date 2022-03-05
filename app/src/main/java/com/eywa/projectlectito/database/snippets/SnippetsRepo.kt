package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.eywa.projectlectito.database.texts.Text

class SnippetsRepo(private val textSnippetsDao: TextSnippetsDao) {
    val allSnippets = textSnippetsDao.getAllSnippets()

    fun getTextSnippetById(textSnippetId: Int): LiveData<TextSnippet?> {
        return textSnippetsDao.getTextSnippetById(textSnippetId)
    }

    fun getTextSnippetEntry(textId: Int, pageReference: Int): LiveData<List<TextSnippet>> {
        return textSnippetsDao.getTextSnippetEntry(textId, pageReference)
    }

    fun getSnippetsForText(textId: Int): LiveData<List<TextSnippet>> {
        return textSnippetsDao.getSnippetsForText(textId)
    }

    suspend fun insert(item: TextSnippet): Long {
        return textSnippetsDao.insert(item)
    }

    suspend fun update(vararg items: TextSnippet) {
        textSnippetsDao.update(*items)
    }

    suspend fun delete(snippetId: Int) {
        textSnippetsDao.deleteSnippet(snippetId)
    }

    fun getSnippetInfo(
            textId: Int,
            snippetId: Int?,
            surrounding: Int
    ): LiveData<SnippetWithTextAndSurroundingSnippets?> {
        val snippetWithText = if (snippetId == null) {
            textSnippetsDao.getFirstSnippetWithText(textId)
        }
        else {
            textSnippetsDao.getSnippetByIdWithText(snippetId)
        }

        return object : MediatorLiveData<SnippetWithTextAndSurroundingSnippets?>() {
            var nextSource: LiveData<List<TextSnippet>>? = null
            var previousSource: LiveData<List<TextSnippet>>? = null

            init {
                addSource(snippetWithText) {
                    synchronized(this) {
                        if (nextSource != null) {
                            removeSource(nextSource!!)
                        }
                        if (previousSource != null) {
                            removeSource(previousSource!!)
                        }
                    }
                    if (it == null) {
                        postValue(null)
                        return@addSource
                    }
                    setupNextSnippetsSource(it)
                    setupPreviousSnippetsSource(it)
                }
            }

            private fun setupNextSnippetsSource(snippetWithText: TextSnippet.WithText) {
                val newSource: LiveData<List<TextSnippet>>
                synchronized(this) {
                    newSource = textSnippetsDao.getNextSnippets(
                            textId,
                            snippetWithText.snippet.pageReference,
                            snippetWithText.snippet.ordinal,
                            surrounding
                    )
                    if (nextSource != null) {
                        removeSource(nextSource!!)
                    }
                    nextSource = newSource
                }
                addSource(newSource) { nextSnippets ->
                    synchronized(this) {
                        if (nextSource == newSource) {
                            updateValue(
                                    snippetWithText.text,
                                    snippetWithText.snippet,
                                    previousSource?.value ?: listOf(),
                                    nextSnippets ?: listOf()
                            )
                        }
                    }
                }
            }

            private fun setupPreviousSnippetsSource(snippetWithText: TextSnippet.WithText) {
                val newSource: LiveData<List<TextSnippet>>
                synchronized(this) {
                    newSource = textSnippetsDao.getPreviousSnippets(
                            textId,
                            snippetWithText.snippet.pageReference,
                            snippetWithText.snippet.ordinal,
                            surrounding
                    )
                    if (previousSource != null) {
                        removeSource(previousSource!!)
                    }
                    previousSource = newSource
                }
                addSource(newSource) { previousSnippets ->
                    synchronized(this) {
                        if (previousSource == newSource) {
                            updateValue(
                                    snippetWithText.text,
                                    snippetWithText.snippet,
                                    previousSnippets ?: listOf(),
                                    nextSource?.value ?: listOf()
                            )
                        }
                    }
                }
            }

            private fun updateValue(
                    text: Text,
                    currentSnippet: TextSnippet,
                    previousSnippets: List<TextSnippet>,
                    nextSnippets: List<TextSnippet>
            ) {
                postValue(
                        SnippetWithTextAndSurroundingSnippets(
                                previousSnippets.plus(currentSnippet).plus(nextSnippets),
                                text,
                                previousSnippets.size
                        )
                )
            }
        }
    }

    data class SnippetWithTextAndSurroundingSnippets(
            val snippets: List<TextSnippet>,
            val text: Text,
            val prevSnippetsCount: Int
    )
}