package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TextSnippetsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TextSnippet)

    @Update
    suspend fun update(vararg items: TextSnippet)

    @Query("SELECT * FROM ${TextSnippet.TABLE_NAME}")
    fun getAllSnippets(): LiveData<List<TextSnippet>>

    @Query("SELECT * FROM ${TextSnippet.TABLE_NAME} WHERE id = :textSnippetId")
    fun getTextSnippetById(textSnippetId: Int): LiveData<TextSnippet>

    @Query(
            """
                SELECT *
                FROM ${TextSnippet.TABLE_NAME} 
                WHERE 
                        textId = :textId AND pageReference = :pageReference 
                        AND chapterId = :chapterId AND ordinal = :ordinal
            """
    )
    fun getTextSnippetEntry(
            textId: Int,
            pageReference: Int,
            chapterId: Int? = 1,
            ordinal: Int? = 1
    ): LiveData<TextSnippet>
}