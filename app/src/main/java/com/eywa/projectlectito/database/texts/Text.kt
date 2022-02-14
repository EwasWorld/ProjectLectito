package com.eywa.projectlectito.database.texts

import androidx.annotation.VisibleForTesting
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
            val totalSnippets: Int,
            /**
             * The number of snippets before [currentSnippet]
             */
            @VisibleForTesting
            val readSnippets: Int
    ) {
        val percentageRead: Double?
            get() {
                when {
                    text.isComplete -> return null
                    text.currentSnippetId == null -> return null
                    else -> {
                        var progress = readSnippets.toDouble() / totalSnippets.toDouble()
                        if (text.currentCharacterIndex != null) {
                            val progressThroughCurrent =
                                    text.currentCharacterIndex.toDouble() / currentSnippet!!.content.length.toDouble()
                            progress += progressThroughCurrent / totalSnippets.toDouble()
                        }
                        return progress
                    }
                }
            }
    }
}