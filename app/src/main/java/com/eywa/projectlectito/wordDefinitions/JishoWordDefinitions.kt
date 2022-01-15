package com.eywa.projectlectito.wordDefinitions

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
    )

    data class JishoJapaneseDetail(
            val word: String,
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
            val source: List<String>,
            val info: List<String>
    )

    data class JishoLinkDetail(
            val text: String,
            val url: String
    )

    data class JishoAttributionDetail(
            val jmdict: Boolean,
            val jmnedict: Boolean,
            val dbpedia: Boolean
    )
}