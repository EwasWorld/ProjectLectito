package com.eywa.projectlectito

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testTeImasu() {
        //val testSentence = "何を言ってるがわがらないと曽いますが事実です"
        val testSentence = "打込む"
        val separator = "・"

//        val tokenizer2 = com.atilika.kuromoji.ipadic.Tokenizer()
//        val tokens2 = tokenizer2.tokenize(testSentence)
//        println(tokens2.joinToString(separator) { it.surface })
//        println(tokens2.joinToString(separator) { it.baseForm })
//        println(tokens2.joinToString(separator) { it.pronunciation })
//        println()

        val tokenizer3 = com.atilika.kuromoji.unidic.kanaaccent.Tokenizer()
        val tokens3 = tokenizer3.tokenize(testSentence)
//        println(tokens3.joinToString(separator) {it.lemma})
        println(tokens3.joinToString(separator) { it.writtenForm })
        println(tokens3.joinToString(separator) { it.writtenBaseForm })
        println(tokens3.joinToString(separator) { it.pronunciation })
        println(tokens3.joinToString(separator) { it.accentType })
        println()
        println(tokens3.joinToString("\n") { it.surface.padStart(3, '　') + separator + it.allFeatures })
    }
}