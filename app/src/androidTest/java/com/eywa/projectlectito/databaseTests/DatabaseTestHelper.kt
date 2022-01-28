package com.eywa.projectlectito.databaseTests

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eywa.projectlectito.database.LectitoRoomDatabase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DatabaseTestHelper {
    companion object {
        /**
         * Breaks in some kind of 'get thread for transaction' type message. Probably something to do with tests running
         * queries in the main thread?
         */
        const val brokenTransactionMessage = "Transactions can't be tested right now for some reason"
        const val testDatabaseName = "test_lectito_database"

        fun createDatabase(): LectitoRoomDatabase {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase(testDatabaseName)
            return Room.inMemoryDatabaseBuilder(context, LectitoRoomDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
        }
    }
}

fun <T> LiveData<T>.retrieveValue(): T? {
    var value: T? = null
    val latch = CountDownLatch(1)
    val observer = Observer<T> { t ->
        value = t
        latch.countDown()
    }
    observeForever(observer)
    latch.await(2, TimeUnit.SECONDS)
    return value
}