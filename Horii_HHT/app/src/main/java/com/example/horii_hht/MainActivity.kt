package com.example.horii_hht

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
//    private lateinit var sd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.start_date)
        val yearEditText = findViewById<EditText>(R.id.year)
        val monthEditText = findViewById<EditText>(R.id.month)
        val dayEditText = findViewById<EditText>(R.id.day)
//入力後エンターでフォーカス移動
        yearEditText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                monthEditText.requestFocus()
                true
            } else {
                false
            }
        }
//入力後エンターでフォーカス移動
        monthEditText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                dayEditText.requestFocus()
                true
            } else {
                false
            }
        }
//入力後エンターでフォーカス移動
        dayEditText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                // Handle the Enter key press on the last EditText if needed
                true
            } else {
                false
            }
        }
    }



    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> {
                // F1キーが押されたときの処理
//                Toast.makeText(this, "押されたよ", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Start_Worker::class.java)
                startActivity(intent)
                true
            }
//            KeyEvent.KEYCODE_F4 -> {
//                // F4キーが押されたときの処理
//                finish()
//                true
//            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}