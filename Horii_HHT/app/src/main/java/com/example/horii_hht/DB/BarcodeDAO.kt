package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BarcodeDAO {
    @Insert
    fun insert(barcode: Barcode)

    @Query("SELECT * FROM barcode LIMIT 1")
    fun getBarcodeAll(): Barcode?

    @Query("DELETE FROM barcode")
    fun deleteAllBarcode()


}
