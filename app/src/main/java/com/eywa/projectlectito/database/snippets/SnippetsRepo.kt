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
            var currentSource: LiveData<List<TextSnippet.WithInt>>? = null

            init {
                addSource(snippetWithText) {
                    synchronized(this) {
                        if (currentSource != null) {
                            removeSource(currentSource!!)
                        }
                    }
                    if (it == null) {
                        postValue(null)
                        return@addSource
                    }
                    update(it)
                }
            }

            private fun update(snippetWithText: TextSnippet.WithText) {
                val newSource: LiveData<List<TextSnippet.WithInt>>
                synchronized(this) {
                    newSource = textSnippetsDao.getWithSurroundingSnippets(
                            textId,
                            snippetWithText.snippet.pageReference,
                            snippetWithText.snippet.ordinal,
                            surrounding
                    )
                    if (currentSource != null) {
                        removeSource(currentSource!!)
                    }
                    currentSource = newSource
                }
                addSource(newSource) { snippetsWithInts ->
                    if (snippetsWithInts.isNullOrEmpty()) {
                        postValue(null)
                        return@addSource
                    }

                    check(snippetsWithInts.distinctBy { it.extraInfo }.size == 1) { "Multiple snippet counts" }
                    synchronized(this) {
                        if (currentSource == newSource) {
                            postValue(
                                    SnippetWithTextAndSurroundingSnippets(
                                            snippetsWithInts.map { it.snippet },
                                            snippetWithText.text,
                                            snippetsWithInts.first().extraInfo
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    data class SnippetWithTextAndSurroundingSnippets(
            val snippets: List<TextSnippet>,
            val text: Text,
            val prevSnippetsCount: Int
    )
}