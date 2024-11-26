package com.example.horii_hht.DB

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time

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
    val case_q:Int,
    //４桁
    val bara :Int,
    //13桁
    val JAN:String,
    //14桁
    val ITF:String,
    val casezumi : Int,
    val barazumi : Int,
    val kenpinTime : Long?
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
        val ITF: String
    )
