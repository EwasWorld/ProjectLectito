package com.eywa.projectlectito.database.snippets

import androidx.annotation.StringRes
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eywa.projectlectito.R
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.features.readSentence.Sentence

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
        val part = if (ordinal != 1) " パート%d".format(ordinal) else ""
        return "%s%s%s".format(chapter, page, part)
    }

    data class WithInt(
            @Embedded val snippet: TextSnippet,
            val extraInfo: Int
    )

    data class WithText(
            @Embedded(prefix = "txt_") val text: Text,
            @Embedded val snippet: TextSnippet,
    )

    companion object {
        const val TABLE_NAME = "text_snippets"

        /**
         * @return list of [StringRes] reasons why the content is not valid
         */
        fun isValidContent(content: String?): List<Int> {
            val errors = mutableListOf<Int>()
            val empty = content.isNullOrBlank()

            if (empty) {
                errors.add(R.string.err__required_field)
            }
            if (empty || !Sentence.containsNonStopCharacter(content!!)) {
                errors.add(R.string.err_bad_snippet_content__no_non_sentence_ends)
            }
            if (empty || !Sentence.containsHardStopCharacter(content!!)) {
                errors.add(R.string.err_bad_snippet_content__no_sentence_end)
            }

            return errors
        }
    }
}
