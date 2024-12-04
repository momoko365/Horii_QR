// SettingsActivity.kt
package com.example.horii_hht.setting

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.horii_hht.Main_Menu
import com.example.horii_hht.R

//設定画面のアクティビティ
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        //SettingsFragmentを設定コンテナに置き換える
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //F4キーが押された場合はメインメニュー画面に遷移
        if (keyCode == KeyEvent.KEYCODE_F4) {
            val intent = Intent(this, Main_Menu::class.java)
            startActivity(intent)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

