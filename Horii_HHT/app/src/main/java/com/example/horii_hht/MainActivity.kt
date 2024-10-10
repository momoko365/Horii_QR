package com.example.horii_hht

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var sd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.start_date)

        sd = findViewById(R.id.sd)
        sd.setOnClickListener {
            setContentView(R.layout.activity_main)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F5 -> {
                // F1キーが押されたときの処理
                Toast.makeText(this, "押されたよ", Toast.LENGTH_SHORT).show()
                true
            }
            KeyEvent.KEYCODE_F4 -> {
                // F4キーが押されたときの処理
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}