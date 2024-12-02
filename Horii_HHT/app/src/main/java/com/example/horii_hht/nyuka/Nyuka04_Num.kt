package com.example.horii_hht.nyuka

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.Item
import com.example.horii_hht.DB.ItemDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter
import android.Manifest
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.horii_hht.Main_Menu
import com.example.horii_hht.R
import com.example.horii_hht.setting.ScreenStateReceiver

import java.io.IOException

class Nyuka04_Num : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private var item: Item? = null // クラス変数として宣言
    private lateinit var caseNum: EditText
    private lateinit var baraNum: EditText
    private lateinit var scannedData: String
    private var caseNumValue: Int? = null
    private var baraNumValue: Int? = null
    private lateinit var screenReceiver: ScreenStateReceiver

    // 外部ストレージへの書き込みパーミッションのリクエスト要求
    companion object {
        private const val REQUEST_WRITE_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka04)

        // ScreenStateReceiverの初期化と登録
        screenReceiver = ScreenStateReceiver()
        val screenfilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenfilter)

        // Intentで全画面からデータを取得
        scannedData = intent.getStringExtra("barcode").toString()

        //検品番号テキスト
        val kenpinNo = findViewById<android.widget.TextView>(R.id.kenpinNo)
        //商品点数テキスト
        val itemNum = findViewById<android.widget.TextView>(R.id.itemNum)
        //商品点数済み数テキスト
        val real_itemNum = findViewById<android.widget.TextView>(R.id.real_itemNum)
        //ケース数テキスト
        val caseAll = findViewById<android.widget.TextView>(R.id.itemAll)
        //ケース済み数テキスト
        val casezumi = findViewById<android.widget.TextView>(R.id.real_itemAll)
        //バラ数テキスト
        val baraAll = findViewById<android.widget.TextView>(R.id.barareal)
        //バラ済み数テキスト
        val barazumi = findViewById<android.widget.TextView>(R.id.barazumi)

        //商品名テキスト
        val itemName = findViewById<android.widget.TextView>(R.id.itemname)
        //ケース数入力
        caseNum = findViewById<android.widget.EditText>(R.id.barcode)
        //バラ数入力
        baraNum = findViewById<android.widget.EditText>(R.id.baraNum)
        //次へボタン
        val nextbtn = findViewById<Button>(R.id.startbtn)
        //完了ボタン
        val finishbtn = findViewById<Button>(R.id.button2)

        // 次へボタンクリックした時の処理
        nextbtn.setOnClickListener {
            lifecycleScope.launch {
                // 非同期処理が完了するまで待つ
                nextBtnClick()
                // 非同期処理が完了した後に画面遷移
                val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                startActivity(intent)
                finish()
            }
        }

        // データベースの初期化
        lifecycleScope.launch(Dispatchers.IO) {
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()
            dao = db.itemDAO()
            // 商品名を取得
            val item = if (scannedData != null && scannedData.length == 13) {
                dao.getItemCode(jan = scannedData, itf = "")
            } else if (scannedData != null && scannedData.length == 14) {
                dao.getItemCode(jan = "", itf = scannedData)
            } else {
                null
            }
            // データベースからデータを取得
            val kenpinNoValue = dao.getKenpinNo() // 検品番号
            val distinctItemCount = dao.getDistinctItemCount() // 商品点数
            val itemCheck = dao.getCSVdata() //商品点数済み数
            val caseTotal = dao.getTotalCase() // ケース数
            val casezumiTotal = dao.getCasezumi() // ケース済み数
            val baraTotal = dao.getTotalBara() // バラ数
            val barazumiTotal = dao.getBarazumi() // バラ済み数

            //UIの更新処理
            launch(Dispatchers.Main) {
                kenpinNo.text = kenpinNoValue // 検品番号を表示
                itemNum.text = distinctItemCount.toString() // 商品点数を表示
                var count = 0
                for (i in itemCheck) {
                    if (i.totalBara == barazumiTotal && i.totalCasezumi == casezumiTotal) {
                        count++
                    }
                }
                real_itemNum.text = count.toString()// 商品点数済み数を表示
                caseAll.text = caseTotal.toString() // ケース数を表示
                casezumi.text = casezumiTotal.toString() // ケース済み数を表示
                baraAll.text = baraTotal.toString() // バラ数を表示
                barazumi.text = barazumiTotal.toString() // バラ済み数を表示
                itemName.text = item?.itemName ?: "商品が見つかりません" // 商品名をテキストビューに表示
            }
        }

        // 完了ボタンがクリックされた時の処理
        finishbtn.setOnClickListener{
            //外部ストレージへの書き込みパーミッションがすでに許可されているかどうかをチェックして必要に応じてリクエストする関数
            requestWritePermissionAndWriteCSV()
            //メインメニューに遷移
//            val intent = Intent(this, Main_Menu::class.java)
//            startActivity(intent)
        }

    }

    //外部ストレージへの書き込みパーミッションがすでに許可されているかどうかをチェックして必要に応じてリクエストする関数
    private fun requestWritePermissionAndWriteCSV() {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            // (1) パーミッションがすでに許可されている場合
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
               // CSV書き込み用の関数を実行
                writeDataToCSV()
            }
            //(2)パーミッションを許可するための説明が必要な場合
            shouldShowRequestPermissionRationale(permission) -> {
                // ユーザーに説明のためのトーストを表示
                Toast.makeText(this, "外部ストレージへの書き込みパーミッションが必要です。", Toast.LENGTH_LONG).show()
            }
            else -> {
                // (3) それ以外（初めてパーミッションをリクエストする場合）
                requestPermissions(arrayOf(permission), REQUEST_WRITE_PERMISSION)
            }
        }
    }

    // ユーザーがパーミッションダイアログに応答した結果（許可か拒否）を受け取る関数
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // パーミッションが許可されたらCSV書き込み用の関数を実行
                writeDataToCSV()
            } else {
                // パーミッションが拒否されたらトーストを表示
                Toast.makeText(this, "パーミッションが拒否されました。", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun writeDataToCSV() {
        // ケース数の入力処理
        val caseNumValue = caseNum.text.toString().toIntOrNull()
        // バラ数の入力処理
        val baraNumValue = baraNum.text.toString().toIntOrNull()

        // エディットテキストに入力があるかどうかをチェック
        if (caseNumValue != null || baraNumValue != null) {
            // 入力がある場合はfinishbtn()を呼び出す
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    finishbtn()
                }
            }
        }

        // 確認ダイアログを表示
        AlertDialog.Builder(this)
            .setTitle("確認")
            .setMessage("CSVを出力します。")
            .setPositiveButton("OK") { dialog, which ->
                // ユーザーがOKを選択した場合、CSV書き込み処理を実行
                lifecycleScope.launch {
                    val items = withContext(Dispatchers.IO) {
                        // データベースから全てのアイテムを取得
                        dao.getCSVdata()
                    }
                    // ファイル書き込み先のディレクトリを作成
                    val customDir = File("/storage/self/primary/horiitest")
                    if (!customDir.exists()) {
                        // ディレクトリが存在しない場合作成
                        val dirCreated = customDir.mkdirs()
                        if (!dirCreated) {
                            runOnUiThread {
                                Toast.makeText(this@Nyuka04_Num, "ディレクトリの作成に失敗しました: ${customDir.absolutePath}", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }
                    }
                    val kenpinNo = withContext(Dispatchers.IO) {
                        // 検品番号を取得
                        dao.getKenpinNo()
                    }
                    // CSVファイルを作成
                    val csvFile = File(customDir, "$kenpinNo.csv")
                    try {
                        // CSVファイルにデータを書き込む
                        FileWriter(csvFile).use { writer ->  //useはwriterオブジェクト使用後クローズ処理を自動で行う
                            CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("商品コード", "商品名", "ケース数", "ケース済数", "バラ数", "バラ済数", "JAN", "ITF")).use { csvPrinter ->
                                for (item in items) {
                                    csvPrinter.printRecord(item.itemCD, item.itemName, item.totalCaseQ, item.totalCasezumi, item.totalBara, item.totalBarazumi, item.JAN, item.ITF)
                                }
                            }
                        }
                        runOnUiThread {
                            Toast.makeText(this@Nyuka04_Num, "CSVファイル書き出しに成功しました", Toast.LENGTH_LONG).show()
                        }
                        withContext(Dispatchers.IO) {
                            // データベースのアイテムを全削除
                            dao.deleteAllItems()
                        }
                        val intent = Intent(this@Nyuka04_Num, Main_Menu::class.java)
                        startActivity(intent)
                        finish()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        runOnUiThread {
                            Toast.makeText(this@Nyuka04_Num, "CSVファイルの書き出しに失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> {
                lifecycleScope.launch {
                    // 非同期処理が完了するまで待つ
                    nextBtnClick()
                    // 非同期処理が完了した後に画面遷移
                    val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                    startActivity(intent)
                    finish()
                }
                true
            }
            // F4キーが押されたとき前画面に戻る処理
            KeyEvent.KEYCODE_F4 -> {
                val intent = Intent(this, Nyuka03_Barread::class.java)
                startActivity(intent)
                finish()
                true
            }
            KeyEvent.KEYCODE_F7 -> {
                // F7キーが押されたときの処理
                val intent = Intent(this, Nyuka02_KenpinStart::class.java)
                startActivity(intent)
                finish()
                true
            }
            KeyEvent.KEYCODE_F8 -> {
                //外部ストレージへの書き込みパーミッションがすでに許可されているかどうかをチェックして必要に応じてリクエストする関数
                requestWritePermissionAndWriteCSV()
                //メインメニューに遷移
//            val intent = Intent(this, Main_Menu::class.java)
//            startActivity(intent)

            true
            }
            KeyEvent.KEYCODE_F6 -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        // zumi数をすべて0に戻し、timeをnullにする
                        dao.resetZumiAndTime()
                    }
                    // Nyuka02_KenpinStartへ遷移
                    val intent = Intent(this@Nyuka04_Num, Nyuka02_KenpinStart::class.java)
                    startActivity(intent)
                    finish()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    // 非同期処理を行う関数を suspend に変更
    private suspend fun nextBtnClick() {
        withContext(Dispatchers.IO) {
            val itemData = dao.getSummarizedData(scannedData)
            val itemAll = dao.getItemAll()
            val item = scannedData?.let { it1 -> dao.getItemCode(jan = it1, itf = "") }

            // 検品中のリストが検品終了しているかどうかを判定
            withContext(Dispatchers.Main) {
                if (itemData.totalCaseQ == itemData.totalCasezumi && itemData.totalBara == itemData.totalBarazumi) {
                    showAlertDialog("検品終了", "その商品は検品終了してます")
                    return@withContext
                } else {
                    // ケース数の入力処理
                    caseNumValue = caseNum.text.toString().toIntOrNull()
                    // バラ数の入力処理
                    baraNumValue = baraNum.text.toString().toIntOrNull()
                    // 入力値がnullの場合にダイアログを表示
                    if (caseNumValue == null && baraNumValue == null) {
                        showAlertDialog("エラー", "数字を入力してください")
                        return@withContext
                    }
                    // 入力値がnullじゃないかつデータベースのアイテムがnullじゃない場合
                    if (caseNumValue != null) {
                        // ケース数がケース数合計を超えている場合
                        if (caseNumValue!! + itemData.totalCasezumi > itemData.totalCaseQ) {
                            withContext(Dispatchers.Main) {
                                Log.d("ActivityState", "isFinishing: $isFinishing, isDestroyed: $isDestroyed")
                                showAlertDialog("エラー", "ケース数が超えています")
                            }
                            return@withContext
                        } else {
                            if (item != null) {
                                item.casezumi = caseNumValue!! + itemData.totalCasezumi
                            }
                        }
                    }
                    // バラ数の入力処理
                    if (baraNumValue != null) {
                        // バラ数がバラ数合計を超えている場合
                        if (baraNumValue!! + itemData.totalBarazumi > itemData.totalBara) {
                            withContext(Dispatchers.Main) {
                                showAlertDialog("エラー", "数量が超えています")
                            }
                            return@withContext
                        } else {
                            if (item != null) {
                                item.barazumi = baraNumValue!! + itemData.totalBarazumi
                            }
                        }
                    }
                    // データベースの更新
                    withContext(Dispatchers.IO) {
                        if (item != null) {
                            dao.update(item)
                        }
                    }
                    val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private suspend fun finishbtn() {
        withContext(Dispatchers.IO) {
            val itemData = dao.getSummarizedData(scannedData)
            val itemAll = dao.getItemAll()
            val item = scannedData?.let { it1 -> dao.getItemCode(jan = it1, itf = "") }

            // 検品中のリストが検品終了しているかどうかを判定
            withContext(Dispatchers.Main) {
                if (itemData.totalCaseQ == itemData.totalCasezumi && itemData.totalBara == itemData.totalBarazumi) {
                    showAlertDialog("検品終了", "その商品は検品終了してます")
                    return@withContext
                } else {
                    // ケース数の入力処理
                    caseNumValue = caseNum.text.toString().toIntOrNull()
                    // バラ数の入力処理
                    baraNumValue = baraNum.text.toString().toIntOrNull()
                    // 入力値がnullの場合にダイアログを表示
                    if (caseNumValue == null && baraNumValue == null) {
                        showAlertDialog("エラー", "数字を入力してください")
                        return@withContext
                    }
                    // 入力値がnullじゃないかつデータベースのアイテムがnullじゃない場合
                    if (caseNumValue != null) {
                        // ケース数がケース数合計を超えている場合
                        if (caseNumValue!! + itemData.totalCasezumi > itemData.totalCaseQ) {
                            withContext(Dispatchers.Main) {
                                Log.d("ActivityState", "isFinishing: $isFinishing, isDestroyed: $isDestroyed")
                                showAlertDialog("エラー", "ケース数が超えています")
                            }
                            return@withContext
                        } else {
                            if (item != null) {
                                item.casezumi = caseNumValue!! + itemData.totalCasezumi
                            }
                        }
                    }
                    // バラ数の入力処理
                    if (baraNumValue != null) {
                        // バラ数がバラ数合計を超えている場合
                        if (baraNumValue!! + itemData.totalBarazumi > itemData.totalBara) {
                            withContext(Dispatchers.Main) {
                                showAlertDialog("エラー", "数量が超えています")
                            }
                            return@withContext
                        } else {
                            if (item != null) {
                                item.barazumi = baraNumValue!! + itemData.totalBarazumi
                            }
                        }
                    }
                    // データベースの更新
                    withContext(Dispatchers.IO) {
                        if (item != null) {
                            dao.update(item)
                        }
                    }
                }
            }
        }
    }

    private fun showAlertDialog(title: String, message: String) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(screenReceiver)
    }    // ダイアログを表示するメソッド
    fun showWorkingDialog() {
        AlertDialog.Builder(this)
            .setTitle("作業中")
            .setMessage("作業中です")
            .setPositiveButton("OK", null)
            .show()
    }

    //指定した時間分スクリーンオフにしてたら起動するメソッド
    fun resetDatabaseAndShowDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            dao.deleteAllItems()
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@Nyuka04_Num)
                    .setTitle("注意")
                    .setMessage("全ての作業を取り消しました。メインメニューに戻ります。")
                    .setPositiveButton("OK") { _, _ ->
                        val intent = Intent(this@Nyuka04_Num, Main_Menu::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .show()
            }
        }
    }
    }

