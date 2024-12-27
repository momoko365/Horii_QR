package com.example.horii_hht.nyuka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.Barcode
import com.example.horii_hht.DB.BarcodeDAO
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
import java.util.Timer
import java.util.TimerTask

class Nyuka03_Barread : AppCompatActivity() {
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private lateinit var barcodedao: BarcodeDAO
    private var data: String? = null
    private lateinit var barcodedata: EditText
    private lateinit var tyudanbtn: Button


    //画面ロックのフラグ
    private var isLocked: Boolean = false

    //スキャンデータのソースを識別するためのフラグを設定
    private var isScanner = false

    // itemsをクラス変数として定義
    private var items: List<Item> = mutableListOf()

    private lateinit var screenReceiver: ScreenStateReceiver
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
        setContentView(R.layout.nyuka03)

        val kakutei_btn = findViewById<Button>(R.id.kakutei_btn)
        val kenpinstart_btn = findViewById<Button>(R.id.textView555)

        kakutei_btn.setOnClickListener {
            // barcodedata.text = キーボードからの入力、空だったらスキャンデータを使う
            val barcodeInput = barcodedata.text.toString().ifEmpty { data }
            lifecycleScope.launch(Dispatchers.IO) {
                if (barcodeInput != null){
                    if(barcodeInput.length == 13 || barcodeInput.length == 14) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val items = dao.getCSVdata(barcodeInput)
                            var itemBar = dao.getTotal(barcodeInput)


                            withContext(Dispatchers.Main) {
                                if (itemBar != null) {
                                    if (items.isNotEmpty()) {
                                        if (itemBar.bara == itemBar.barazumi && itemBar.case_q == itemBar.casezumi){
                                            withContext(Dispatchers.Main) {
                                                showAlertDialog("検品終了", "その商品は検品終了してます")
                                                barcodedata.text.clear()
                                            }
                                        }else{
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                barcodedao.deleteAllBarcode()
                                                val barcode = Barcode(code = barcodeInput)
                                                barcodedao.insert(barcode)
                                            }
                                            val intent = Intent(this@Nyuka03_Barread, Nyuka04_Num::class.java)
                                            intent.putExtra("barcode", barcodeInput)
                                            startActivity(intent)
                                            finish()
                                        }

                                    } else {
                                        showAlertDialog("エラー", "商品が見つかりません")
                                        barcodedata.text.clear()
                                    }
                                }
                            }
                        }
                    }else{
                        withContext(Dispatchers.Main) {
                            showAlertDialog(
                                "エラー",
                                "有効なJANまたはITFコードを入力してください"
                            )
                            barcodedata.text.clear()
                        }
                    }
                }else{
                    withContext(Dispatchers.Main) {
                        showAlertDialog("エラー", "バーコードを入力してください")
                        barcodedata.text.clear()
                    }
                }
            }
            true
        }

        kenpinstart_btn.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // zumi数をすべて0に戻し、timeをnullにする
                    dao.resetZumiAndTime()
                }
                // Nyuka02_KenpinStartへ遷移
                val intent = Intent(this@Nyuka03_Barread, Nyuka02_KenpinStart::class.java)
                startActivity(intent)
                finish()
            }
            true
        }
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
        barcodedata = findViewById(R.id.barcode)

        //ｹｰｽ数テキスト
//        val caseAll = findViewById<TextView>(R.id.itemAll)
        //ケース数済み数テキスト
        val casezumiAll = findViewById<TextView>(R.id.real_itemAll)
        //バラ数テキスト
//        val baraAll = findViewById<TextView>(R.id.baraall)
        //バラ数済み数テキスト
        val barazumiAll = findViewById<TextView>(R.id.barazumi)
        //商品点数テキスト
//        val itemNum = findViewById<TextView>(R.id.itemNum)
        //商品点数済み数テキスト
        val real_itemNum = findViewById<TextView>(R.id.real_itemNum)
        //検品番号テキスト
        val kenpinNo = findViewById<TextView>(R.id.kenpinNo)



        //作業中断ボタン
        tyudanbtn = findViewById<Button>(R.id.startbtn)
// SharedPreferencesの初期化
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // 作業中断ボタンのクリックリスナー
        tyudanbtn.setOnClickListener {
            if (isLocked) {
                // 画面ロック解除
                unlockScreen()
                tyudanbtn.text = "作業中断"
                tyudanbtn.setTextColor(Color.RED)
                // タイマーをキャンセル
                timer?.cancel()
            } else {
                // 画面ロック
                lockScreen()
                tyudanbtn.text = "作業再開"
                tyudanbtn.setTextColor(Color.BLUE)
//                disableEditText()
                barcodedata.isEnabled = false
                // タイマーを開始
                startLockTimer()
            }
            isLocked = !isLocked
        }

        // データベースの初期化
        lifecycleScope.launch(Dispatchers.IO) {
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()
            dao = db.itemDAO()
            barcodedao = db.barcodeDAO()


            // データベースからデータを取得
            val kenpinNoValue = dao.getKenpinNo() //検品番号
//            val kenpinpage = dao.getkenpinpagescandata()
            val distinctItemCount = dao.getDistinctItemCount() //商品点数
            val itemCheck = dao.getCSVdata() //商品点数済み数
            val caseTotal = dao.getTotalCase() //ケース数
            val casezumiTotal = dao.getCasezumi() //ケース数済み数
            val baraTotal = dao.getTotalBara() //バラ数
            val barazumiTotal = dao.getBarazumi() //バラ数済み数
            launch(Dispatchers.Main) {
                kenpinNo.text = kenpinNoValue //検品番号
//                itemNum.text = distinctItemCount.toString() //商品点数
                var count = 0
                for (i in itemCheck) {
                    if (i.totalBara == i.totalBarazumi && i.totalCasezumi == i.totalCaseQ) {
                        count++
                    }
                }

                val countFormat = String.format("%5d", count)
                val distinctItemCountFormat = String.format("%5d", distinctItemCount)

                val caseFormat = String.format("%5d", casezumiTotal)
                val baraFormat = String.format("%5d", barazumiTotal)
                val caseTotalFormat = String.format("%5d", caseTotal)
                val baraTotalFormat = String.format("%5d", baraTotal)

                real_itemNum.text = "$countFormat/$distinctItemCountFormat" // 商品点数済み数を表示
//                caseAll.text = caseTotal.toString() //ケース数
                casezumiAll.text = "$caseFormat/$caseTotalFormat" //ケース数済み数
//                baraAll.text = baraTotal.toString() //バラ数
                barazumiAll.text = "$baraFormat/$baraTotalFormat" //バラ数済み数
            }
        }

        // インテントフィルタの初期化（ハードウェアスキャンをサポート）
        filter = IntentFilter().apply {
            addAction(GeneralString.Intent_PASS_TO_APP) // ハードウェアスキャン用
        }
        // BroadcastReceiverの登録
        registerReceiver(scanDataReceiver, filter)

        // エディットテキストの入力監視
        barcodedata.addTextChangedListener(object : TextWatcher {
            //テキストが変更される直前に呼ばれる
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            //テキストが変更されてる最中に呼ばれる
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            //テキストが変更された直後に呼ばれる
            override fun afterTextChanged(s: Editable?) {
                // ハードウェアスキャンからの入力なら処理をスキップ
                if (isScanner) {
                    //フラグをリセットして次回以降の処理に備える
                    isScanner = false
                    //以降の処理を中断
                    return
                }
                //ユーザーが入力したテキストを取得
                val inputText = s.toString()
                //入力されたテキストが13桁か14桁の数字であるかをチェック
                if (isValidBarcode(inputText)) {
                    //有効なバーコードの場合検索実行
                    searchBarcodeAndNavigate(inputText)
                }
            }
        })
    }

    // タイマーを管理する変数
    private var timer: Timer? = null

    // タイマーを開始する関数
    private fun startLockTimer() {
        val lockTime = getLockTimeFromPreferences() // SharedPreferencesから時間を取得する関数
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                // lock_timeの時間経過後にデータベース削除の処理を実行
                resetDatabaseAndShowDialog()
            }
        }, lockTime)
    }

    // SharedPreferencesから時間を取得する関数
    private fun getLockTimeFromPreferences(): Long {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        // デフォルト値は5分（5 * 60 * 1000ミリ秒）
        return sharedPreferences.getLong("lock_time", 5 * 60 * 1000L)
    }

    // ハードウェアスキャン用の BroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        //スキャン受信
        override fun onReceive(context: Context, intent: Intent) {
            // isLocked が true の場合、処理を中断
            if (isLocked) {
                return
            }
            //受信したインテントがスキャナからの入力用のアクションであるかどうか確認
            if (intent.action == GeneralString.Intent_PASS_TO_APP) {
                //スキャンされたデータを取得
                data = intent.getStringExtra(GeneralString.BcReaderData)?.replace("\n", "") ?: ""
                // スキャンされたデータが13桁か14桁か判定
                if (isValidBarcode(data)) {
                    //スキャナからの入力であることを示すフラグをtrueに変更
                    isScanner = true
                    //スキャンデータを表示
                    barcodedata.setText(data)
                    barcodedata.setSelection(barcodedata.text.length)
                    //有効なデータだった場合検索実行
                    lifecycleScope.launch(Dispatchers.IO) {
                        val items = dao.getCSVdata(data!!)
                        var itemBar = dao.getTotal(data!!)
                        if (items.isNotEmpty()) {
                            if (itemBar != null) {
                                if (itemBar.bara == itemBar.barazumi && itemBar.case_q == itemBar.casezumi){
                                    withContext(Dispatchers.Main) {
                                        showAlertDialog("検品終了", "その商品は検品終了してます")
                                        barcodedata.text.clear()
                                    }
                                }else{
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        barcodedao.deleteAllBarcode()
                                        val barcode = Barcode(code = data!!)
                                        barcodedao.insert(barcode)
                                    }
                                    val intent = Intent(this@Nyuka03_Barread, Nyuka04_Num::class.java)
                                    intent.putExtra("barcode", data)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showAlertDialog("エラー", "商品が見つかりません")
                                barcodedata.text.clear()
                            }
                        }
                    }
                } else {
                    showAlertDialog("エラー", "コードが不正です")
                    barcodedata.text.clear()
                }
            }
        }
    }

    // 共通のバーコード検証関数(ITF or JANか調べる)
    private fun isValidBarcode(barcode: String?): Boolean {
        // nullチェックと13桁または14桁の数字であることを確認、改行は無視
        return barcode?.replace("\n", "")?.matches(Regex("\\d{13,14}")) == true

    }

    // データベース検索と画面遷移の共通処理
    private fun searchBarcodeAndNavigate(barcode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            // バーコードが13桁ならJAN検索、14桁ならITF検索
            items = if (barcode.length == 13) {
                dao.getItemByCode(jan = barcode, itf = "")
            } else {
                dao.getItemByCode(jan = "", itf = barcode)
            }
        }
    }

    // エラーダイアログ表示の共通関数
    private fun showAlertDialog(title: String, message: String) {
        // ダイアログを表示
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // ファンクションキー入力処理
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
//            KeyEvent.KEYCODE_F4 -> {
//                // F4キーが押されたときの処理
//                val intent = Intent(this, Main_Menu::class.java)
//                startActivity(intent)
//                true
//            }

            KeyEvent.KEYCODE_F1 -> {
                // barcodedata.text = キーボードからの入力、空だったらスキャンデータを使う
                val barcodeInput = barcodedata.text.toString().ifEmpty { data }
                lifecycleScope.launch(Dispatchers.IO) {
                    if (barcodeInput != null){
                        if(barcodeInput.length == 13 || barcodeInput.length == 14) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val items = dao.getCSVdata(barcodeInput)
                                var itemBar = dao.getTotal(barcodeInput)


                                withContext(Dispatchers.Main) {
                                    if (itemBar != null) {
                                        if (items.isNotEmpty()) {
                                            if (itemBar.bara == itemBar.barazumi && itemBar.case_q == itemBar.casezumi){
                                                withContext(Dispatchers.Main) {
                                                    showAlertDialog("検品終了", "その商品は検品終了してます")
                                                    barcodedata.text.clear()
                                                }
                                            }else{
                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    barcodedao.deleteAllBarcode()
                                                    val barcode = Barcode(code = barcodeInput)
                                                    barcodedao.insert(barcode)
                                                }
                                                val intent = Intent(this@Nyuka03_Barread, Nyuka04_Num::class.java)
                                                intent.putExtra("barcode", barcodeInput)
                                                startActivity(intent)
                                                finish()
                                            }

                                        } else {
                                            showAlertDialog("エラー", "商品が見つかりません")
                                            barcodedata.text.clear()
                                        }
                                    }
                                }
                            }
                        }else{
                            withContext(Dispatchers.Main) {
                                showAlertDialog(
                                    "エラー",
                                    "有効なJANまたはITFコードを入力してください"
                                )
                                barcodedata.text.clear()
                            }
                        }
                    }else{
                        withContext(Dispatchers.Main) {
                            showAlertDialog("エラー", "バーコードを入力してください")
                            barcodedata.text.clear()
                        }
                    }
                }
                true
            }

            KeyEvent.KEYCODE_F7 -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        // zumi数をすべて0に戻し、timeをnullにする
                        dao.resetZumiAndTime()
                    }
                    // Nyuka02_KenpinStartへ遷移
                    val intent = Intent(this@Nyuka03_Barread, Nyuka02_KenpinStart::class.java)
                    startActivity(intent)
                    finish()
                }
                true
            }

//            KeyEvent.KEYCODE_F6 -> {
//                lifecycleScope.launch {
//                    withContext(Dispatchers.IO) {
//                        // zumi数をすべて0に戻し、timeをnullにする
//                        dao.resetZumiAndTime()
//                    }
//                    // Nyuka02_KenpinStartへ遷移
//                    val intent = Intent(this@Nyuka03_Barread, Nyuka02_KenpinStart::class.java)
//                    startActivity(intent)
//                    finish()
//                }
//                true
//            }
            KeyEvent.KEYCODE_BACK -> {
                // バックキーが押されたときの処理
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    //画面ロック
    private fun lockScreen() {
        // ボタン以外すべてのビューを無効にする（画面ロック）
//        findViewById<View>(R.id.itemAll).isEnabled = false
        findViewById<View>(R.id.real_itemAll).isEnabled = false
//        findViewById<View>(R.id.itemNum).isEnabled = false
        findViewById<View>(R.id.real_itemNum).isEnabled = false
        findViewById<View>(R.id.kenpinNo).isEnabled = false
//        findViewById<View>(R.id.baraall).isEnabled = false
        findViewById<View>(R.id.barazumi).isEnabled = false
        findViewById<EditText>(R.id.barcode).isEnabled = false
        // ボタンは無効化しない
        tyudanbtn.isEnabled = true
    }

    //画面ロック解除
    private fun unlockScreen() {
        // ボタン以外のすべてのビューを有効にする例
//        findViewById<View>(R.id.itemAll).isEnabled = true
        findViewById<View>(R.id.real_itemAll).isEnabled = true
//        findViewById<View>(R.id.itemNum).isEnabled = true
        findViewById<View>(R.id.real_itemNum).isEnabled = true
        findViewById<View>(R.id.kenpinNo).isEnabled = true
//        findViewById<View>(R.id.baraall).isEnabled = true
        findViewById<View>(R.id.barazumi).isEnabled = true
        findViewById<EditText>(R.id.barcode).isEnabled = true
        // ボタンは引き続き有効
        tyudanbtn.isEnabled = true
    }

    // Activity破棄される時に呼び出されるライフサイクルメソッド
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

    // EditTextの入力を無効にするメソッド
    private fun disableEditText() {
        barcodedata.isEnabled = false // 入力を無効化
        barcodedata.isFocusable = false // フォーカスを無効化
        barcodedata.isFocusableInTouchMode = false // タッチによるフォーカスを無効化
        barcodedata.clearFocus() // フォーカスをクリア
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // isLocked が true の場合、すべてのキー入力を無効化
        if (isLocked) {
            return true // すべてのキーイベントを無効化
        }
        // ロックされていない場合は通常のキー処理
        return super.dispatchKeyEvent(event)
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
                    PreferenceManager.getDefaultSharedPreferences(this@Nyuka03_Barread)
                val lockTimeMinutes =
                    sharedPreferences.getString("lock_time", "5")?.toLongOrNull() ?: 5
                val lockTimeMillis = lockTimeMinutes * 60 * 1000

                // 現在の日時と最大時間の差分が設定した時間を超えている場合
                if (currentDate.time - maxDate.time >= lockTimeMillis) {
                    // データベースの全アイテムを削除
                    dao.deleteAllItems()
                    // メインスレッドでダイアログを表示
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@Nyuka03_Barread)
                            .setTitle("注意")
                            .setMessage("経過時間$lockTimeMinutes 分。全ての作業を取り消しました。メインメニューに戻ります。")
                            .setPositiveButton("OK") { _, _ ->
                                // メインメニューに遷移
                                val intent = Intent(this@Nyuka03_Barread, Main_Menu::class.java)
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