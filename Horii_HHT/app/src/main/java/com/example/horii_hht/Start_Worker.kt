package com.example.horii_hht

import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.Worker
import com.example.horii_hht.DB.WorkerDAO
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Database
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Start_Worker : AppCompatActivity() {
private  lateinit var db: AppDatabase
private lateinit var dao: WorkerDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.start_worker)

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
                   val initialWorker = Worker("1" ,"John Doe")
                   dao.insert(initialWorker)

           }

           }
       }

        val workerCD = findViewById<EditText>(R.id.workerCD)

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> {
                val workerCDEditText = findViewById<EditText>(R.id.workerCD)
                val inputWorkerCD = workerCDEditText.text.toString()
                lifecycleScope.launch {
                    val worker = withContext(Dispatchers.IO) { dao.getWorkerCD(inputWorkerCD) }
                    if (worker != null) {
                        // workerCDが合致した場合、画面遷移
                        val intent = Intent(this@Start_Worker, Main_Menu::class.java)
                        startActivity(intent)
                    } else {
                        // workerCDが合致しない場合、トースト表示
                        Toast.makeText(this@Start_Worker, "担当者が登録されていません", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            KeyEvent.KEYCODE_F4 -> {
                // F4キーが押されたときの処理
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}