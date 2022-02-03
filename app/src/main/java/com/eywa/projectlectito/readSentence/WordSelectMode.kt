package com.eywa.projectlectito.readSentence

import com.eywa.projectlectito.R

enum class WordSelectMode(
        val iconId: Int,
        val iconDescriptionId: Int,
        val noDefinitionStringId: Int,
        val isAuto: Boolean
) {
    AUTO(
            R.drawable.ic_auto_fix,
            R.string.read_sentence__select_mode_auto,
            R.string.read_sentence__no_definition_auto,
            true
    ),
    AUTO_WITH_COLOUR(
            R.drawable.ic_auto_fix,
            R.string.read_sentence__select_mode_auto_with_colour,
            R.string.read_sentence__no_definition_auto,
            true
    ),
    SELECT(
            R.drawable.ic_touch,
            R.string.read_sentence__select_mode_select,
            R.string.read_sentence__no_definition_select,
            false
    ),
    TYPE(
            R.drawable.ic_text_fields,
            R.string.read_sentence__select_mode_type,
            R.string.read_sentence__no_definition_type,
            false
    )
}