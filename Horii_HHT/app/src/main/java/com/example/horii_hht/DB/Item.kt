package com.example.horii_hht.DB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Item(
    @PrimaryKey
    //最初だけ読む４桁
    var kenpinNo: String,
    //4桁
    val itemCD: String,
    //10桁
    val itemName: String,
    //４桁
    val suryo:Int,
    //４桁
    val in_q:Int,
    //４桁
    val case_q:Int,
    //13桁
    val JAN:String,
    //14桁
    val ITF:String,
) {
}