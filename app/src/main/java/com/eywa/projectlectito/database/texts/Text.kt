package com.eywa.projectlectito.database.texts

import androidx.room.Entity
import androidx.room.PrimaryKey

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
}