package com.eywa.projectlectito.database.texts

import androidx.lifecycle.LiveData

class TextsRepo(private val textsDao: TextsDao) {
    fun getTextById(textId: Int): LiveData<Text?> {
        return textsDao.getTextById(textId)
    }

    fun getAllTexts(): LiveData<List<Text>> {
        return textsDao.getAllTexts()
    }

    fun getAllTextsWithSnippetInfo(): LiveData<List<Text.WithCurrentSnippetInfo>> {
        return textsDao.getAllTextsWithSnippetInfo()
    }

    suspend fun insert(item: Text) {
        textsDao.insert(item)
    }

    suspend fun update(vararg items: Text) {
        textsDao.update(*items)
    }
}