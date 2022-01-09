package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData

class SnippetsRepo(private val textSnippetsDao: TextSnippetsDao) {
    fun getTextSnippetById(textSnippetId: Int): LiveData<TextSnippet> {
        return textSnippetsDao.getTextSnippetById(textSnippetId)
    }

    fun getTextSnippetEntry(textId: Int, pageReference: Int, chapterId: Int?, ordinal: Int?): LiveData<TextSnippet> {
        return textSnippetsDao.getTextSnippetEntry(textId, pageReference, chapterId, ordinal)
    }

    suspend fun insert(item: TextSnippet) {
        textSnippetsDao.insert(item)
    }

    suspend fun update(vararg items: TextSnippet) {
        textSnippetsDao.update(*items)
    }
}