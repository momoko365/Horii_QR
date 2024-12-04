package com.example.horii_hht.DB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Worker(
    @PrimaryKey
    val workerCD: String,
    val workerName: String
)