package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao

interface SyukkaDAO {
    @Insert
    fun insert(syukkaItems: List<SyukkaItem>) // List<Item> 型の引数を受け取る

    @Query("SELECT MAX(kenpinTime) AS maxKenpinTime, MAX(QRTime) AS maxQRTime FROM SyukkaItem")
    fun getMaxTimes(): MaxTimes?

    @Query("SELECT * FROM SyukkaItem")
    fun getSyukkaItemAll(): List<SyukkaItem>

    //アイテムを全削除
    @Query("DELETE FROM SyukkaItem")
    fun deleteAllItems()

}