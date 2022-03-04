package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData
import androidx.room.*
import com.eywa.projectlectito.database.texts.Text

@Dao
interface TextSnippetsDao {
    companion object {
        private const val ORDERING = "ORDER BY pageReference, ordinal"
        private const val INVERSE_ORDERING = "ORDER BY pageReference DESC, ordinal DESC"
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TextSnippet): Long

    @Update
    suspend fun update(vararg items: TextSnippet)

    @Query("SELECT * FROM ${TextSnippet.TABLE_NAME} $ORDERING")
    fun getAllSnippets(): LiveData<List<TextSnippet>>

    @Query("SELECT * FROM ${TextSnippet.TABLE_NAME} WHERE id = :textSnippetId")
    fun getTextSnippetById(textSnippetId: Int): LiveData<TextSnippet?>

    @Query(
            """
                SELECT 
                    snip.*,
                    text.id as txt_id,
                    text.currentSnippetId as txt_currentSnippetId,
                    text.currentCharacterIndex as txt_currentCharacterIndex,
                    text.isComplete as txt_isComplete,
                    text.name as txt_name
                FROM ${TextSnippet.TABLE_NAME} as snip 
                LEFT JOIN ${Text.TABLE_NAME} text ON snip.textId = text.id 
                WHERE snip.id = :textSnippetId
            """
    )
    fun getSnippetByIdWithText(textSnippetId: Int): LiveData<TextSnippet.WithText?>

    /**
     * @return previous snippets then the current snippet then next snippets in standard snippet order
     */
    @Query(
            """
                SELECT *, 
                (
                    SELECT COUNT(*)
                    FROM ${TextSnippet.TABLE_NAME}
                    WHERE
                        textId = :textId
                        AND (
                            pageReference < :currentSnippetPageRef
                            OR (pageReference = :currentSnippetPageRef AND ordinal < :currentSnippetOrdinal)
                        )
                ) as extraInfo
                FROM ${TextSnippet.TABLE_NAME}
                WHERE 
                    textId = :textId 
                    AND (
                        (pageReference = :currentSnippetPageRef AND ordinal == :currentSnippetOrdinal)
                        OR (
                            pageReference > :currentSnippetPageRef
                            OR (pageReference = :currentSnippetPageRef AND ordinal > :currentSnippetOrdinal)
                        )
                        OR (
                            pageReference < :currentSnippetPageRef
                            OR (pageReference = :currentSnippetPageRef AND ordinal < :currentSnippetOrdinal) 
                        )
                    )
                $ORDERING
                LIMIT :snippetsToRetrieve
            """
    )
    fun getWithSurroundingSnippets(
            textId: Int,
            currentSnippetPageRef: Int,
            currentSnippetOrdinal: Int,
            snippetsToRetrieve: Int
    ): LiveData<List<TextSnippet.WithInt>>

    @Query(
            """
                SELECT 
                    snip.*,
                    text.id as txt_id,
                    text.currentSnippetId as txt_currentSnippetId,
                    text.currentCharacterIndex as txt_currentCharacterIndex,
                    text.isComplete as txt_isComplete,
                    text.name as txt_name
                FROM ${TextSnippet.TABLE_NAME} as snip 
                LEFT JOIN ${Text.TABLE_NAME} text ON snip.textId = text.id 
                WHERE snip.textId = :textId
                $ORDERING
                LIMIT 1
            """
    )
    fun getFirstSnippetWithText(textId: Int): LiveData<TextSnippet.WithText?>

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
                $ORDERING
            """
    )
    fun getSnippetsForText(textId: Int): LiveData<List<TextSnippet>>

    @Query("DELETE FROM ${TextSnippet.TABLE_NAME} WHERE textId = :textId")
    suspend fun deleteSnippetsForText(textId: Int)

    @Query("DELETE FROM ${TextSnippet.TABLE_NAME} WHERE id = :snippetId")
    suspend fun deleteSnippet(snippetId: Int)
}