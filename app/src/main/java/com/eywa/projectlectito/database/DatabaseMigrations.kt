package com.eywa.projectlectito.database

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseMigrations {
    companion object {
        private const val MIGRATION_LOG_TAG = "DatabaseMigration"

        /**
         * Migration template from which others can be made
         */
        val MIGRATION_BLANK = object : Migration(1, 1) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val sqlStrings = mutableListOf<String>()
                TODO()
                executeMigrations(sqlStrings, database, startVersion, endVersion)
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val sqlStrings = mutableListOf<String>()
                sqlStrings.add("DROP TABLE `parsed_info`")
                executeMigrations(sqlStrings, database, startVersion, endVersion)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val sqlStrings = mutableListOf<String>()
                sqlStrings.add("ALTER TABLE `text_snippets` ADD COLUMN `name` TEXT")
                sqlStrings.add(
                        """
                            CREATE TABLE `text_chapters`(
                                `chapterNumber` INTEGER NOT NULL, 
                                `textId` INTEGER NOT NULL, 
                                `name` TEXT NOT NULL, 
                                CONSTRAINT PK_text_chapters PRIMARY KEY (textId, chapterNumber)
                            )
                        """
                )
                executeMigrations(sqlStrings, database, startVersion, endVersion)
            }
        }

        private fun executeMigrations(
                sqlStrings: List<String>, database: SupportSQLiteDatabase,
                startVersion: Int, endVersion: Int
        ) {
            Log.i(MIGRATION_LOG_TAG, "migrating from $startVersion to $endVersion")
            for (sqlStatement in sqlStrings) {
                database.execSQL(sqlStatement.trimIndent().replace("\\n", ""))
            }
        }
    }
}