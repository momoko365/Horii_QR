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

class Nyuka01_QRread : AppCompatActivity() {
    private lateinit var filter: IntentFilter //インテントフィルターを初期化
    private var readerManager: ReaderManager? = null //ReaderManagerを初期化
    private lateinit var db: AppDatabase //データベースを初期化
    private lateinit var dao: ItemDAO //DAOを初期化
    private lateinit var screenReceiver: ScreenStateReceiver //ScreenStateReceiverを初期化
    private val handler = Handler(Looper.getMainLooper()) // ハンドラ
    private val checkInterval: Long = 10000 // 10秒ごとにチェック

    private val checkRunnable = object : Runnable { // チェック用のRunnable

        override fun run() {
            resetDatabaseAndShowDialog() // データベースをリセットしてダイアログ表示
            handler.postDelayed(this, checkInterval) // 10秒後に再度実行
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka01)

        handler.post(checkRunnable)  // 定期的にresetDatabaseAndShowDialogを呼び出す
        screenReceiver = ScreenStateReceiver()  // ScreenStateReceiverの初期化と登録
        val screenfilter = IntentFilter().apply {   // スクリーン用のインテントフィルター設定
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenfilter)  // スクリーンのオンオフのBroadcastReceiverの登録

        readerManager = ReaderManager.InitInstance(this) // ReaderManagerの初期化
        filter = IntentFilter().apply {  // インテントフィルタの初期化（ハードウェアスキャンをサポート）
            addAction(GeneralString.Intent_PASS_TO_APP) // ハードウェアスキャン用
        }
        registerReceiver(scanDataReceiver, filter)  // BroadcastReceiverの登録（スキャンデータのブロードキャスト受信準備）

        lifecycleScope.launch { // データベースの初期化
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()

            dao = db.itemDAO()   //データベースオブジェクトの取得
        }
    }

    private val scanDataReceiver = object : BroadcastReceiver() { // スキャン結果を受け取るBroadcastReceiver
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {
                GeneralString.Intent_PASS_TO_APP -> {
                    var scannedData = intent.getStringExtra(GeneralString.BcReaderData)
                    Log.d("ScanData", "Received raw data: $scannedData")

                    if (scannedData != null) {
                        var cleanedData = scannedData.replace("\n", "")
                        var dataParts = cleanedData.split(",")
                        var validDataParts = (dataParts.size / 8) * 8

                        val itemsToInsert = mutableListOf<Item>()  // データ挿入用のリスト

                        val currentDate = Date()  // 現在の日時を取得
                        val dateFormat = // 日時を指定の形式でフォーマット
                            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                        val formattedDate = dateFormat.format(currentDate)
                        for (i in 0 until validDataParts step 8) {
                            val item = Item(
                                id = 0,
                                kenpinNo = dataParts[i],
                                kenpinpage = dataParts[i + 1],
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
                            itemsToInsert.add(item) // 挿入するアイテムをリストに追加
                        }

                        lifecycleScope.launch(Dispatchers.IO) { // データベースに一括挿入
                            try {
                                dao.insert(itemsToInsert)
                                val allItem = dao.getItemAll()
                                Log.d("Database", "Current items in database: $allItem")

                                // すべての処理が完了してから次の画面に遷移
                                runOnUiThread {
                                    val nextIntent =
                                        Intent(this@Nyuka01_QRread, Nyuka02_KenpinStart::class.java)
                                    startActivity(nextIntent)
                                    finish()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                runOnUiThread {
                                    AlertDialog.Builder(this@Nyuka01_QRread)
                                        .setTitle("エラー")
                                        .setMessage("不正なQRコードです。")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                            }
                        }
                    } else {
                        if (!isFinishing && !isDestroyed) {
                            AlertDialog.Builder(this@Nyuka01_QRread)
                                .setTitle("エラー")
                                .setMessage("不正なQRコードです。")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }

                else -> {
                    if (!isFinishing && !isDestroyed) {
                        AlertDialog.Builder(this@Nyuka01_QRread)
                            .setTitle("エラー")
                            .setMessage("不正なQRコードです。")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F4 -> { // F4キーが押されたときメインメニューに戻る処理
                val intent = Intent(this, Main_Menu::class.java)
                startActivity(intent)
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    // Activity破棄される時に呼び出されるライフサイクルメソッド
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable) // ハンドラの停止
        unregisterReceiver(scanDataReceiver)  // BroadcastReceiverの解除
        readerManager?.Release() // ReaderManagerの解放
        Log.d("ScanData", "Received raw data: ${intent.getStringExtra(GeneralString.BcReaderData)}")
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
            val maxTimes = dao.getMaxTimes()  // データベースから最大時間を取得
            Log.d("Nyuka04_Num", "maxTimes: $maxTimes")  // 取得した最大時間をログに出力
            val dateFormat =
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())  // 日時フォーマットの設定
            val maxKenpinDate = // maxKenpinTimeが空でない場合に解析
                maxTimes?.maxKenpinTime?.takeIf { it.isNotEmpty() }?.let { dateFormat.parse(it) }
            val maxQRDate =  // maxQRTimeが空でない場合に解析
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
                val currentDate = Date()  // 現在の日時を取得
                // 設定した時間を取得
                val sharedPreferences: SharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this@Nyuka01_QRread)
                val lockTimeMinutes =
                    sharedPreferences.getString("lock_time", "5")?.toLongOrNull() ?: 5
                val lockTimeMillis = lockTimeMinutes * 60 * 1000
                // 現在の日時と最大時間の差分が設定した時間を超えている場合
                if (currentDate.time - maxDate.time >= lockTimeMillis) {
                    dao.deleteAllItems() // データベースの全アイテムを削除
                    // メインスレッドでダイアログを表示
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@Nyuka01_QRread)
                            .setTitle("注意")
                            .setMessage("経過時間$lockTimeMinutes 分。全ての作業を取り消しました。メインメニューに戻ります。")
                            .setPositiveButton("OK") { _, _ ->
                                // メインメニューに遷移
                                val intent = Intent(this@Nyuka01_QRread, Main_Menu::class.java)
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
        val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("lastActivity", this::class.java.simpleName)
        editor.apply()
    }
}
