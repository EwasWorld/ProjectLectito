package com.eywa.projectlectito.database

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.database.snippets.TextSnippetsDao
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.database.texts.TextsDao
import com.eywa.projectlectito.features.readSentence.TempTestData
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Singleton


@Module
class DatabaseDaggerModule(application: Application) {
    // Keep all initialisation code together in constructor
    @Suppress("JoinDeclarationAndAssignment")
    internal val lectitoRoomDatabase: LectitoRoomDatabase

    init {
//        application.deleteDatabase(LectitoRoomDatabase.DATABASE_NAME)
        lectitoRoomDatabase =
                Room.databaseBuilder(application, LectitoRoomDatabase::class.java, LectitoRoomDatabase.DATABASE_NAME)
                        .addCallback(PopulateDatabaseCallback(MainScope()))
                        .addMigrations(
                                DatabaseMigrations.MIGRATION_1_2,
                                DatabaseMigrations.MIGRATION_2_3
                        )
                        .build()
        /*
         * Write ahead mode suspected of causes issues with the instrumented test,
         * crashing suite runs with the error:
         *      SQLiteDiskIOException: disk I/O error (code 522 SQLITE_IOERR_SHORT_READ):
         *      , while compiling: PRAGMA journal_mode
         * A few sources point to turning of WAL, notably: https://github.com/Tencent/wcdb/issues/243
         * This also appears to have fixed some test failures
         */
        lectitoRoomDatabase.openHelper.setWriteAheadLoggingEnabled(false)
    }

    private suspend fun populateDatabase(textsDao: TextsDao, textSnippetsDao: TextSnippetsDao) {
        // TODO Remove initial data
        Log.i("Database", "Initial population of database")
        textsDao.insert(Text(1, "傭兵団の料理番"))
        textsDao.insert(Text(2, "Test Text"))
        textSnippetsDao.insert(TextSnippet(1, TempTestData.page5Text, 1, 5, 1))
        textSnippetsDao.insert(TextSnippet(2, TempTestData.page6Text, 1, 6, 1))
        textSnippetsDao.insert(TextSnippet(3, TempTestData.page7Text, 1, 7, 1))
    }

    private inner class PopulateDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
                populateDatabase(lectitoRoomDatabase.textsDao(), lectitoRoomDatabase.textSnippetsDao())
            }
        }
    }

    @Singleton
    @Provides
    fun providesRoomDatabase(): LectitoRoomDatabase {
        return lectitoRoomDatabase
    }
}