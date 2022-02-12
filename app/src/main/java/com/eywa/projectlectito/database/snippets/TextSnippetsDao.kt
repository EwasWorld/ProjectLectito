package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TextSnippetsDao {
    companion object {
        private const val ORDERING = "ORDER BY pageReference, ordinal"
        private const val INVERSE_ORDERING = "ORDER BY pageReference DESC, ordinal DESC"
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TextSnippet)

    @Update
    suspend fun update(vararg items: TextSnippet)

    @Query("SELECT * FROM ${TextSnippet.TABLE_NAME}")
    fun getAllSnippets(): LiveData<List<TextSnippet>>

    @Query("SELECT * FROM ${TextSnippet.TABLE_NAME} WHERE id = :textSnippetId")
    fun getTextSnippetById(textSnippetId: Int): LiveData<TextSnippet>

    /**
     * First item is the item just after the current snippet, going down the list gets further away from the current
     * snippet
     */
    @Query(
            """
                SELECT *
                FROM ${TextSnippet.TABLE_NAME} 
                WHERE 
                    textId = :textId 
                    AND (
                        pageReference > :currentSnippetPageRef
                        OR (pageReference = :currentSnippetPageRef AND ordinal > :currentSnippetOrdinal) 
                    )
                $ORDERING
                LIMIT :snippetsToRetrieve
            """
    )
    fun getNextSnippets(
            textId: Int,
            currentSnippetPageRef: Int,
            currentSnippetOrdinal: Int,
            snippetsToRetrieve: Int
    ): LiveData<List<TextSnippet>>

    /**
     * First item is the item just before the current snippet, going down the list gets further away from the current
     * snippet
     */
    @Query(
            """
                SELECT *
                FROM ${TextSnippet.TABLE_NAME} 
                WHERE 
                    textId = :textId 
                    AND (
                        pageReference < :currentSnippetPageRef
                        OR (pageReference = :currentSnippetPageRef AND ordinal < :currentSnippetOrdinal) 
                    )
                $INVERSE_ORDERING
                LIMIT :snippetsToRetrieve
            """
    )
    fun getPreviousSnippets(
            textId: Int,
            currentSnippetPageRef: Int,
            currentSnippetOrdinal: Int,
            snippetsToRetrieve: Int
    ): LiveData<List<TextSnippet>>

    @Query(
            """
                SELECT id
                FROM ${TextSnippet.TABLE_NAME} 
                WHERE textId = :textId
                $ORDERING
                LIMIT 1
            """
    )
    fun getFirstSnippetId(textId: Int): LiveData<Int>

    @Query(
            """
                SELECT *
                FROM ${TextSnippet.TABLE_NAME} 
                WHERE textId = :textId AND pageReference = :pageReference 
            """
    )
    fun getTextSnippetEntry(
            textId: Int,
            pageReference: Int
    ): LiveData<List<TextSnippet>>

    @Query(
            """
                SELECT *
                FROM ${TextSnippet.TABLE_NAME} 
                WHERE textId = :textId
            """
    )
    fun getSnippetsForText(textId: Int): LiveData<List<TextSnippet>>
}