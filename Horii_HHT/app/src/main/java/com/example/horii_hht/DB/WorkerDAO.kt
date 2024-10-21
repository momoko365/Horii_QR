package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkerDAO {
    @Query("SELECT * FROM Worker WHERE workerCD = :workerCD")
    fun getWorkerCD(workerCD: String): Worker?
    @Query("SELECT Count(*) FROM Worker")
    fun getAll(): Int
    @Insert
    fun insert(worker: Worker): Long
}