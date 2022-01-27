package com.eywa.projectlectito

import com.eywa.projectlectito.readSentence.Sentence
import org.junit.Assert
import org.junit.Test

class SentenceUnitTests {
    @Test
    fun `test current sentence - simple`() {
        val sentence = Sentence(
                "あいうえおかきくけこたちつてとさしすせそはひふへほ",
                0,
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - simple mid-sentence start`() {
        val sentence = Sentence(
                "あいうえおかきくけこたちつてとさしすせそはひふへほ",
                5,
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - current on break char`() {
        val sentence = Sentence(
                "あいうえおかきくけこ\n\n。たちつてとさしすせそはひふへほ",
                10,
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("たちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals("あいうえおかきくけこ。", sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - current on break char and no text after`() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Sentence(
                    "あいうえおかきくけこ\n\n。",
                    11,
                    parserSuccessCallback = {},
                    parserFailCallback = {}
            )
        }
    }

    @Test
    fun `test current sentence - current on break char and no text after - has next snippet`() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Sentence(
                    "あいうえおかきくけこ\n\n。",
                    11,
                    nextSnippet = "たちつてとさしすせそはひふへほ",
                    parserSuccessCallback = {},
                    parserFailCallback = {}
            )
        }
    }

    @Test
    fun `test current sentence - up to first break`() {
        val sentence = Sentence(
                "あいうえお\n。かきくけこたちつてとさしすせそはひふへほ",
                0,
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえお。", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(7, Sentence.SnippetLocation.CURRENT), sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - with next snippet - hard stop at end`() {
        val sentence = Sentence(
                "あいうえお。",
                0,
                nextSnippet = "かきくけこたちつてとさしすせそはひふへほ",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえお。", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(0, Sentence.SnippetLocation.NEXT), sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - with next snippet - hard stop at start of next`() {
        val sentence = Sentence(
                "あいうえお",
                0,
                nextSnippet = "。かきくけこたちつてとさしすせそはひふへほ",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえお。", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(1, Sentence.SnippetLocation.NEXT), sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - with next snippet - soft stop at end`() {
        val sentence = Sentence(
                "あいうえお\n",
                0,
                nextSnippet = "かきくけこたちつてとさしすせそはひふへほ",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - with next snippet - ends in middle of next snippet`() {
        val sentence = Sentence(
                "あいうえお",
                0,
                nextSnippet = "かきくけこ\nたちつてとさしすせそはひふへほ",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえおかきくけこ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(Sentence.IndexInfo(6, Sentence.SnippetLocation.NEXT), sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - has previous snippet - no breaks`() {
        val sentence = Sentence(
                "たちつてとさしすせそはひふへほ",
                0,
                previousSnippet = "あいうえおかきくけこ",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - has previous snippet - hard stop at end of prev`() {
        val sentence = Sentence(
                "たちつてとさしすせそはひふへほ",
                0,
                previousSnippet = "あいうえおかきくけこ。",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("たちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals("あいうえおかきくけこ。", sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - has previous snippet - soft stop at end of prev`() {
        val sentence = Sentence(
                "たちつてとさしすせそはひふへほ",
                0,
                previousSnippet = "あいうえおかきくけこ\n  \n  ",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("あいうえおかきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals(null, sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }

    @Test
    fun `test current sentence - has previous snippet - hard stop in middle of prev`() {
        val sentence = Sentence(
                "たちつてとさしすせそはひふへほ",
                0,
                previousSnippet = "あいうえお。かきくけこ",
                parserSuccessCallback = {},
                parserFailCallback = {}
        )
        Assert.assertEquals("かきくけこたちつてとさしすせそはひふへほ", sentence.currentSentence)
        Assert.assertEquals("あいうえお。", sentence.previousSentence)
        Assert.assertEquals(null, sentence.nextSentenceStart)
    }
}