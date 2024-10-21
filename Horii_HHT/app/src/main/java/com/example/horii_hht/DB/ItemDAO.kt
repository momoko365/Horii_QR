package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ItemDAO {
  @Insert
    fun insert(item: Item)

}