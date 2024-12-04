package com.example.horii_hht

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.horii_hht.nyuka.Nyuka01_QRread
import com.example.horii_hht.nyuka.Nyuka02_KenpinStart
import com.example.horii_hht.nyuka.Nyuka03_Barread
import com.example.horii_hht.nyuka.Nyuka04_Num
import java.io.File
import java.util.Calendar

class Start_Day : AppCompatActivity() {
    //    private lateinit var sd: Button
    private lateinit var tempFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.start_date)

        val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)
        val lastActivity = sharedPreferences.getString("lastActivity", null)

        if (lastActivity != null) {
            val intent = when (lastActivity) {
                "Nyuka01_QRread" -> Intent(this, Nyuka01_QRread::class.java)
                "Nyuka02_KenpinStart" -> Intent(this, Nyuka02_KenpinStart::class.java)
                "Nyuka03_Barread" -> Intent(this, Nyuka03_Barread::class.java)
                "Nyuka04_Num" -> Intent(this, Nyuka03_Barread::class.java)
                // 他のアクティビティも必要に応じて追加
                else -> null
            }
            intent?.let {
                AlertDialog.Builder(this)
                    .setTitle("再開確認")
                    .setMessage("前回中断したところから再開しますか？")
                    .setNegativeButton("いいえ", null)
                    .setPositiveButton("はい") { _, _ ->
                        startActivity(it)
                        finish()
                    }
                    .show()
            }
        }

        val yearEditText = findViewById<EditText>(R.id.year)
        val monthEditText = findViewById<EditText>(R.id.month)
        val dayEditText = findViewById<EditText>(R.id.day)


        // 今日の日付を取得
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTHは0から始まるため+1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // EditTextにデフォルト表示
        yearEditText.setText(year.toString())
        monthEditText.setText(month.toString())
        dayEditText.setText(day.toString())

        yearEditText.setSelection(yearEditText.text.length) // カーソルを末尾に移動
        monthEditText.setSelection(monthEditText.text.length) // カーソルを末尾に移動
        dayEditText.setSelection(dayEditText.text.length) // カーソルを末尾に移動

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
        // ファイルを作成
        val filesDir = filesDir // アプリ専用の内部ストレージディレクトリ
        tempFile = File(filesDir, "example.txt")

        if (!tempFile.exists()) {
            tempFile.createNewFile()
            tempFile.writeText("このファイルは一時的に作成されます。")
            Log.d("SplashActivity", "ファイルが作成されました: ${tempFile.absolutePath}")
        } else {
            Log.d("SplashActivity", "ファイルは既に存在します: ${tempFile.absolutePath}")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> {
                // F1キーが押されたときの処理
                val intent = Intent(this, Start_Worker::class.java)
                startActivity(intent)
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // アプリ終了時処理
        // ファイルを削除
        val filesDir = filesDir
        val tempFile = File(filesDir, "example.txt")
        if (tempFile.exists()) {
            tempFile.delete()
            Log.d("MyApplication", "ファイルが削除されました: ${tempFile.absolutePath}")
        }
    }

}