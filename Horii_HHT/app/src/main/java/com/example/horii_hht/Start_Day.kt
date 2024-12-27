package com.example.horii_hht

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.ItemDAO
import com.example.horii_hht.databinding.StartDateBinding
import com.example.horii_hht.nyuka.Nyuka01_QRread
import com.example.horii_hht.nyuka.Nyuka02_KenpinStart
import com.example.horii_hht.nyuka.Nyuka03_Barread
import com.example.horii_hht.nyuka.Nyuka04_Num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class Start_Day : AppCompatActivity() {
    //    private lateinit var sd: Button
    private lateinit var tempFile: File
    private var fromStartWorker: String? = null
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.start_date)



        val kakutei_btn= findViewById<Button>(R.id.kakutei_btn)
        val end_btn= findViewById<Button>(R.id.back_btn)

        kakutei_btn.setOnClickListener {
            val intent = Intent(this, Start_Worker::class.java)
            startActivity(intent)
            true

        }

        end_btn.setOnClickListener {
            // ダイアログを作成
            val builder = AlertDialog.Builder(this)
            builder.setMessage("アプリを終了しますか？")
                .setCancelable(false) // ダイアログの外をタップしても閉じない
                .setPositiveButton("はい") { _, _ ->
                    finishAndRemoveTask() // アプリのタスクを完全に終了
                }
                .setNegativeButton("いいえ") { dialog, _ ->

                    dialog.dismiss() // ダイアログを閉じる

                }
            // ダイアログを表示
            val alertDialog = builder.create()
            alertDialog.show()
            true
        }



        // データベースの初期化
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                dao = db.itemDAO()
            }

            // `dao`の初期化が完了した後に実行する処理
            fromStartWorker = intent.getStringExtra("fromStartWorker")
            Log.d("Start_Day", "fromStartWorker: $fromStartWorker")

            val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)

            if (fromStartWorker == "start_Worker") {
                sharedPreferences.edit()
                    .clear()
                    .apply()
                Log.d("SharedPreferences", "クリア後: ${sharedPreferences.getString("lastActivity", null)}")
            }

            val lastActivity = sharedPreferences.getString("lastActivity", null)
            Log.d("SharedPreferences", "あるはず: $lastActivity")

            if (lastActivity != null) {
                val items = withContext(Dispatchers.IO) {
                    dao.getItemAll() // データベースから全てのアイテムを取得
                }

                if (items.isNotEmpty()) {
                    val intent = when (lastActivity) {
                        "Nyuka01_QRread" -> Intent(this@Start_Day, Nyuka01_QRread::class.java)
                        "Nyuka02_KenpinStart" -> Intent(this@Start_Day, Nyuka02_KenpinStart::class.java)
                        "Nyuka03_Barread" -> Intent(this@Start_Day, Nyuka03_Barread::class.java)
                        "Nyuka04_Num" -> Intent(this@Start_Day, Nyuka04_Num::class.java)
                        else -> null
                    }

                    sharedPreferences.edit()
                        .clear()
                        .apply()

                    intent?.let {
//                        CustomDialog.Builder(this@Start_Day)
//                            .setTitle("再開確認")
//                            .setMessage("前回中断したところから再開しますか")
//                            .setPositiveButton("はい"){
//                                startActivity(it)
//                                finish()
//                            }
//                            .setNegativeButton("いいえ"){
//                                lifecycleScope.launch {
//                                    withContext(Dispatchers.IO) {
//                                        dao.deleteAllItems()
//                                    }
//                                }
//                            }
//                            .build()
//                            .show(supportFragmentManager, CustomDialog::class.simpleName)

                        val dialog = AlertDialog.Builder(this@Start_Day)
                            .setTitle("再開確認")
                            .setMessage("前回中断したところから再開しますか？")
                            .setNegativeButton("いいえ") { _, _ ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        dao.deleteAllItems()
                                    }
                                }
                            }
                            .setPositiveButton("はい") { _, _ ->
                                startActivity(it)
                                finish()
                            }
                            .create()
                        dialog.setCancelable(false) // ダイアログの外をタップしても閉じない

                        dialog.setOnShowListener {
                            // ダイアログ表示後に「はい」ボタンにフォーカスを設定
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.requestFocus()
                        }

                        dialog.show()
                    }
                }
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
            KeyEvent.KEYCODE_F8 -> {

                // ダイアログを作成
                val builder = AlertDialog.Builder(this)
                builder.setMessage("アプリを終了しますか？")
                    .setCancelable(false) // ダイアログの外をタップしても閉じない
                    .setPositiveButton("はい") { _, _ ->
                        finishAndRemoveTask() // アプリのタスクを完全に終了
                    }
                    .setNegativeButton("いいえ") { dialog, _ ->

                        dialog.dismiss() // ダイアログを閉じる

                    }
                // ダイアログを表示
                val alertDialog = builder.create()
                alertDialog.show()
                    true
            }
            KeyEvent.KEYCODE_BACK -> {
                // バックキーが押されたときの処理
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



