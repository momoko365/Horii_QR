package com.example.horii_hht.DB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Worker::class, Item::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDAO(): WorkerDAO
abstract fun itemDAO(): ItemDAO
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "app_database"
//                )
//                    .addCallback(AppDatabaseCallback())
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//
//        private class AppDatabaseCallback : RoomDatabase.Callback() {
//            override fun onCreate(db: SupportSQLiteDatabase) {
//                super.onCreate(db)
//                // データベースが作成された時に初期データを挿入
//                INSTANCE?.let { database ->
//                    CoroutineScope(Dispatchers.IO).launch {
//                        populateDatabase(database.workerDAO())
//                    }
//                }
//            }
//        }
//
//        suspend fun populateDatabase(workerDAO: WorkerDAO) {
//            // 初期データを挿入
//            val worker = Worker("1", "John Doe")
//            workerDAO.insert(worker)
//        }
//    }
}
