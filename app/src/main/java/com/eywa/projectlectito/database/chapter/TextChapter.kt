package com.eywa.projectlectito.database.chapter

import androidx.room.Entity

@Entity(tableName = TextChapter.TABLE_NAME, primaryKeys = ["textId", "chapterNumber"])
data class TextChapter(
        /**
         * The actual text for the snippet
         */
        val name: String,
        val textId: Int,

        /**
         * Can be negative as some books include prefixes or prologues before chapter 1
         */
        val chapterNumber: Int,
) {
    companion object {
        const val TABLE_NAME = "text_chapters"
    }
}