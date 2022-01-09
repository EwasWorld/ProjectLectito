package com.eywa.projectlectito.readSentence

data class ParsedInfo(
        /*
         * Which phrase this parsed info belongs to
         */
        val startTextSnippetId: Int,
        val startCharacterIndex: Int,
        val endTextSnippetId: Int,

        /**
         * Last character in the string (inclusive)
         */
        val endCharacterIndex: Int,

        val dictionaryForm: String,
        val partsOfSpeech: List<String>,
        val pitchAccentPattern: Int?
)
