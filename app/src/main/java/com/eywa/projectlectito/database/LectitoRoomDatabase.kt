package com.eywa.projectlectito.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.eywa.projectlectito.database.snippets.ParsedInfo
import com.eywa.projectlectito.database.snippets.ParsedInfoDao
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.snippets.TextSnippetsDao
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.database.texts.TextsDao
import java.util.*

@Database(
        entities = [
            Text::class, TextSnippet::class, ParsedInfo::class
        ],
        version = 1,
        exportSchema = true
)
@TypeConverters(LectitoRoomDatabase.Converters::class)
abstract class LectitoRoomDatabase : RoomDatabase() {
    abstract fun textsDao(): TextsDao
    abstract fun textSnippetsDao(): TextSnippetsDao
    abstract fun parsedInfoDao(): ParsedInfoDao

    companion object {
        const val DATABASE_NAME = "scores_database"
    }

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return value?.let { Date(it) }
        }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }

        @TypeConverter
        fun toStringList(value: String): List<String> {
            if (value.isEmpty()) return listOf()
            return value.split(':')
        }

        @TypeConverter
        fun toFlatString(value: List<String>): String {
            return value.joinToString(":")
        }
    }
}