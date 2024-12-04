package com.example.horii_hht.setting


import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.horii_hht.R

//設定画面のフラグメント
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sharedpreferences, rootKey)

        // "lock_time"でEditTextPreferenceを取得
        val lockTimePreference: EditTextPreference? = findPreference("lock_time")
        lockTimePreference?.summaryProvider =
            Preference.SummaryProvider<EditTextPreference> { preference ->
                val text = preference.text
                if (text.isNullOrBlank()) {
                    "未設定"
                } else {
                    "現在の設定: \n $text 分"
                }
            }

// 数字のみを入力可能にする
        lockTimePreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
}