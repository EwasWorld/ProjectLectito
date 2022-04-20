package com.eywa.projectlectito

import com.eywa.projectlectito.features.readSentence.DotIndicatorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DotsUnitTests {
    @Test
    fun `test no truncation - low selected item`() {
        compareDotsLists(
                MAX_DOTS,
                1,
                listOf(
                        DotType.NORMAL,
                        DotType.SELECTED,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL
                )
        )
    }

    @Test
    fun `test no truncation - high selected item`() {
        compareDotsLists(
                MAX_DOTS,
                MAX_DOTS - 2,
                listOf(
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.SELECTED,
                        DotType.NORMAL
                )
        )
    }

    @Test
    fun `test end truncation`() {
        compareDotsLists(
                MAX_DOTS + 5,
                1,
                listOf(
                        DotType.NORMAL,
                        DotType.SELECTED,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.SMALL
                )
        )
    }

    @Test
    fun `test start truncation`() {
        compareDotsLists(
                MAX_DOTS + 5,
                MAX_DOTS + 5 - 2,
                listOf(
                        DotType.SMALL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.SELECTED,
                        DotType.NORMAL
                )
        )
    }

    @Test
    fun `test truncation both ends`() {
        compareDotsLists(
                MAX_DOTS * 2,
                MAX_DOTS,
                listOf(
                        DotType.SMALL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.SELECTED,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.NORMAL,
                        DotType.SMALL
                )
        )
    }

    private fun compareDotsLists(totalItems: Int, selectedItem: Int, expected: List<DotType>) {
        val actual = DotIndicatorInfo.getDotsList(totalItems, selectedItem, MAX_DOTS)
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals("index $i", expected[i].isMini, actual[i].isMini)
            assertEquals("index $i", expected[i].isSelected, actual[i].isSelected)
        }
    }

    enum class DotType(val isMini: Boolean, val isSelected: Boolean) {
        SMALL(true, false),
        NORMAL(false, false),
        SELECTED(false, true)
    }

    companion object {
        const val MAX_DOTS = 9
    }
}