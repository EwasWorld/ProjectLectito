package com.eywa.projectlectito.features.readSentence

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
    MANUAL(
            R.drawable.ic_touch,
            R.string.read_sentence__select_mode_manual,
            R.string.read_sentence__no_definition_manual,
            false
    )
}