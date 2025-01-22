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
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.EditText
import androidx.preference.PreferenceManager
import com.example.horii_hht.CustomDialog
import com.example.horii_hht.DB.BarcodeDAO
import com.example.horii_hht.Main_Menu
import com.example.horii_hht.R
import com.example.horii_hht.setting.ScreenStateReceiver
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume


class Nyuka04_Num : AppCompatActivity() {
    private lateinit var db: AppDatabase // データベース
    lateinit var dao: ItemDAO // DAO
    lateinit var barcodedao: BarcodeDAO
    private var item: Item? = null // クラス変数として宣言
    private lateinit var caseNum: EditText // ケース数
    private lateinit var baraNum: EditText // バラ数
    private lateinit var scannedData: String // スキャンデータ
    private var caseNumValue: Int? = null // ケース数の入力値
    private var baraNumValue: Int? = null    // バラ数の入力値
    private lateinit var screenReceiver: ScreenStateReceiver // スクリーンのオンオフのBroadcastReceiver
    private val handler = Handler(Looper.getMainLooper()) // ハンドラ
    private val checkInterval: Long = 10000 // 10秒ごとにチェック
    private var isDialogShown = false // finishbtn()でダイアログが表示されたかどうかを示すフラグ

    private val checkRunnable = object : Runnable { // チェック用のRunnable
        override fun run() {
            resetDatabaseAndShowDialog() // データベースをリセットしてダイアログ表示
            handler.postDelayed(this, checkInterval) // 10秒後に再度実行
        }
    }

    // 外部ストレージへの書き込みパーミッションのリクエスト要求
    companion object {
        private const val REQUEST_WRITE_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka04)

        val back_btn = findViewById<Button>(R.id.textView3333)
        val kenpinstart_btn = findViewById<Button>(R.id.textView555)

        back_btn.setOnClickListener {
            lifecycleScope.launch {
                if (!checkSumsAndShowDialog()) {
                    val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            true
        }

        kenpinstart_btn.setOnClickListener {
            lifecycleScope.launch {
                if (!checkSumsAndShowDialog()) {
                    withContext(Dispatchers.IO) {
                        // zumi数をすべて0に戻し、timeをnullにする
                        dao.resetZumiAndTime()
                    }
                    // Nyuka02_KenpinStartへ遷移
                    val intent = Intent(this@Nyuka04_Num, Nyuka02_KenpinStart::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            true
        }

        // 定期的にresetDatabaseAndShowDialogを呼び出す
        handler.post(checkRunnable)
        // ScreenStateReceiverの初期化と登録
        screenReceiver = ScreenStateReceiver()
        // スクリーン用のインテントフィルター設定
        val screenfilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        // スクリーンのオンオフのBroadcastReceiverの登録
        registerReceiver(screenReceiver, screenfilter)

        // Intentで全画面からデータを取得
//        scannedData = intent.getStringExtra("barcode").toString()

        //検品番号テキスト
        val kenpinNo = findViewById<android.widget.TextView>(R.id.kenpinNo)
        //商品点数テキスト
//        val itemNum = findViewById<android.widget.TextView>(R.id.itemNum)
        //商品点数済み数テキスト
        val real_itemNum = findViewById<android.widget.TextView>(R.id.real_itemNum)
        //ケース数テキスト
//        val caseAll = findViewById<android.widget.TextView>(R.id.itemAll)
        //ケース済み数テキスト
        val casezumi = findViewById<android.widget.TextView>(R.id.real_itemAll)
        //バラ数テキスト
//        val baraAll = findViewById<android.widget.TextView>(R.id.barareal)
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

        caseNum.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                baraNum.requestFocus()
                true
            } else {
                false
            }
        }

        baraNum.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                caseNum.requestFocus()
                true
            } else {
                false
            }
        }

        // 次へボタンクリックした時の処理
        nextbtn.setOnClickListener {
            lifecycleScope.launch {
                // 非同期処理が完了するまで待つ
                nextBtnClick()
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
            barcodedao = db.barcodeDAO()
            scannedData = barcodedao.getBarcodeAll()?.code ?: ""
            // 商品名を取得
            val item = if (scannedData.length == 13) {
                dao.getCSVdata(scannedData)
            } else if (scannedData.length == 14) {
                dao.getCSVdata(scannedData)
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
//                itemNum.text = distinctItemCount.toString() // 商品点数を表示
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

//                real_itemNum.text = count.toString()// 商品点数済み数を表示
                real_itemNum.text = "$countFormat/$distinctItemCountFormat"// 商品点数済み数を表示
//                caseAll.text = caseTotal.toString() // ケース数を表示
                casezumi.text = "$caseFormat/$caseTotalFormat" // ケース済み数を表示
//                baraAll.text = baraTotal.toString() // バラ数を表示
                barazumi.text = "$baraFormat/$baraTotalFormat" // バラ済み数を表示

                itemName.text =
                    item?.firstOrNull()?.itemName ?: "商品が見つかりません" // 商品名をテキストビューに表示
            }
        }

        // 完了ボタンがクリックされた時の処理
        finishbtn.setOnClickListener {
            lifecycleScope.launch {
                // EditTextに何か入力されているかチェック
                val caseNumValue = caseNum.text.toString().toIntOrNull()
                val baraNumValue = baraNum.text.toString().toIntOrNull()

                if (caseNumValue != null || baraNumValue != null) {
                    // 入力がある場合はfinishbtn()を呼び出す
                    withContext(Dispatchers.IO) {
                        finishbtn()
                    }
                }

                // ダイアログが表示されていない場合のみ以下の処理を実行
                if (!isDialogShown) {
                    // finishbtn()の処理が終わったらareSumsEqualを呼び出して結果を確認
                    val areSumsEqual = withContext(Dispatchers.IO) {
                        dao.areSumsEqual()
                    }

                    if (areSumsEqual) {
                        // 結果がTrueならそのままCSV書き出しの処理
                        writeDataToCSV()
                    } else {
                        // 結果がFalseならダイアログを表示
                        withContext(Dispatchers.Main) {
                            CustomDialog.Builder(this@Nyuka04_Num)
                                .setTitle("確認")
                                .setMessage("検品終了していません。\n完了させますか？")
                                .setPositiveButton("いいえ") {
                                    // NOが選択されたらUIを更新してダイアログを閉じる
                                    val intent =
                                        Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .setNegativeButton("はい") {
                                    // YESが選択されたらCSV書き出しの処理
                                    writeDataToCSV()
                                }
                                .build()
                                .show(supportFragmentManager, CustomDialog::class.simpleName)
                            true
                        }
                    }
                }
            }
            true
        }

    }

    //外部ストレージへの書き込みパーミッションがすでに許可されているかどうかをチェックして必要に応じてリクエストする関数
    private fun requestWritePermissionAndWriteCSV() {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            // (1) パーミッションがすでに許可されている場合
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // CSV書き込み用の関数を実行
                writeDataToCSV()
            }
            //(2)パーミッションを許可するための説明が必要な場合
            shouldShowRequestPermissionRationale(permission) -> {
                // ユーザーに説明のためのトーストを表示
                Toast.makeText(
                    this,
                    "外部ストレージへの書き込みパーミッションが必要です。",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                // (3) それ以外（初めてパーミッションをリクエストする場合）
                requestPermissions(arrayOf(permission), REQUEST_WRITE_PERMISSION)
            }
        }
    }

    // ユーザーがパーミッションダイアログに応答した結果（許可か拒否）を受け取る関数
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
        // 確認ダイアログを表示
        CustomDialog.Builder(this)
            .setTitle("確認")
            .setMessage("CSVを出力します。")
            .setPositiveButton("はい") {
                // ユーザーがOKを選択した場合、CSV書き込み処理を実行
                lifecycleScope.launch {
                    val items = withContext(Dispatchers.IO) {
                        // データベースから全てのアイテムを取得
                        dao.getCSVdata()
                    }
                    items.forEach { item ->
                        Log.d(
                            "CSVData",
                            "Item: ${item.itemCD}, ${item.itemName}, ${item.totalCaseQ}, ${item.totalCasezumi}, ${item.totalBara}, ${item.totalBarazumi}, ${item.JAN}, ${item.ITF}, ${item.kenpinTime}, ${item.QRTime}"
                        )
                    }
                    // ファイル書き込み先のディレクトリを作成
                    val customDir = File("/storage/self/primary/horiitest")
                    if (!customDir.exists()) {
                        // ディレクトリが存在しない場合作成
                        val dirCreated = customDir.mkdirs()
                        if (!dirCreated) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@Nyuka04_Num,
                                    "ディレクトリの作成に失敗しました: ${customDir.absolutePath}",
                                    Toast.LENGTH_LONG
                                ).show()
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
                            CSVPrinter(
                                writer,
                                CSVFormat.DEFAULT.withHeader(
                                    "商品コード",
                                    "商品名",
                                    "ケース数",
                                    "ケース済数",
                                    "バラ数",
                                    "バラ済数",
                                    "JAN",
                                    "ITF",
                                    "最終検品時間",
                                    "QR読込時間"
                                )
                            ).use { csvPrinter ->
                                for (item in items) {
                                    csvPrinter.printRecord(
                                        item.itemCD,
                                        item.itemName,
                                        item.totalCaseQ,
                                        item.totalCasezumi,
                                        item.totalBara,
                                        item.totalBarazumi,
                                        item.JAN,
                                        item.ITF,
                                        item.kenpinTime,
                                        item.QRTime
                                    )
                                }
                            }
                        }
                        runOnUiThread {
                            Toast.makeText(
                                this@Nyuka04_Num,
                                "CSVファイル書き出しに成功しました",
                                Toast.LENGTH_LONG
                            ).show()
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
                            Toast.makeText(
                                this@Nyuka04_Num,
                                "CSVファイルの書き出しに失敗しました: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("いいえ")
            .build()
            .show(supportFragmentManager, CustomDialog::class.simpleName)
        true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // 次へ
            KeyEvent.KEYCODE_F1 -> {
                lifecycleScope.launch {
                    // 非同期処理が完了するまで待つ
                    nextBtnClick()
                }
                true
            }
            // F4：戻るキーが押されたとき前画面に戻る処理
            KeyEvent.KEYCODE_F4 -> {
                lifecycleScope.launch {
                    if (!checkSumsAndShowDialog()) {
                        val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                true
            }

            KeyEvent.KEYCODE_F7 -> {
                lifecycleScope.launch {
                    if (!checkSumsAndShowDialog()) {
                        withContext(Dispatchers.IO) {
                            // zumi数をすべて0に戻し、timeをnullにする
                            dao.resetZumiAndTime()
                        }
                        // Nyuka02_KenpinStartへ遷移
                        val intent = Intent(this@Nyuka04_Num, Nyuka02_KenpinStart::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                true
            }

            KeyEvent.KEYCODE_F8 -> {
                lifecycleScope.launch {
                    // EditTextに何か入力されているかチェック
                    val caseNumValue = caseNum.text.toString().toIntOrNull()
                    val baraNumValue = baraNum.text.toString().toIntOrNull()
                    if (caseNumValue != null || baraNumValue != null) {
                        // 入力がある場合はfinishbtn()を呼び出す
                        withContext(Dispatchers.IO) {
                            finishbtn()
                        }
                    }

                    // ダイアログが表示されていない場合のみ以下の処理を実行
                    if (!isDialogShown) {
                        // finishbtn()の処理が終わったらareSumsEqualを呼び出して結果を確認
                        val areSumsEqual = withContext(Dispatchers.IO) {
                            dao.areSumsEqual()
                        }

                        withContext(Dispatchers.Main) {
                            if (areSumsEqual) {
                                // 結果がTrueならそのままCSV書き出しの処理
                                writeDataToCSV()
                            } else {
                                // 結果がFalseならダイアログを表示
                                CustomDialog.Builder(this@Nyuka04_Num)
                                    .setTitle("確認")
                                    .setMessage("検品終了していません。\n完了させますか？")
                                    .setPositiveButton("いいえ") {
                                        // NOが選択されたらUIを更新してダイアログを閉じる
                                        val intent =
                                            Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .setNegativeButton("はい") {
                                        // YESが選択されたらCSV書き出しの処理
                                        writeDataToCSV()
                                    }
                                    .build()
                                    .show(supportFragmentManager, CustomDialog::class.simpleName)
                                true
                            }
                        }
                    }
                }
                true
            }

//            KeyEvent.KEYCODE_F6 -> {
//                lifecycleScope.launch {
//                    if (!checkSumsAndShowDialog()) {
//                        withContext(Dispatchers.IO) {
//                            // zumi数をすべて0に戻し、timeをnullにする
//                            dao.resetZumiAndTime()
//                        }
//                        // Nyuka02_KenpinStartへ遷移
//                        val intent = Intent(this@Nyuka04_Num, Nyuka02_KenpinStart::class.java)
//                        startActivity(intent)
//                        finish()
//                    }
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

    // 非同期処理を行う関数を suspend に変更
    private suspend fun nextBtnClick() {
        withContext(Dispatchers.IO) {
            var itemData = dao.getTotal(scannedData)
            val itemAll = dao.getItemAll()

            val item = itemData
            if (item != null) {
                if (item.bara == item.barazumi && item.case_q == item.casezumi) {
                    withContext(Dispatchers.Main) {
                        CustomDialog.Builder(this@Nyuka04_Num)
                            .setTitle("検品終了")
                            .setMessage("その商品は検品が終了しています。")
                            .setPositiveButton("OK")
                            .setNegativeButton("")
                            .build()
                            .show(supportFragmentManager, CustomDialog::class.simpleName)
                        true

                    }
                    return@withContext
                } else {
                    caseNumValue = caseNum.text.toString().toIntOrNull()
                    baraNumValue = baraNum.text.toString().toIntOrNull()

                    if (caseNumValue == null && baraNumValue == null) {
                        withContext(Dispatchers.Main) {
                            CustomDialog.Builder(this@Nyuka04_Num)
                                .setTitle("エラー")
                                .setMessage("ケース数かバラ数かいずれかに\n数字を入力してください。")
                                .setPositiveButton("OK")
                                .setNegativeButton("")
                                .build()
                                .show(supportFragmentManager, CustomDialog::class.simpleName)
                            true
                        }
                        return@withContext
                    } else {
                        if (caseNumValue != null) {
                            if (caseNumValue!! + item.casezumi > item.case_q) {
                                val excessCasezumi = caseNumValue!! + item.casezumi - item.case_q // 超過ケース数を計算
                                item.casezumi = item.case_q // ケース済み数を予定数量に設定
                                dao.update(item) // データベースを更新
                                withContext(Dispatchers.Main) {
                                updateUI() // UIを更新
                                caseNum.text.clear()

                                    CustomDialog.Builder(this@Nyuka04_Num)
                                        .setTitle("エラー") // ダイアログのタイトルを設定
                                        .setMessage("ケース数が超えています。\n超過ケース数: $excessCasezumi") // ダイアログのメッセージを設定
                                        .setPositiveButton("OK")

                                        .setNegativeButton("") // 否定ボタンを設定
                                        .build() // ダイアログを構築
                                        .show(supportFragmentManager, CustomDialog::class.simpleName) // ダイアログを表示
                                    true

                                }
                                return@withContext
                            } else {
                                item.casezumi = caseNumValue!! + item.casezumi // ケース済み数を更新
                            }
                        }


                        if (baraNumValue != null) {
                            if (baraNumValue!! + item.barazumi > item.bara) {
                                val excessBarazumi = baraNumValue!! + item.barazumi - item.bara // 超過バラ数を計算
                                item.barazumi = item.bara // バラ済み数を予定数量に設定
                                dao.update(item) // データベースを更新
                                withContext(Dispatchers.Main) {
                                updateUI() // UIを更新
                                baraNum.text.clear()

                                    CustomDialog.Builder(this@Nyuka04_Num)
                                        .setTitle("確認") // ダイアログのタイトルを設定
                                        .setMessage("バラ数が超えています。\n超過バラ数: $excessBarazumi") // ダイアログのメッセージを設定
                                        .setPositiveButton("OK") {
                                            val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                                            startActivity(intent)
                                            finish()
                                        }// OKボタンを設定
                                        .setNegativeButton("") // 否定ボタンを設定
                                        .build() // ダイアログを構築
                                        .show(supportFragmentManager, CustomDialog::class.simpleName) // ダイアログを表示
                                    true
                                }
                                return@withContext
                            } else {
                                item.barazumi = baraNumValue!! + item.barazumi // バラ済み数を更新
                            }
                        }


                        val currentDate = Date()
                        val dateFormat =
                            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                        val formattedDate = dateFormat.format(currentDate)

                        item.kenpinTime = formattedDate
                        dao.update(item)
                        lifecycleScope.launch {
                            // 非同期処理が完了してダイアログが表示されなかった場合のみ 画面遷移 を実行、UIをもう一度更新
                            val shouldProceed = !checkSumsAndShowDialog()
                            //UIをもう一度更新
                            updateUI()
                            baraNum.text.clear()
                            caseNum.text.clear()
                            if (shouldProceed) {
                                withContext(Dispatchers.Main) {
                                    val intent =
                                        Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                        true
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    CustomDialog.Builder(this@Nyuka04_Num)
                        .setTitle("検品終了")
                        .setMessage("検品終了しています。\nF8を押して検品を完了させてください。")
                        .setPositiveButton("OK")
                        .setNegativeButton("")
                        .build()
                        .show(supportFragmentManager, CustomDialog::class.simpleName)
                    true
                }
            }
        }
    }


    // F8押して、エディットテキストに入力値があった時に動く関数
    private suspend fun finishbtn() {
        withContext(Dispatchers.IO) {
            // スキャンデータに基づいてアイテムデータを取得
            // itemData=合計値
            val itemData = dao.getTotal(scannedData)
            val itemAll = dao.getItemAll()

            // 入力値を取得
            val caseNumValue = caseNum.text.toString().toIntOrNull()
            val baraNumValue = baraNum.text.toString().toIntOrNull()

            // アイテムデータが存在する場合
            if (itemData != null) {
                // 既存のケース数とバラ数を取得
                val existingCasezumi = itemData.casezumi
                val existingBarazumi = itemData.barazumi

                // 入力値が存在する場合、それぞれの済み数に加算
                val newCasezumi = existingCasezumi + (caseNumValue ?: 0)
                val newBarazumi = existingBarazumi + (baraNumValue ?: 0)

                // 予定数量を超えないかチェック
                if (newCasezumi <= itemData.case_q && newBarazumi <= itemData.bara) {
                    // 超えない場合、データベースをアップデート
                    itemData.casezumi = newCasezumi
                    itemData.barazumi = newBarazumi
                    dao.update(itemData)
                    updateUI()
                    withContext(Dispatchers.Main) {
                        caseNum.text.clear()
                        baraNum.text.clear()
                    }


                } else {
                    // 超える場合、超えない分だけアップデートし、超過分をダイアログで表示
                    val excessCasezumi = if (newCasezumi > itemData.case_q) newCasezumi - itemData.case_q else 0 // 超過ケース数を計算
                    val excessBarazumi = if (newBarazumi > itemData.bara) newBarazumi - itemData.bara else 0 // 超過バラ数を計算

                    itemData.casezumi = minOf(newCasezumi, itemData.case_q) // 超えない分のケース済み数を設定
                    itemData.barazumi = minOf(newBarazumi, itemData.bara) // 超えない分のバラ済み数を設定
                    dao.update(itemData) // データベースを更新
                    updateUI() // UIを更新

                    isDialogShown = true // ダイアログが表示されたことを示すフラグを設定
                    withContext(Dispatchers.Main) {
                        CustomDialog.Builder(this@Nyuka04_Num)
                            .setTitle("確認") // ダイアログのタイトルを設定
                            .setMessage("入力値が予定数量を超えています。\n超過ケース数: $excessCasezumi\n超過バラ数: $excessBarazumi") // ダイアログのメッセージを設定
                            .setPositiveButton("OK") {
                                caseNum.text.clear() // ケース数入力欄をクリア
                                baraNum.text.clear() // バラ数入力欄をクリア
                                isDialogShown = false // ダイアログが閉じられたらフラグをリセット
                            }
                            .setNegativeButton("") // 否定ボタンを設定
                            .build() // ダイアログを構築
                            .show(supportFragmentManager, CustomDialog::class.simpleName) // ダイアログを表示
                    }
                }
            } else {
                // アイテムデータが存在しない場合、エラーメッセージを表示
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Nyuka04_Num, "アイテムが見つかりません。", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

//    suspend fun showAlertDialog(title: String, message: String) {
//        suspendCancellableCoroutine<Unit> { continuation ->
//            CustomDialog.Builder(this)
//                .setTitle("作業中")
//                .setMessage("作業中です")
//                .setPositiveButton("OK")
//                .setNegativeButton("")
//                .build()
//                .show(supportFragmentManager, CustomDialog::class.simpleName)
//            true
//            AlertDialog.Builder(this@Nyuka04_Num)
//                .setTitle(title)
//                .setMessage(message)
//                .setPositiveButton("OK") { _, _ ->
//                    continuation.resume(Unit) // ダイアログが閉じられたら処理を再開
//                }
//                .setCancelable(false)
//                .show()
//        }
//    }


    override fun onDestroy() {
        super.onDestroy()
        // ハンドラのコールバックを削除
        handler.removeCallbacks(checkRunnable)
        // スクリーンのオンオフのBroadcastReceiverの解除
        unregisterReceiver(screenReceiver)
    }    // ダイアログを表示するメソッド

    fun showWorkingDialog() {
        CustomDialog.Builder(this)
            .setTitle("作業中")
            .setMessage("作業中です")
            .setPositiveButton("OK")
            .setNegativeButton("")
            .build()
            .show(supportFragmentManager, CustomDialog::class.simpleName)
        true
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
                    PreferenceManager.getDefaultSharedPreferences(this@Nyuka04_Num)
                val lockTimeMinutes =
                    sharedPreferences.getString("lock_time", "5")?.toLongOrNull() ?: 5
                val lockTimeMillis = lockTimeMinutes * 60 * 1000

                // 現在の日時と最大時間の差分が設定した時間を超えている場合
                if (currentDate.time - maxDate.time >= lockTimeMillis) {
                    // データベースの全アイテムを削除
                    dao.deleteAllItems()
                    // メインスレッドでダイアログを表示
                    withContext(Dispatchers.Main) {
                        CustomDialog.Builder(this@Nyuka04_Num)
                            .setTitle("注意")
                            .setMessage("経過時間$lockTimeMinutes 分。\n全ての作業を取り消しました。\nメインメニューに戻ります。")
                            .setPositiveButton("OK") {
                                // メインメニューに遷移
                                val intent = Intent(this@Nyuka04_Num, Main_Menu::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .setNegativeButton("")
                            .build()
                            .show(supportFragmentManager, CustomDialog::class.simpleName)
                        true
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // インスタンスを取得
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("AppState", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("lastActivity", this::class.java.simpleName)
            .apply()
//        val editor = sharedPreferences.edit()
//        editor.putString("lastActivity", this::class.java.simpleName)
//        editor.putString("scannedData", scannedData) // scannedDataを保存
//        Log.d("AppState", "scannedData: $scannedData") // 保存する前に値を確認
//
//        editor.commit()
//        // 保存後に確認のためログを追加
//        Log.d("AppState", "scannedData saved successfully")
    }

    private suspend fun checkSumsAndShowDialog(): Boolean {
        val areSumsEqual = withContext(Dispatchers.IO) {
            dao.areSumsEqual()
        }
        if (areSumsEqual) {
            withContext(Dispatchers.Main) {
                CustomDialog.Builder(this@Nyuka04_Num)
                    .setTitle("検品終了")
                    .setMessage("検品終了してます。\nF8を押して検品を完了させてください。")
                    .setPositiveButton("OK")
                    .setNegativeButton("")
                    .build()
                    .show(supportFragmentManager, CustomDialog::class.simpleName)
                true
            }
            return true  // ダイアログ表示した場合
        }
        return false  // ダイアログを表示しなかった場合
    }


    // UIを更新する関数
    private fun updateUI() {
        // UI更新の具体的な処理をここに記述
        //商品点数数テキスト
        val real_itemNum = findViewById<android.widget.TextView>(R.id.real_itemNum)
        //ケース数テキスト
        val casezumi = findViewById<android.widget.TextView>(R.id.real_itemAll)
        //バラ数テキスト
        val barazumi = findViewById<android.widget.TextView>(R.id.barazumi)

        lifecycleScope.launch(Dispatchers.IO) {
            val distinctItemCount = dao.getDistinctItemCount() // 商品点数
            val itemCheck = dao.getCSVdata() //商品点数済み数
            val caseTotal = dao.getTotalCase() // ケース数
            val casezumiTotal = dao.getCasezumi() // ケース済み数
            val baraTotal = dao.getTotalBara() // バラ数
            val barazumiTotal = dao.getBarazumi() // バラ済み数

            withContext(Dispatchers.Main) {
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
                casezumi.text = "$caseFormat/$caseTotalFormat" //ケース数済み数
                barazumi.text = "$baraFormat/$baraTotalFormat" //バラ数済み数
            }
        }
    }

}

