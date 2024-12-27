package com.example.horii_hht.DB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode")
data class Barcode(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String
)