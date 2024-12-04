package com.example.horii_hht.DB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    //最初だけ読む４桁
    var kenpinNo: String,
    var kenpinpage: String,
    //4桁
    val itemCD: String,
    //10桁
    val itemName: String,
    //４桁
    val case_q: Int,
    //４桁
    val bara: Int,
    //13桁
    val JAN: String,
    //14桁
    val ITF: String,
    var casezumi: Int,
    var barazumi: Int,
    var kenpinTime: String, //数量更新されたら時刻アップデート
    val QRTime: String //QRコード読み込んだ時間をアップデートしていく
)

data class SummarizedData(
    val totalCaseQ: Int,
    val totalBara: Int,
    val totalCasezumi: Int,
    val totalBarazumi: Int
)

// SummarizedItemデータクラスを定義
data class CSVData(
    val kenpinNo: String,
    val itemCD: String,
    val itemName: String,
    val totalCaseQ: Int,
    val totalBara: Int,
    val totalCasezumi: Int,
    val totalBarazumi: Int,
    val JAN: String,
    val ITF: String,
    val kenpinTime: String?,
    val QRTime: String?
)

data class MaxTimes(
    val maxKenpinTime: String?,
    val maxQRTime: String?
)
