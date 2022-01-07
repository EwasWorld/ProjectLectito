package com.eywa.projectlectito.database.snippets

import androidx.room.Entity

@Entity(tableName = ParsedInfo.TABLE_NAME, primaryKeys = ["startTextSnippetId", "startCharacterIndex"])
data class ParsedInfo(
        /*
         * Which phrase this parsed info belongs to
         */
        val startTextSnippetId: Int,
        val startCharacterIndex: Int,
        val endTextSnippetId: Int,
        val endCharacterIndex: Int,

        val dictionaryForm: String,
        val partsOfSpeech: List<String>,
        val pitchAccentPattern: Int
) {
    companion object {
        const val TABLE_NAME = "parsed_info"
    }
}
