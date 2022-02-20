package com.eywa.projectlectito.database.texts

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TextsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: Text)

    @Update
    suspend fun update(vararg items: Text)

    @Query("SELECT * FROM ${Text.TABLE_NAME} WHERE id = :id")
    fun getTextById(id: Int): LiveData<Text?>

    @Query("SELECT * FROM ${Text.TABLE_NAME}")
    fun getAllTexts(): LiveData<List<Text>>

    @Query(
            """
                SELECT
                    texts.id AS txt_id,
                    texts.name AS txt_name,
                    texts.currentSnippetId AS txt_currentSnippetId,
                    texts.currentCharacterIndex AS txt_currentCharacterIndex,
                    texts.isComplete AS txt_isComplete,
                    curr.*,
                    (
                        SELECT COUNT(*) FROM text_snippets WHERE texts.id = text_snippets.textId
                    ) AS totalSnippets,
                    (
                        SELECT COUNT(*) FROM text_snippets WHERE texts.id = text_snippets.textId 
                            AND (
                                text_snippets.pageReference < curr.pageReference 
                                OR (text_snippets.pageReference = curr.pageReference AND text_snippets.ordinal < curr.ordinal)
                            )
                    ) AS readSnippets
                FROM ${Text.TABLE_NAME} 
                    LEFT JOIN text_snippets curr ON texts.currentSnippetId = curr.id
            """
    )
    fun getAllTextsWithSnippetInfo(): LiveData<List<Text.WithCurrentSnippetInfo>>

    @Query("DELETE FROM ${Text.TABLE_NAME} WHERE id = :id")
    suspend fun deleteText(id: Int)
}
