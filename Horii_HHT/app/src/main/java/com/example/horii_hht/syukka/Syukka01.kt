package com.example.horii_hht.syukka

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.horii_hht.CustomDialog
import com.example.horii_hht.R
import com.example.horii_hht.Start_Worker

class Syukka01 : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.syukka01)
        val casebtn = findViewById<Button>(R.id.syukkabtn)
        val barabtn = findViewById<Button>(R.id.tanaoroshibtn)

        casebtn.setOnClickListener {
            val intent = Intent(this, Syukka_C02::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> {
                // F1キーが押されたときの処理
                val intent = Intent(this, Syukka_C02::class.java)
                startActivity(intent)
                finish()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

}