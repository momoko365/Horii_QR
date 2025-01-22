package com.example.horii_hht

import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.Worker
import com.example.horii_hht.DB.WorkerDAO
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Start_Worker : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var dao: WorkerDAO
    private lateinit var workerCDEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.start_worker)

        val kakutei_btn = findViewById<Button>(R.id.kakutei_btn)
        val back_btn = findViewById<Button>(R.id.back_btn)

        kakutei_btn.setOnClickListener {
            workerCDEditText = findViewById<EditText>(R.id.workerCD)
            workerCDEditText.setSelection(workerCDEditText.text.length) // カーソルを末尾に移動
            val inputWorkerCD = workerCDEditText.text.toString()
            lifecycleScope.launch {
                val worker = withContext(Dispatchers.IO) { dao.getWorkerCD(inputWorkerCD) }
                if (worker != null) {
                    // workerCDが合致した場合、画面遷移
                    val intent = Intent(this@Start_Worker, Main_Menu::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // workerCDが合致しない場合、ダイアログ表示
                    CustomDialog.Builder(this@Start_Worker)
                        .setTitle("エラー")
                        .setMessage("コードの誤りです")
                        .setPositiveButton("OK")
                        .setNegativeButton("")
                        .build()
                        .show(supportFragmentManager, CustomDialog::class.simpleName)
                    true

                }
            }
            true
        }

        back_btn.setOnClickListener {
            val intent = Intent(this, Start_Day::class.java)
            intent.putExtra("fromStartWorker", "start_Worker")
            startActivity(intent)
            finish()
            true
        }

        // workerCDEditTextを初期化
        workerCDEditText = findViewById<EditText>(R.id.workerCD)

        workerCDEditText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                // スペースキーの入力を無効にする
                true
            } else {
                false
            }
        }

        // データベースの初期化
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                dao = db.workerDAO()
                // 初期データが存在しない場合にのみデータを挿入
                val workerCount = dao.getAll()
                if (workerCount == 0) {
                    // 初期データを挿入
                    val initialWorker = Worker("1", "John Doe")
                    dao.insert(initialWorker)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> {
                workerCDEditText = findViewById<EditText>(R.id.workerCD)
                workerCDEditText.setSelection(workerCDEditText.text.length) // カーソルを末尾に移動
                val inputWorkerCD = workerCDEditText.text.toString()
                lifecycleScope.launch {
                    val worker = withContext(Dispatchers.IO) { dao.getWorkerCD(inputWorkerCD) }
                    if (worker != null) {
                        // workerCDが合致した場合、画面遷移
                        val intent = Intent(this@Start_Worker, Main_Menu::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // workerCDが合致しない場合、ダイアログ表示
                        CustomDialog.Builder(this@Start_Worker)
                            .setTitle("エラー")
                            .setMessage("コードの誤りです")
                            .setPositiveButton("OK")
                            .setNegativeButton("")
                            .build()
                            .show(supportFragmentManager, CustomDialog::class.simpleName)
                        true
                    }
                }
                true
            }

            KeyEvent.KEYCODE_F4 -> {
                // F4キーが押されたときの処理
                val intent = Intent(this, Start_Day::class.java)
                intent.putExtra("fromStartWorker", "start_Worker")
                startActivity(intent)
                finish()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                // バックキーが押されたときの処理
                true
            }
            KeyEvent.KEYCODE_ENTER -> {
                // エンターキーが押されたときの処理
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }
}