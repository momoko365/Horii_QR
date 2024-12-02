package com.example.horii_hht

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class Start_Day : AppCompatActivity() {
//    private lateinit var sd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.start_date)
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


        // SharedPreferencesから最後のアクティビティを取得
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val lastActivity = sharedPreferences.getString("last_activity", null)

        // 最後のアクティビティが存在する場合、そのアクティビティを開始
        if (lastActivity != null) {
            showResumeDialog(lastActivity)
        } else {
            // 通常の起動処理
            setContentView(R.layout.activity_main)
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
    // 中断されたところから再開するダイアログを表示
    private fun showResumeDialog(activityName: String) {
        AlertDialog.Builder(this)
            .setTitle("再開")
            .setMessage("中断されたところから再開します。")
            .setPositiveButton("OK") { _, _ ->
                try {
                    val clazz = Class.forName("com.example.Horii_HHT.$activityName")
                    val intent = Intent(this, clazz)
                    startActivity(intent)
                    finish()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("キャンセル") { _, _ ->
                // 通常の起動処理
                setContentView(R.layout.activity_main)
            }
            .show()
    }
}