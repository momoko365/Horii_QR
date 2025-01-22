package com.example.horii_hht.DB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SyukkaItem")

data class SyukkaItem(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val kenpinNo: String,
    val kenpinpage: String,
    val itemCD: String,
    val itemName: String,
    val JAN: String,
    val ITF: String,
    val case_q: Int,
    val bara: Int,
    var casezumi: Int,
    var barazumi: Int,
    var kenpinTime: String?,
    val QRTime: String?
)

@Entity
// SummarizedItemデータクラスを定義
data class Syukka_CSVData(
    @PrimaryKey val kenpinNo: String,
    val itemCD: String,
    val itemName: String,
    val totalCaseQ: Int,
    val totalBara: Int,
    var totalCasezumi: Int,
    var totalBarazumi: Int,
    val JAN: String,
    val ITF: String,
    var kenpinTime: String?,
    val QRTime: String?
)

data class Syukka_MaxTimes(
    val maxKenpinTime: String?,
    val maxQRTime: String?
)