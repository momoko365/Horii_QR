package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.Item
import com.example.horii_hht.DB.ItemDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Nyuka03_Barread: AppCompatActivity() {
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private var data: String? = null
    private lateinit var barcodedata: EditText
    private lateinit var tyudanbtn: Button
    //画面ロックのフラグ
    private var isLocked: Boolean = false
    //スキャンデータのソースを識別するためのフラグを設定
    private var isScanner = false
    // itemsをクラス変数として定義
    private var items: List<Item> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka03)

        // ReaderManagerの初期化
        readerManager = ReaderManager.InitInstance(this)
        barcodedata = findViewById(R.id.barcode)

        //ｹｰｽ数テキスト
        val caseAll = findViewById<TextView>(R.id.itemAll)
        //ケース数済み数テキスト
        val casezumiAll = findViewById<TextView>(R.id.real_itemAll)
        //バラ数テキスト
        val baraAll = findViewById<TextView>(R.id.baraall)
        //バラ数済み数テキスト
        val barazumiAll = findViewById<TextView>(R.id.barazumi)

        //商品点数テキスト
        val itemNum = findViewById<TextView>(R.id.itemNum)
        //商品点数済み数テキスト
        val real_itemNum = findViewById<TextView>(R.id.real_itemNum)
        //検品番号テキスト
        val kenpinNo = findViewById<TextView>(R.id.kenpinNo)

        //作業中断ボタン
        tyudanbtn = findViewById<Button>(R.id.startbtn)

        // 作業中断ボタンのクリックリスナー
        tyudanbtn.setOnClickListener {
            if (isLocked) {
                // 画面ロック解除
                unlockScreen()
                tyudanbtn.text = "作業中断"
                tyudanbtn.setTextColor(Color.RED)

            } else {
                // 画面ロック
                lockScreen()
                tyudanbtn.text = "作業再開"
                tyudanbtn.setTextColor(Color.BLUE)
                disableEditText()
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

            // データベースからデータを取得
            val kenpinNoValue = dao.getKenpinNo() //検品番号
            val distinctItemCount = dao.getDistinctItemCount() //商品点数
//            val getCountOfZumiItems = dao.getCountOfMatchedItems() //商品点数済み数
            val caseTotal = dao.getTotalCase() //ケース数
            val casezumiTotal = dao.getCasezumi() //ケース数済み数
            val baraTotal = dao.getTotalBara() //バラ数
            val barazumiTotal = dao.getBarazumi() //バラ数済み数


            launch (Dispatchers.Main){
                kenpinNo.text = kenpinNoValue //検品番号
                itemNum.text = distinctItemCount.toString() //商品点数
//                real_itemNum.text = getCountOfZumiItems.toString() //商品点数済み数
                caseAll.text = caseTotal.toString() //ケース数
                casezumiAll.text = casezumiTotal.toString() //ケース数済み数
                baraAll.text = baraTotal.toString() //バラ数
                barazumiAll.text = barazumiTotal.toString() //バラ数済み数



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
    // ハードウェアスキャン用の BroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        //スキャン受信
        override fun onReceive(context: Context, intent: Intent) {
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
                    //有効なデータだった場合検索実行
                    searchBarcodeAndNavigate(data!!)
                } else {
                    showAlertDialog("エラー", "コードが不正です")
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
            KeyEvent.KEYCODE_F4 -> {
                // F4キーが押されたときの処理
                val intent = Intent(this, Main_Menu::class.java)
                startActivity(intent)
                true
            }
            KeyEvent.KEYCODE_F1 -> {
                //barcodedata.text=キーボードからの入力、空だったらスキャンデータを使う
                val barcodeInput = barcodedata.text.toString().ifEmpty { data }
                //バーコードが入力されているかチェック

                if (barcodeInput != null) {
                    if (barcodeInput.isNotEmpty()) {
                        barcodeInput?.let { searchBarcodeAndNavigate(it) }
                        val intent = Intent(this, Nyuka04_Num::class.java)
                        intent.putExtra("barcode", barcodeInput)
                        startActivity(intent)
                        finish()
                    } else {
                        showAlertDialog("エラー", "有効なJANまたはITFコードを入力してください")

                    }
                }
                true
            }

            KeyEvent.KEYCODE_F7 -> {
                // F7キーが押されたときの処理
                val intent = Intent(this, Nyuka02_KenpinStart::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


    //画面ロック
    private fun lockScreen() {
        // ボタン以外すべてのビューを無効にする（画面ロック）
        findViewById<View>(R.id.itemAll).isEnabled = false
        findViewById<View>(R.id.real_itemAll).isEnabled = false
        findViewById<View>(R.id.itemNum).isEnabled = false
        findViewById<View>(R.id.real_itemNum).isEnabled = false
        findViewById<View>(R.id.kenpinNo).isEnabled = false

        findViewById<EditText>(R.id.barcode).isEnabled = false
        // ボタンは無効化しない
        tyudanbtn.isEnabled = true
    }

    //画面ロック解除
    private fun unlockScreen() {
        // ボタン以外のすべてのビューを有効にする例
        findViewById<View>(R.id.itemAll).isEnabled = true
        findViewById<View>(R.id.real_itemAll).isEnabled = true
        findViewById<View>(R.id.itemNum).isEnabled = true
        findViewById<View>(R.id.real_itemNum).isEnabled = true
        findViewById<View>(R.id.kenpinNo).isEnabled = true

        findViewById<EditText>(R.id.barcode).isEnabled = true
        // ボタンは引き続き有効
        tyudanbtn.isEnabled = true
    }

    // Activity破棄される時に呼び出されるライフサイクルメソッド
    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
        // ReaderManagerの解放
        readerManager?.Release()
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
}