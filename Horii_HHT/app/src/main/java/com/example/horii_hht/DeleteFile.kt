package com.example.horii_hht

import android.app.Application
import android.util.Log
import java.io.File

// システム終了時にファイルを削除するためのカスタムアプリケーション
//エミュレーターでしか使えないらしい。実機は✖
class DeleteFile: Application() {
    private lateinit var tempFile: File

    override fun onCreate() {
        super.onCreate()
        // アプリ生成時処理
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
    override fun onTerminate() {
        super.onTerminate()
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