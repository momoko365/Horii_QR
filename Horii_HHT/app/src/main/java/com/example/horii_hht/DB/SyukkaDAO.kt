package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Insert

@Dao

interface SyukkaDAO {
    @Insert
    fun insert(sykkaItems: List<SyukkaItem>) // List<Item> 型の引数を受け取る

}