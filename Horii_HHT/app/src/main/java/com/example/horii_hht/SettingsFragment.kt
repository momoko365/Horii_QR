package com.example.horii_hht



import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.example.horii_hht.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sharedpreferences, rootKey)

        val lockTimePreference: EditTextPreference? = findPreference("lock_time")
        lockTimePreference?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

        // 数字のみを入力可能にする
        lockTimePreference?.setOnBindEditTextListener { editText ->
           editText.inputType = InputType.TYPE_CLASS_NUMBER

        }
    }
}