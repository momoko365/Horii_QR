package com.example.horii_hht

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
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
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
import android.widget.EditText

import java.io.IOException

class Nyuka04_Num : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private var item: Item? = null // クラス変数として宣言
    private lateinit var caseNum: EditText
    private lateinit var baraNum: EditText
    companion object {
        private const val REQUEST_WRITE_PERMISSION = 100
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka04)

        // Intentで全画面からデータを取得
        val scannedData = intent.getStringExtra("SCANNED_DATA")

        //商品総数テキスト
        val itemAll = findViewById<android.widget.TextView>(R.id.itemAll)
        //商品総数済み数テキスト
        val real_itemAll = findViewById<android.widget.TextView>(R.id.real_itemAll)
        //商品点数テキスト
        val itemNum = findViewById<android.widget.TextView>(R.id.itemNum)
        //商品点数済み数テキスト
        val real_itemNum = findViewById<android.widget.TextView>(R.id.real_itemNum)
        //検品番号テキスト
        val kenpinNo = findViewById<android.widget.TextView>(R.id.kenpinNo)
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

        nextbtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                item = scannedData?.let { it1 -> dao.getItemCode(jan = it1, itf = "") }
                // ケース数の入力処理
                val caseNumValue = caseNum.text.toString().toIntOrNull()
                if (caseNumValue != null && item != null) {
                    val totalCaseNum = caseNumValue * item!!.in_q + item!!.zumi
                    // `zumi` に登録する処理をここに追加
                    dao.update(item!!.copy(zumi = totalCaseNum))
                }
                // バラ数の入力処理
                val baraNumValue = baraNum.text.toString().toIntOrNull()
                if (baraNumValue != null) {
                    // `zumi` に登録する処理をここに追加
                    item?.let { dao.update(it.copy(zumi = baraNumValue + it.zumi)) }
                }
                // メインスレッドで画面遷移を行う
                launch(Dispatchers.Main) {
                    val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                    startActivity(intent)
                }
            }
        }

        finishbtn.setOnClickListener{
            requestWritePermissionAndWriteCSV()
            val intent = Intent(this, Main_Menu::class.java)
            startActivity(intent)
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
            val distinctItemCount = dao.getDistinctItemCount()
            val totalSuryo = dao.getTotalSuryo()
            val kenpinNoValue = dao.getKenpinNo()
            val getCountOfZumiItems = dao.getCountOfZumiItems()
            val getTotalZumiCount = dao.getTotalZumiCount()


            launch (Dispatchers.Main){
                real_itemNum.text = getCountOfZumiItems.toString()
                itemNum.text = distinctItemCount.toString()
                real_itemAll.text = getTotalZumiCount.toString()
                itemAll.text = totalSuryo.toString()
                kenpinNo.text = kenpinNoValue
                // 商品名をテキストビューに表示
                itemName.text = item?.itemName ?: "商品が見つかりません"

            }
        }
    }

    private fun requestWritePermissionAndWriteCSV() {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                writeDataToCSV()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "外部ストレージへの書き込みパーミッションが必要です。", Toast.LENGTH_LONG).show()
            }
            else -> {
                requestPermissions(arrayOf(permission), REQUEST_WRITE_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                writeDataToCSV()
            } else {
                Toast.makeText(this, "パーミッションが拒否されました。", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun writeDataToCSV() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                dao.getItemAll()
            }
            val customDir = File("/storage/self/primary/horiitest")
            if (!customDir.exists()) {
                val dirCreated = customDir.mkdirs()
                if (!dirCreated) {
                    runOnUiThread {
                        Toast.makeText(this@Nyuka04_Num, "ディレクトリの作成に失敗しました: ${customDir.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
            }
            val csvFile = File(customDir, "app_database.csv")
            try {
                FileWriter(csvFile).use { writer ->
                    CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(*Item::class.java.declaredFields.map { it.name }.toTypedArray())).use { csvPrinter ->
                        for (item in items) {
                            csvPrinter.printRecord(item.itemCD, item.itemName, item.suryo, item.in_q, item.case_q, item.JAN, item.ITF, item.zumi)
                        }
                    }
                }
                runOnUiThread {
                    Toast.makeText(this@Nyuka04_Num, "CSVファイルに書き出しました: ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@Nyuka04_Num, "CSVファイルの書き出しに失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // F4キーが押されたとき前画面に戻る処理
            KeyEvent.KEYCODE_F4 -> {
                val intent = Intent(this, Nyuka03_Barread::class.java)
                startActivity(intent)
                true
            }
            KeyEvent.KEYCODE_F7 -> {
                caseNum.setText("")
baraNum.setText("")
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }


    }
}