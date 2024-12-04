package com.example.horii_hht.DB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Worker::class, Item::class], version = 31, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDAO(): WorkerDAO
    abstract fun itemDAO(): ItemDAO
}
