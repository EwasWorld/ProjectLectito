package com.eywa.projectlectito

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException

class TestLogic {
    companion object {
        private const val LOG_TAG = "ECH TEST"

        private val testSentence = "突然ですが、僕の現状をお伝えしたいと思います"
        private val testText = """
            拝啓、父さん、母さん。
            お元気でしようか。
            あなたたちの息子、東朱里は元気です。
            突然ですが、僕の現状をお伝えしたいと思います。
            どこかのテントのベッドに寝かされ、八人の男女に囲まれています。
            何を言ってるがわがらないと曽いますが事実です。
            しかも、八人全てが美男美女という、僕のようなフノメンには居い地の悪い空間です
            つまり、現代の日本ではヽ普通は体験てきなレ現場に出くわしてしるわけてす。
            ははは、貴重な体験ですね。
            「ーおい、何を笑っている」
            そう言って僕に尋ねるのはリーダ]]格の方で、僕のベノトの傍て気味亜]そうに聞縄ました。失礼な人だな、と思いましたが口には出しません。
            だって、この人たち、剣とか槍とか弓とか、ともかく武装しているんですから。麟蝿
        """.trimIndent()

        private val parsedLine by lazy {
            Log.i(LOG_TAG, "parsing")
//            val tokens = Tokenizer.createDefaultTokenizer().tokenize(testSentence)
//            Log.i(LOG_TAG, "finished parsing")
//            tokens.map { it.text }
            listOf("突然")
        }
        private var currentItem = -1

        fun getNextLine(): String {
            return testSentence
        }

        fun getFirstWord(): String {
            currentItem = 0
            return parsedLine[0]
        }

        fun getNextWord(): String {
            if (currentItem + 1 >= parsedLine.size) {
                currentItem = - 1
            }
            return parsedLine[++currentItem]
        }

        fun getDefinition(callback: (JishoReturnData) -> Unit) {
            val rawJson = "{\"meta\":{\"status\":200},\"data\":[{\"slug\":\"突然\",\"is_common\":true,\"tags\":[\"wanikani26\"],\"jlpt\":[\"jlpt-n3\"],\"japanese\":[{\"word\":\"突然\",\"reading\":\"とつぜん\"}],\"senses\":[{\"english_definitions\":[\"abrupt\",\"sudden\",\"unexpected\"],\"parts_of_speech\":[\"Noun which may take the genitive case particle 'no'\",\"Na-adjective (keiyodoshi)\",\"Adverb (fukushi)\"],\"links\":[],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[]}],\"attribution\":{\"jmdict\":true,\"jmnedict\":false,\"dbpedia\":false}},{\"slug\":\"突然死\",\"is_common\":false,\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然死\",\"reading\":\"とつぜんし\"}],\"senses\":[{\"english_definitions\":[\"sudden death\"],\"parts_of_speech\":[\"Noun\"],\"links\":[],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[]},{\"english_definitions\":[\"Sudden cardiac death\"],\"parts_of_speech\":[\"Wikipedia definition\"],\"links\":[{\"text\":\"Read “Sudden cardiac death” on English Wikipedia\",\"url\":\"http://en.wikipedia.org/wiki/Sudden_cardiac_death?oldid=491618300\"},{\"text\":\"Read “突然死” on Japanese Wikipedia\",\"url\":\"http://ja.wikipedia.org/wiki/突然死?oldid=42426005\"}],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[],\"sentences\":[]}],\"attribution\":{\"jmdict\":true,\"jmnedict\":false,\"dbpedia\":\"http://dbpedia.org/resource/Sudden_cardiac_death\"}},{\"slug\":\"突然変異\",\"is_common\":false,\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然変異\",\"reading\":\"とつぜんへんい\"}],\"senses\":[{\"english_definitions\":[\"mutation\"],\"parts_of_speech\":[\"Noun\",\"Suru verb\"],\"links\":[],\"tags\":[\"Biology\"],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[]},{\"english_definitions\":[\"Mutation\"],\"parts_of_speech\":[\"Wikipedia definition\"],\"links\":[{\"text\":\"Read “Mutation” on English Wikipedia\",\"url\":\"http://en.wikipedia.org/wiki/Mutation?oldid=494428451\"},{\"text\":\"Read “突然変異” on Japanese Wikipedia\",\"url\":\"http://ja.wikipedia.org/wiki/突然変異?oldid=40451149\"}],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[],\"sentences\":[]}],\"attribution\":{\"jmdict\":true,\"jmnedict\":false,\"dbpedia\":\"http://dbpedia.org/resource/Mutation\"}},{\"slug\":\"突然変異説\",\"is_common\":false,\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然変異説\",\"reading\":\"とつぜんへんいせつ\"}],\"senses\":[{\"english_definitions\":[\"mutationism\"],\"parts_of_speech\":[\"Noun\"],\"links\":[],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[]},{\"english_definitions\":[\"Mutationism\"],\"parts_of_speech\":[\"Wikipedia definition\"],\"links\":[{\"text\":\"Read “Mutationism” on English Wikipedia\",\"url\":\"http://en.wikipedia.org/wiki/Mutationism?oldid=467496981\"},{\"text\":\"Read “突然変異説” on Japanese Wikipedia\",\"url\":\"http://ja.wikipedia.org/wiki/突然変異説?oldid=34951463\"}],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[],\"sentences\":[]}],\"attribution\":{\"jmdict\":true,\"jmnedict\":false,\"dbpedia\":\"http://dbpedia.org/resource/Mutationism\"}},{\"slug\":\"突然変異体\",\"is_common\":false,\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然変異体\",\"reading\":\"とつぜんへんいたい\"}],\"senses\":[{\"english_definitions\":[\"mutant\"],\"parts_of_speech\":[\"Noun\"],\"links\":[],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[]}],\"attribution\":{\"jmdict\":true,\"jmnedict\":false,\"dbpedia\":false}},{\"slug\":\"突然だけど\",\"is_common\":false,\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然だけど\",\"reading\":\"とつぜんだけど\"}],\"senses\":[{\"english_definitions\":[\"apropos of nothing\",\"to change the subject\"],\"parts_of_speech\":[\"Expressions (phrases, clauses, etc.)\"],\"links\":[],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[]}],\"attribution\":{\"jmdict\":true,\"jmnedict\":false,\"dbpedia\":false}},{\"slug\":\"突然目\",\"is_common\":false,\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然目\",\"reading\":\"とつぜんめ\"}],\"senses\":[{\"english_definitions\":[\"somewhat sudden\"],\"parts_of_speech\":[\"Expressions (phrases, clauses, etc.)\"],\"links\":[],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[]}],\"attribution\":{\"jmdict\":true,\"jmnedict\":false,\"dbpedia\":false}},{\"slug\":\"518695bfd5dda7b2c603da5d\",\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然! マッチョマン\"}],\"senses\":[{\"english_definitions\":[\"Amagon\"],\"parts_of_speech\":[\"Wikipedia definition\"],\"links\":[{\"text\":\"Read “Amagon” on English Wikipedia\",\"url\":\"http://en.wikipedia.org/wiki/Amagon?oldid=473707056\"},{\"text\":\"Read “突然! マッチョマン” on Japanese Wikipedia\",\"url\":\"http://ja.wikipedia.org/wiki/突然!_マッチョマン?oldid=40995916\"}],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[],\"sentences\":[]}],\"attribution\":{\"jmdict\":false,\"jmnedict\":false,\"dbpedia\":\"http://dbpedia.org/resource/Amagon\"}},{\"slug\":\"51869cfcd5dda7b2c60745d7\",\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然の恐怖\"}],\"senses\":[{\"english_definitions\":[\"Sudden Fear\"],\"parts_of_speech\":[\"Wikipedia definition\"],\"links\":[{\"text\":\"Read “Sudden Fear” on English Wikipedia\",\"url\":\"http://en.wikipedia.org/wiki/Sudden_Fear?oldid=495485216\"},{\"text\":\"Read “突然の恐怖” on Japanese Wikipedia\",\"url\":\"http://ja.wikipedia.org/wiki/突然の恐怖?oldid=37810219\"}],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[],\"sentences\":[]}],\"attribution\":{\"jmdict\":false,\"jmnedict\":false,\"dbpedia\":\"http://dbpedia.org/resource/Sudden_Fear\"}},{\"slug\":\"51869d20d5dda7b2c60756f4\",\"tags\":[],\"jlpt\":[],\"japanese\":[{\"word\":\"突然!サバイバル\"}],\"senses\":[{\"english_definitions\":[\"Flight 29 Down\"],\"parts_of_speech\":[\"Wikipedia definition\"],\"links\":[{\"text\":\"Read “Flight 29 Down” on English Wikipedia\",\"url\":\"http://en.wikipedia.org/wiki/Flight_29_Down?oldid=495009962\"},{\"text\":\"Read “突然!サバイバル” on Japanese Wikipedia\",\"url\":\"http://ja.wikipedia.org/wiki/突然!サバイバル?oldid=37635792\"}],\"tags\":[],\"restrictions\":[],\"see_also\":[],\"antonyms\":[],\"source\":[],\"info\":[],\"sentences\":[]}],\"attribution\":{\"jmdict\":false,\"jmnedict\":false,\"dbpedia\":\"http://dbpedia.org/resource/Flight_29_Down\"}}]}"
//            val request =
//                    Request.Builder().url("https://jisho.org/api/v1/search/words?keyword=${parsedLine[currentItem]}")
//                            .build()
//            val httpClient = OkHttpClient()
//            httpClient.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    Log.e(LOG_TAG, "Error retrieving dataset")
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    val body = response.body?.string()
//                    if (body == null) {
//                        Log.e(LOG_TAG, "Empty body when retrieving dataset")
//                        return
//                    }

                    val finalData: JishoReturnData?
                    try {
                        finalData = GsonBuilder().create().fromJson(rawJson, JishoReturnData::class.java)
                        if (finalData == null) {
                            Log.e(LOG_TAG, "Error parsing dataset")
                            return
                        }
                        callback(finalData)
                    }
                    catch (e: JsonSyntaxException) {
                        Log.e(LOG_TAG, "Invalid JSON format")
                        Log.e(LOG_TAG, e.message ?: "No message on exception")
                        return
                    }
//                }
//            })
        }
    }

}