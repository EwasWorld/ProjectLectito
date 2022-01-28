package com.eywa.projectlectito.database.snippets

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = TextSnippet.TABLE_NAME,
        // Force uniqueness on certain fields to simulate a composite key
        // Using an ID as the primary key as many other tables reference this ID
        indices = [Index(value = ["textId", "pageReference", "ordinal"], unique = true)]
)
data class TextSnippet(
        @PrimaryKey(autoGenerate = true)
        val id: Int,

        /**
         * The actual text for the snippet
         */
        val content: String,

        /*
         * Uniqueness fields, mostly used for ordering
         */
        val textId: Int,
        /**
         * Can be negative as some books include pages using roman numerals before page 1
         */
        val pageReference: Int,
        /**
         * Can be negative as some books include prefixes or prologues before chapter 1
         */
        val chapterId: Int? = null,

        /**
         * Extra field in case there are multiple snippets for a single page reference.
         * 1-indexed
         */
        val ordinal: Int = 1,
) {

    fun getChapterPageString(): String {
        val chapter = if (chapterId != null) "第%d章".format(chapterId) else ""
        val page = "%dページ".format(pageReference)
        return "%s%s".format(chapter, page)
    }

    companion object {
        const val TABLE_NAME = "text_snippets"
    }
}
