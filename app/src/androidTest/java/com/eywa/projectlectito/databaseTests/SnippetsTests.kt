package com.eywa.projectlectito.databaseTests

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.snippets.TextSnippetsDao
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.database.texts.TextsDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnippetsTests {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: LectitoRoomDatabase
    private lateinit var textsDao: TextsDao
    private lateinit var textSnippetsDao: TextSnippetsDao

    @Before
    fun createDb() {
        db = DatabaseTestHelper.createDatabase()
        textsDao = db.textsDao()
        textSnippetsDao = db.textSnippetsDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testGetSurroundingSnippets_noPreviousSnippets() {
        val textSnippets = listOf(
                TextSnippet(1, "", 1, 1, 1, 1),
                TextSnippet(2, "", 1, 2, 1, 1),
                TextSnippet(3, "", 1, 2, 1, 2),
                TextSnippet(4, "", 1, 3, 1, 1),
        )
        val expectedData = listOf(TextSnippet.WithInt(textSnippets[0], 0))
        surroundingSnippetsTest(expectedData, textSnippets, textSnippets[0].pageReference, textSnippets[0].ordinal)
    }

    @Test
    fun testGetSurroundingSnippets_hasPreviousSnippets() {
        val textSnippets = listOf(
                TextSnippet(1, "", 1, 1, 1, 1),
                TextSnippet(2, "", 1, 2, 1, 1),
                TextSnippet(3, "", 1, 2, 1, 2),
                TextSnippet(4, "", 1, 3, 1, 1),
        )
        val expectedData = listOf(TextSnippet.WithInt(textSnippets[0], 2))
        surroundingSnippetsTest(expectedData, textSnippets, textSnippets[2].pageReference, textSnippets[2].ordinal)
    }

    private fun surroundingSnippetsTest(
            expectedData: List<TextSnippet.WithInt>,
            snippets: List<TextSnippet>,
            currentPageRef: Int,
            currentOrdinal: Int,
            snippetsToRetrieve: Int = 1
    ) {
        val texts = listOf(
                Text(1, "text1", 3, 1, false),
                Text(2, "text2")
        )
        val textTwoSnippets = listOf(
                TextSnippet(1000, "", 2, 1, 1, 1),
                TextSnippet(1001, "", 2, 2, 1, 1),
        )

        runBlocking {
            texts.forEach { textsDao.insert(it) }
            snippets.plus(textTwoSnippets).forEach { textSnippetsDao.insert(it) }
        }

        val retrievedInfo = textSnippetsDao.getWithSurroundingSnippets(
                texts[0].id,
                currentPageRef,
                currentOrdinal,
                snippetsToRetrieve
        ).retrieveValue()!!

        retrievedInfo.forEach { actual ->
            Log.i("test", "Testing testId: ${actual.snippet.id}")
            val expected = expectedData.find { it.snippet.id == actual.snippet.id }
            assertNotNull(expected)
            assertEquals(expected?.extraInfo, actual.extraInfo)
        }
    }
}