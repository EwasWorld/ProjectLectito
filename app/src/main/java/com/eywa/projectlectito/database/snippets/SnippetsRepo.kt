package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData

class SnippetsRepo(private val textSnippetsDao: TextSnippetsDao, private val parsedInfoDao: ParsedInfoDao) {
    val allSnippets = textSnippetsDao.getAllSnippets()

    fun getTextSnippetById(textSnippetId: Int): LiveData<TextSnippet> {
        return textSnippetsDao.getTextSnippetById(textSnippetId)
    }

    fun getTextSnippetEntry(textId: Int, pageReference: Int, chapterId: Int?, ordinal: Int?): LiveData<TextSnippet> {
        return textSnippetsDao.getTextSnippetEntry(textId, pageReference, chapterId, ordinal)
    }

    fun getParsedInfoEntry(startTextSnippetId: Int, startCharacterIndex: Int): LiveData<ParsedInfo> {
        return parsedInfoDao.getParsedInfoById(startTextSnippetId, startCharacterIndex)
    }

    suspend fun insert(item: TextSnippet) {
        textSnippetsDao.insert(item)
    }

    suspend fun insert(item: ParsedInfo) {
        parsedInfoDao.insert(item)
    }

    suspend fun update(vararg items: TextSnippet) {
        textSnippetsDao.update(*items)
    }

    suspend fun update(vararg items: ParsedInfo) {
        parsedInfoDao.update(*items)
    }
}