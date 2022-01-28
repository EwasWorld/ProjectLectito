package com.eywa.projectlectito.databaseTests

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eywa.projectlectito.database.LectitoRoomDatabase
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.snippets.TextSnippetsDao
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.database.texts.TextsDao
import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextsTests {
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
    fun testGetAllTextsWithSnippetInfo() {
        val texts = listOf(
                Text(1, "text1", 3, 1, false),
                Text(2, "text2", null, null, false),
                Text(3, "text3", null, null, false),
                Text(4, "text4", null, null, true),
        )
        val textSnippets = listOf(
                TextSnippet(1, "", 1, 1, 1, 1),
                TextSnippet(2, "", 1, 2, 1, 1),
                TextSnippet(3, "", 1, 2, 1, 2),
                TextSnippet(4, "", 1, 3, 1, 1),
                TextSnippet(5, "", 2, 1, 1, 1),
                TextSnippet(6, "", 2, 2, 1, 1),
                TextSnippet(7, "", 4, 1, 1, 1),
                TextSnippet(8, "", 4, 2, 1, 1),
        )

        runBlocking {
            texts.forEach { textsDao.insert(it) }
            textSnippets.forEach { textSnippetsDao.insert(it) }
        }

        val expectedData = listOf(
                Text.WithCurrentSnippetInfo(texts[0], textSnippets[2], 4, 2),
                Text.WithCurrentSnippetInfo(texts[1], null, 2, 0),
                Text.WithCurrentSnippetInfo(texts[2], null, 0, 0),
                Text.WithCurrentSnippetInfo(texts[3], null, 2, 0),
        )

        val retrievedInfo = textsDao.getAllTextsWithSnippetInfo().retrieveValue()!!
        retrievedInfo.forEach { actual ->
            Log.i("test", "Testing testId: {$actual.text.id}")
            val expected = expectedData.find { it.text.id == actual.text.id }
            assertNotNull(expected)
            assertEquals(expected?.currentSnippet?.id, actual.currentSnippet?.id)
            assertEquals(expected?.totalSnippets, actual.totalSnippets)
            assertEquals(expected?.readSnippets, actual.readSnippets)
        }
    }
}