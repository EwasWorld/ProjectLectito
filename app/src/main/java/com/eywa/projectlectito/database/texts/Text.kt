package com.eywa.projectlectito.database.texts

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eywa.projectlectito.database.snippets.TextSnippet

@Entity(tableName = Text.TABLE_NAME)
data class Text(
        @PrimaryKey(autoGenerate = true)
        val id: Int,
        val name: String,

        /**
         * Which snippet the user is currently at. A bookmark if you like
         */
        val currentSnippetId: Int? = null,
        /**
         * Which character within [currentSnippetId] the user is currently at. A bookmark if you like
         */
        val currentCharacterIndex: Int? = null,
        val isComplete: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "texts"
    }

    data class WithCurrentSnippetInfo(
            @Embedded(prefix = "txt_") val text: Text,
            @Embedded val currentSnippet: TextSnippet? = null,
            private val totalSnippets: Int,
            private val readSnippets: Int
    ) {
        val percentageRead: Double
            get() {
                when {
                    text.isComplete -> return 1.0
                    text.currentSnippetId == null -> return 0.0
                    else -> return readSnippets.toDouble() / totalSnippets.toDouble()
                }
            }
    }
}