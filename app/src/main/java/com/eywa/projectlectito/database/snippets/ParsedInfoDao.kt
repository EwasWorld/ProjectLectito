package com.eywa.projectlectito.database.snippets

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ParsedInfoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ParsedInfo)

    @Update
    suspend fun update(vararg items: ParsedInfo)

    @Query(
            """
                SELECT * 
                FROM ${ParsedInfo.TABLE_NAME} 
                WHERE startTextSnippetId = :startTextSnippetId AND startCharacterIndex = :startCharacterIndex
            """
    )
    fun getParsedInfoById(startTextSnippetId: Int, startCharacterIndex: Int): LiveData<ParsedInfo>
}