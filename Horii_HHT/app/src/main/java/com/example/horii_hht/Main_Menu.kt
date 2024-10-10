package com.example.horii_hht

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Main_Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)
        val nyukaBtn = findViewById<android.widget.Button>(R.id.nyukabtn)
        val syukkaBtn = findViewById<android.widget.Button>(R.id.syukkabtn)
        val tanaoroshiBtn = findViewById<android.widget.Button>(R.id.tanaoroshibtn)
        val checkBtn = findViewById<android.widget.Button>(R.id.checkbtn)

        nyukaBtn.setOnClickListener {
            val intent = Intent(this, Nyuka01::class.java)
            startActivity(intent)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {

            KeyEvent.KEYCODE_F4 -> {
                // F4キーが押されたときの処理
                val intent = Intent(this, Start_Worker::class.java)
                startActivity(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}