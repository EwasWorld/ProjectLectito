package com.eywa.projectlectito

import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.features.readSentence.Sentence
import com.eywa.projectlectito.features.readSentence.TempTestData
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class SentenceUnitTests {
    @Test
    fun `test current sentence - simple`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえおかきくけこたちつてとさしすせそはひふへほ", 1, 1),
                0
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - simple mid-sentence start`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえおかきくけこたちつてとさしすせそはひふへほ", 1, 1),
                5
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - current on break char`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえおかきくけこ\n\n。たちつてとさしすせそはひふへほ", 1, 1),
                10
        )
        Assert.assertEquals("たちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals("あいうえおかきくけこ。", sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - current on break char and no text after`() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Sentence(
                    TextSnippet(1, "あいうえおかきくけこ\n\n。", 1, 1),
                    11
            )
        }
    }

    @Test
    fun `test current sentence - current on break char and no text after - has next snippet`() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Sentence(
                    TextSnippet(1, "あいうえおかきくけこ\n\n。", 1, 1),
                    11,
                    nextSnippets = listOf(TextSnippet(2, "たちつてとさしすせそはひふへほ", 1, 2))
            )
        }
    }

    @Test
    fun `test current sentence - up to first break`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえお\n。かきくけこたちつてとさしすせそはひふへほ", 1, 1),
                0
        )
        Assert.assertEquals("あいうえお。", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(7, 1), sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - with next snippet - hard stop at end`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえお。", 1, 1),
                0,
                nextSnippets = listOf(TextSnippet(2, "かきくけこたちつてとさしすせそはひふへほ", 1, 2))
        )
        Assert.assertEquals("あいうえお。", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(0, 2), sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - with next snippet - hard stop at start of next`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえお", 1, 1),
                0,
                nextSnippets = listOf(TextSnippet(2, "。かきくけこたちつてとさしすせそはひふへほ", 1, 2))
        )
        Assert.assertEquals("あいうえお。", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(1, 2), sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - with next snippet - soft stop at end`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえお\n", 1, 1),
                0,
                nextSnippets = listOf(TextSnippet(2, "かきくけこたちつてとさしすせそはひふへほ", 1, 2))
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - with next snippet - ends in middle of next snippet`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえお", 1, 1),
                0,
                nextSnippets = listOf(TextSnippet(2, "かきくけこ\nたちつてとさしすせそはひふへほ", 1, 2))
        )
        Assert.assertEquals("あいうえおかきくけこ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(6, 2), sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - has previous snippet - no breaks`() {
        val sentence = Sentence(
                TextSnippet(1, "たちつてとさしすせそはひふへほ", 1, 1),
                0,
                previousSnippets = listOf(TextSnippet(2, "あいうえおかきくけこ", 1, 2))
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - has previous snippet - hard stop at end of prev`() {
        val sentence = Sentence(
                TextSnippet(1, "たちつてとさしすせそはひふへほ", 1, 1),
                0,
                previousSnippets = listOf(TextSnippet(2, "あいうえおかきくけこ。", 1, 2))
        )
        Assert.assertEquals("たちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals("あいうえおかきくけこ。", sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - has previous snippet - soft stop at end of prev`() {
        val sentence = Sentence(
                TextSnippet(1, "たちつてとさしすせそはひふへほ", 1, 1),
                0,
                previousSnippets = listOf(TextSnippet(2, "あいうえおかきくけこ\n  \n  ", 1, 2))
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - has previous snippet - hard stop in middle of prev`() {
        val sentence = Sentence(
                TextSnippet(1, "たちつてとさしすせそはひふへほ", 1, 1),
                0,
                previousSnippets = listOf(TextSnippet(2, "あいうえお。かきくけこ", 1, 2))
        )
        Assert.assertEquals("かきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals("あいうえお。", sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - hard stop and next sentence starts with ignore character`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえお。\n「かきくけこ」", 1, 1),
                6
        )
        Assert.assertEquals("「かきくけこ」", sentence.currentSentence)
        Assert.assertEquals("あいうえお。", sentence.previousSentence)
        Assert.assertEquals(null, sentence.getNextSentenceStart())
    }

    @Test
    fun `test current sentence - soft stop and next sentence starts on an ignore character`() {
        val sentence = Sentence(
                TextSnippet(1, "あいうえお\n「かきくけこ」\n。\nたちつてと", 1, 1),
                6
        )
        Assert.assertEquals("「かきくけこ」。", sentence.currentSentence)
        Assert.assertEquals("あいうえお", sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(16, 1), sentence.getNextSentenceStart())
    }

    @Suppress("unused")
    @Ignore("Manual test")
    fun `manually test sentence`() {
        listOf(TempTestData.page5Text, TempTestData.page6Text, TempTestData.page7Text).forEach { content ->
            val snippet = TextSnippet(1, content, 1, 1)
            var currentChar: Int? = 0

            while (currentChar != null && currentChar >= 0 && currentChar < content.length) {
                val sentence = Sentence(snippet, currentChar)
                println(sentence.currentSentence)
                currentChar = sentence.getNextSentenceStart()?.startIndex
            }

            println("NEXT SNIPPET =======\n\n")
        }
    }
}