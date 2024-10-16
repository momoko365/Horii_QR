package com.example.horii_hht

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity

class Nyuka02_KenpinStart : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka02)
        val kenpinBtn = findViewById<android.widget.Button>(R.id.startbtn)
        //DBからデータを取得してそれぞれに表示する処理必要
        //スキャンするたびに表示を更新していくように
        val itemNum = findViewById<android.widget.TextView>(R.id.real_itemNum)
        val itemAll = findViewById<android.widget.TextView>(R.id.real_itemAll)
        val kenpinNo = findViewById<android.widget.TextView>(R.id.kenpinNo)

        kenpinBtn.setOnClickListener {
            //商品点数と商品総数が検品リストのものと一致したら次の画面に遷移できるようにする
            //一致するまでボタン押せないようにしたい
            val intent = Intent(this, Nyuka03_Barread::class.java)
            startActivity(intent)
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {

            KeyEvent.KEYCODE_F7 -> {
                // F7キーが押されたときDBリセットする処理

                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}