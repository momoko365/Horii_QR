package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ItemDAO {
  @Insert
    fun insert(item: Item)
    @Query("SELECT * FROM Item")
    fun getItemAll(): List<Item>
  @Query("SELECT COUNT(DISTINCT itemCD) FROM Item")
 fun getDistinctItemCount(): Int

  @Query("SELECT SUM(suryo) FROM Item")
fun getTotalSuryo(): Int

  @Query("SELECT kenpinNo FROM Item LIMIT 1")
   fun getKenpinNo(): String?

    // suryoがzumiのアイテムの数を取得する
    @Query("SELECT COUNT(*) FROM Item WHERE suryo = 'zumi'")
    fun getCountOfZumiItems(): Int

    // 全行のzumi数をカウントする
    @Query("SELECT SUM(zumi) FROM Item")
    fun getTotalZumiCount(): Int

    @Query("SELECT * FROM ITEM WHERE JAN = :jan OR ITF = :itf")
    fun getItemByCode(jan: String, itf: String): List<Item>
}