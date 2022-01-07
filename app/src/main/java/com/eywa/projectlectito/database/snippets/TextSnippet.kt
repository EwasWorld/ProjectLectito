package com.eywa.projectlectito.database.snippets

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = TextSnippet.TABLE_NAME,
        // Force uniqueness on certain fields to simulate a composite key
        // Using an ID as the primary key as many other tables reference this ID
        indices = [Index(value = ["textId", "chapterId", "pageReference", "ordinal"], unique = true)]
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
         * 1-indexed
         */
        val pageReference: Int,
        /**
         * 1-indexed
         */
        val chapterId: Int? = 1,

        /**
         * Extra field in case there are multiple snippets for a single page reference.
         * 1-indexed
         */
        val ordinal: Int? = 1,
) {
    companion object {
        const val TABLE_NAME = "text_snippets"
    }
}
