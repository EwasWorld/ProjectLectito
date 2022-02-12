package com.eywa.projectlectito.features.readSentence.wordDefinitions

import com.eywa.projectlectito.database.word.StudyingWord

data class JishoWordDefinitions(
        val meta: JishoMetadataDetail,
        val data: List<JishoEntry>
) {
    data class JishoMetadataDetail(
            val status: Int
    )

    data class JishoEntry(
            val slug: String,
            val is_common: Boolean,
            val tags: List<String>,
            val jlpt: List<String>,
            val japanese: List<JishoJapaneseDetail>,
            val senses: List<JishoSensesDetail>,
            val attribution: JishoAttributionDetail
    ) {
        private constructor(jishoEntry: JishoEntry, sensesIndex: Int) : this(
                jishoEntry.slug,
                jishoEntry.is_common,
                jishoEntry.tags,
                jishoEntry.jlpt,
                jishoEntry.japanese,
                listOf(jishoEntry.senses[sensesIndex]),
                jishoEntry.attribution
        )

        fun toStudyingWord(senseIndex: Int): StudyingWord {
            return StudyingWord(
                    0,
                    slug,
                    is_common,
                    jlpt.map { Integer.parseInt(it.last().toString()) },
                    JishoEntry(this, senseIndex),
                    StudyingWord.State.LEARNING
            )
        }
    }

    data class JishoJapaneseDetail(
            val word: String?,
            val reading: String
    )

    data class JishoSensesDetail(
            val english_definitions: List<String>,
            val parts_of_speech: List<String>,
            val links: List<JishoLinkDetail>,
            val tags: List<String>,
            val restrictions: List<String>,
            val see_also: List<String>,
            val antonyms: List<String>,
            val source: List<JishoSourceDetail>,
            val info: List<String>
    )

    data class JishoLinkDetail(
            val text: String,
            val url: String
    )

    data class JishoSourceDetail(
            val language: String,
            val word: String
    )

    data class JishoAttributionDetail(
            val jmdict: Boolean,
            val jmnedict: Boolean,
            val dbpedia: Boolean
    )
}