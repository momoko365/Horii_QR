package com.example.horii_hht

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Main_Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)
        val nyukaBtn = findViewById<android.widget.Button>(R.id.nyukabtn)
        val syukkaBtn = findViewById<android.widget.Button>(R.id.syukkabtn)
        val tanaoroshiBtn = findViewById<android.widget.Button>(R.id.tanaoroshibtn)
        val checkBtn = findViewById<android.widget.Button>(R.id.checkbtn)
        val view = findViewById<TextView>(R.id.textView4)
        view.requestFocus()


        nyukaBtn.setOnClickListener {
            val intent = Intent(this, Nyuka01_QRread::class.java)
            startActivity(intent)
            finish()
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {

            KeyEvent.KEYCODE_F4 -> {
                // F4キーが押されたときの処理
                val intent = Intent(this, Start_Worker::class.java)
                startActivity(intent)
                finish()
                true
            }
            KeyEvent.KEYCODE_F1 -> {
                // F1キーが押されたときの処理
                val intent = Intent(this, Nyuka01_QRread::class.java)
                startActivity(intent)
                finish()
                true
            }
            KeyEvent.KEYCODE_F5 -> {
                // F2キーが押されたときの処理
                val intent = Intent(this,SettingsActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            KeyEvent.KEYCODE_ENTER -> {
                // エンターキーが押されたときの処理を無効にする
                false
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }
}