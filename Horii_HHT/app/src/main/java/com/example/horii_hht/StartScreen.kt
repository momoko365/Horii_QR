package com.example.horii_hht

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileWriter
import java.io.IOException

// 最初に表示されるスプラッシュ画面
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 数秒後に MainActivity に遷移
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, Start_Day::class.java)
            startActivity(intent)
            finish()
        }, 1500) // 1500ミリ秒（1.5秒）
    }
}