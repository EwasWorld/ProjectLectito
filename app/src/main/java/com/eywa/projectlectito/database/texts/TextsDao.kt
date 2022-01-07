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
    fun getTextById(id: Int): LiveData<Text>
}
