package com.example.horii_hht.nyuka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.Item
import com.example.horii_hht.DB.ItemDAO
import com.example.horii_hht.Main_Menu
import com.example.horii_hht.R
import com.example.horii_hht.setting.ScreenStateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Nyuka02_KenpinStart : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null
    private var scannedData: String? = null
    private lateinit var screenReceiver: ScreenStateReceiver
    private val handler = Handler(Looper.getMainLooper()) // ハンドラ
    private val checkInterval: Long = 10000 // 10秒ごとにチェック

    private val checkRunnable = object : Runnable { // チェック用のRunnable
        override fun run() {
            resetDatabaseAndShowDialog() // データベースをリセットしてダイアログ表示
            handler.postDelayed(this, checkInterval) // 10秒後に再度実行
        }
    }

    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka02)

        val QRread = findViewById<Button>(R.id.textView4)
        QRread.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                scannedData = null
                dao.deleteAllItems()
                val distinctItemCount = dao.getDistinctItemCount() ?: 0
                val totalSuryo = dao.getTotalCase() ?: 0
                val totalBara = dao.getTotalBara() ?: 0
                withContext(Dispatchers.Main) {
                    if (distinctItemCount == 0 && totalSuryo == 0 && totalBara == 0) {
                        val itemNum = findViewById<TextView>(R.id.real_itemNum)
                        val itemAll = findViewById<TextView>(R.id.real_itemAll)
                        itemNum.text = distinctItemCount.toString()
                        itemAll.text = totalSuryo.toString()
                    } else {
                        Toast.makeText(
                            this@Nyuka02_KenpinStart,
                            "リセットに失敗しました。",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                withContext(Dispatchers.Main) {
                    withContext(Dispatchers.IO) {
                        val allItem = dao.getItemAll()
                        Log.d("Database", "Current items in database: $allItem")
                    }
                    val intent = Intent(this@Nyuka02_KenpinStart, Nyuka01_QRread::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            true
        }

        source = intent.getStringExtra("source")

        // 定期的にresetDatabaseAndShowDialogを呼び出す
        handler.post(checkRunnable)
        // ScreenStateReceiverの初期化と登録
        screenReceiver = ScreenStateReceiver()
        val screenfilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenfilter)


        // ReaderManagerの初期化
        readerManager = ReaderManager.InitInstance(this)

        // インテントフィルタの初期化
        filter = IntentFilter().apply {
            addAction(GeneralString.Intent_PASS_TO_APP) // ハードウェアスキャン用
        }

        // BroadcastReceiverの登録
        registerReceiver(scanDataReceiver, filter)

        //商品点数テキスト
        val itemNum = findViewById<TextView>(R.id.real_itemNum)
        //ケース数テキスト
        val caseAll = findViewById<TextView>(R.id.real_itemAll)
        //バラ数テキスト
        val baraAll = findViewById<TextView>(R.id.realbara)
        //検品番号テキスト
        val kenpinNo = findViewById<TextView>(R.id.kenpinNo)

        // データベースの初期化
        lifecycleScope.launch(Dispatchers.IO) {
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .fallbackToDestructiveMigration()
                .build()
            dao = db.itemDAO()

            // データベースからデータを取得
            val distinctItemCount = dao.getDistinctItemCount()
            val totalCase = dao.getTotalCase()
            val totalBara = dao.getTotalBara()
            val kenpinNoValue = dao.getKenpinNo()
            val kenpinpage = dao.getKenpinpage()
            val check = dao.getItemCount()
            // UIスレッドでテキストビューに値を設定
            launch(Dispatchers.Main) {
                itemNum.text = distinctItemCount.toString()
                caseAll.text = totalCase.toString()
                baraAll.text = totalBara.toString()
                kenpinNo.text = "$kenpinNoValue-$kenpinpage"
            }
        }

        val kenpinBtn = findViewById<Button>(R.id.startbtn)
        kenpinBtn.setOnClickListener {
            val intent = Intent(this, Nyuka03_Barread::class.java)
            startActivity(intent)
            finish()
        }
    }

    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == GeneralString.Intent_PASS_TO_APP) {
                val receivedData = intent.getStringExtra(GeneralString.BcReaderData) ?: return

                Log.d("ScanData", "Received raw data: $receivedData")

                if (receivedData != null && receivedData != scannedData) {
                    scannedData = receivedData

                    // 改行文字を削除
                    val cleanedData = scannedData!!.replace("\n", "").replace(":", "")
                    val dataParts = cleanedData.split(",")

                    // データ挿入用のリスト
                    val itemsToInsert = mutableListOf<Item>()

                    // 7の倍数のデータだけ処理
                    val validDataParts = (dataParts.size / 8) * 8
                    Log.d("ScanData", "Received raw data: $scannedData")

                    if (validDataParts >= 8) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            for (i in 0 until validDataParts step 8) {
                                val kenpinNo = dataParts[i]
                                val kenpinpage = dataParts[i + 1]


                                // 現在の日時を取得
                                val currentDate = Date()
// 日時を指定の形式でフォーマット
                                val dateFormat =
                                    SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                                val formattedDate = dateFormat.format(currentDate)
                                // 既存のデータをチェック
                                val count = dao.duplicationQR(kenpinNo, kenpinpage)
                                if (count > 0) {
                                    withContext(Dispatchers.Main) {
                                        AlertDialog.Builder(this@Nyuka02_KenpinStart)
                                            .setTitle("エラー")
                                            .setMessage("すでに読み込まれているQRコードです")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
                                    return@launch
                                }

                                val item = Item(
                                    id = 0,
                                    kenpinNo = kenpinNo,
                                    kenpinpage = kenpinpage,
                                    itemCD = dataParts[i + 2],
                                    itemName = dataParts[i + 3],
                                    case_q = dataParts[i + 4].toInt(),
                                    bara = dataParts[i + 5].toInt(),
                                    JAN = dataParts[i + 6],
                                    ITF = dataParts[i + 7],
                                    casezumi = 0,
                                    barazumi = 0,
                                    kenpinTime = "",
                                    QRTime = formattedDate
                                )
                                itemsToInsert.add(item)
                            }

                            try {
                                // データベースにインサート
                                dao.insert(itemsToInsert)

                                val allItem = dao.getItemAll()
                                Log.d("Database", "Current items in database: $allItem")

                                // UIスレッドでアクティビティを再起動
                                withContext(Dispatchers.Main) {
                                    val intent = Intent(
                                        this@Nyuka02_KenpinStart,
                                        Nyuka02_KenpinStart::class.java
                                    )
                                    finish() // 現在のアクティビティを終了
                                    startActivity(intent)
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("DatabaseError", "Error during insertion", e)
                                withContext(Dispatchers.Main) {
                                    AlertDialog.Builder(this@Nyuka02_KenpinStart)
                                        .setTitle("エラー")
                                        .setMessage("不正なQRコードです。")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@Nyuka02_KenpinStart,
                            "スキャンデータが取得できませんでした",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F7 -> {

                lifecycleScope.launch(Dispatchers.IO) {
                    scannedData = null
                    dao.deleteAllItems()
                    val distinctItemCount = dao.getDistinctItemCount() ?: 0
                    val totalSuryo = dao.getTotalCase() ?: 0
                    val totalBara = dao.getTotalBara() ?: 0
                    withContext(Dispatchers.Main) {
                        if (distinctItemCount == 0 && totalSuryo == 0 && totalBara == 0) {
                            val itemNum = findViewById<TextView>(R.id.real_itemNum)
                            val itemAll = findViewById<TextView>(R.id.real_itemAll)
                            itemNum.text = distinctItemCount.toString()
                            itemAll.text = totalSuryo.toString()
                        } else {
                            Toast.makeText(
                                this@Nyuka02_KenpinStart,
                                "リセットに失敗しました。",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            val allItem = dao.getItemAll()
                            Log.d("Database", "Current items in database: $allItem")
                        }
                        val intent = Intent(this@Nyuka02_KenpinStart, Nyuka01_QRread::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
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
        // ハンドラのコールバックを削除
        handler.removeCallbacks(checkRunnable)
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
        // ReaderManagerの解放
        readerManager?.Release()
        unregisterReceiver(screenReceiver)
    }

    // ダイアログを表示するメソッド
    fun showWorkingDialog() {
        AlertDialog.Builder(this)
            .setTitle("作業中")
            .setMessage("作業中です")
            .setPositiveButton("OK", null)
            .show()
    }

    //指定した時間分放置してたら起動するメソッド
    fun resetDatabaseAndShowDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            // データベースから最大時間を取得
            val maxTimes = dao.getMaxTimes()
            // 取得した最大時間をログに出力
            Log.d("Nyuka04_Num", "maxTimes: $maxTimes")
            // 日時フォーマットの設定
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            // maxKenpinTimeが空でない場合に解析
            val maxKenpinDate =
                maxTimes?.maxKenpinTime?.takeIf { it.isNotEmpty() }?.let { dateFormat.parse(it) }
            // maxQRTimeが空でない場合に解析
            val maxQRDate =
                maxTimes?.maxQRTime?.takeIf { it.isNotEmpty() }?.let { dateFormat.parse(it) }
            // maxKenpinDateとmaxQRDateのうち、より直近の時間の方を取得
            val maxDate = when {
                maxKenpinDate != null && maxQRDate != null -> maxOf(maxKenpinDate, maxQRDate)
                maxKenpinDate != null -> maxKenpinDate
                maxQRDate != null -> maxQRDate
                else -> null
            }

            // maxDateがnullでない場合に処理を実行
            if (maxDate != null) {
                // 現在の日時を取得
                val currentDate = Date()

                // 設定した時間を取得
                val sharedPreferences: SharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this@Nyuka02_KenpinStart)
                val lockTimeMinutes =
                    sharedPreferences.getString("lock_time", "5")?.toLongOrNull() ?: 5
                val lockTimeMillis = lockTimeMinutes * 60 * 1000

                // 現在の日時と最大時間の差分が設定した時間を超えている場合
                if (currentDate.time - maxDate.time >= lockTimeMillis) {
                    // データベースの全アイテムを削除
                    dao.deleteAllItems()
                    // メインスレッドでダイアログを表示
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@Nyuka02_KenpinStart)
                            .setTitle("注意")
                            .setMessage("経過時間$lockTimeMinutes 分。全ての作業を取り消しました。メインメニューに戻ります。")
                            .setPositiveButton("OK") { _, _ ->
                                // メインメニューに遷移
                                val intent = Intent(this@Nyuka02_KenpinStart, Main_Menu::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .show()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        if (source != "Start_Day") {
        val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("lastActivity", this::class.java.simpleName)
        editor.apply()
    }
//    }
}