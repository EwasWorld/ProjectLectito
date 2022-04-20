package com.eywa.projectlectito.features.readSentence

/**
 * Helper class for creating [DotIndicatorLayoutView]s. Indicates a type of dot to be shown
 */
data class DotIndicatorInfo(
        var isMini: Boolean = false,
        var isSelected: Boolean = false
) {
    companion object {
        /**
         * Calculates what dots are needed to indicate the given info
         *
         * @param totalItems how many total items are in the set being indicated
         * @param selectedIndex index of the selected item (zero-indexed)
         * @param maxDots the maximum number of dots to show (must be odd). If [totalItems] is larger than this, a mini
         * dot will be used to indicate the list continues. The dot indicating the selected item will be kept in the
         * centre where possible
         */
        fun getDotsList(totalItems: Int, selectedIndex: Int, maxDots: Int = 9): List<DotIndicatorInfo> {
            require(maxDots % 2 == 1) { "Max dots should be odd" }
            require(selectedIndex in 0 until totalItems) { "Selected index out of range" }

            val totalDots = totalItems.coerceAtMost(maxDots)
            val inverseSelectedDot = totalItems - selectedIndex - 1
            val centreDot = (maxDots - 1) / 2
            var truncatedStart = false
            var truncatedEnd = false

            val selectedDot = when {
                totalDots == totalItems -> selectedIndex
                selectedIndex <= centreDot -> {
                    truncatedEnd = true
                    selectedIndex
                }
                inverseSelectedDot <= centreDot -> {
                    truncatedStart = true
                    totalDots - inverseSelectedDot - 1
                }
                else -> {
                    truncatedStart = true
                    truncatedEnd = true
                    centreDot
                }
            }

            val dotsList = List(totalDots) { DotIndicatorInfo() }
            if (truncatedStart) {
                dotsList.first().isMini = true
            }
            if (truncatedEnd) {
                dotsList.last().isMini = true
            }
            dotsList[selectedDot].isSelected = true

            return dotsList
        }
    }
}