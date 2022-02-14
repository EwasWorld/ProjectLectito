package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData
import androidx.lifecycle.map

class SnippetsRepo(private val textSnippetsDao: TextSnippetsDao) {
    val allSnippets = textSnippetsDao.getAllSnippets()

    fun getTextSnippetById(textSnippetId: Int): LiveData<TextSnippet?> {
        return textSnippetsDao.getTextSnippetById(textSnippetId)
    }

    /**
     * First item is the item just after [currentSnippet], going down the list gets further away from [currentSnippet]
     */
    fun getNextSnippets(
            currentSnippet: TextSnippet,
            snippetsToRetrieve: Int
    ): LiveData<List<TextSnippet>> {
        return textSnippetsDao.getNextSnippets(
                currentSnippet.textId, currentSnippet.pageReference, currentSnippet.ordinal, snippetsToRetrieve
        )
    }

    /**
     * Last item is the item just before [currentSnippet], going back up the list gets further away from [currentSnippet]
     */
    fun getPreviousSnippets(
            currentSnippet: TextSnippet,
            snippetsToRetrieve: Int
    ): LiveData<List<TextSnippet>> {
        return textSnippetsDao.getPreviousSnippets(
                currentSnippet.textId, currentSnippet.pageReference, currentSnippet.ordinal, snippetsToRetrieve
        ).map { it.reversed() }
    }

    fun getFirstSnippetId(textId: Int): LiveData<Int> {
        return textSnippetsDao.getFirstSnippetId(textId)
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
}