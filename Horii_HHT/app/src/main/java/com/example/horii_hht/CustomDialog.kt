package com.example.horii_hht

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.example.horii_hht.databinding.LayoutCustomDialogBinding


class CustomDialog : DialogFragment() {
    private var title: String = ""
    private var message: String = ""
    private var positiveButtonText: String = ""
    private var negativeButtonText: String = ""

    companion object {
        private const val DEFAULT_POSITIVE_BUTTON_TEXT = "OK"
        private const val DEFAULT_NEGATIVE_BUTTON_TEXT = "キャンセル"
        private const val TITLE_KEY = "TitleKey"
        private const val MESSAGE_KEY = "MessageKey"
        private const val REQUEST_POSITIVE_BUTTON_KEY = "RequestPositiveButtonKey"
        private const val REQUEST_NEGATIVE_BUTTON_KEY = "RequestNegativeButtonKey"
        private const val POSITIVE_BUTTON_TEXT_KEY = "PositiveButtonTextKey"
        private const val NEGATIVE_BUTTON_TEXT_KEY = "NegativeButtonTextKey"
    }

    class Builder {
        private val fragment: Fragment?
        private val activity: AppCompatActivity?
        private val bundle = Bundle()

        constructor(fragment: Fragment) {
            this.fragment = fragment
            this.activity = null
        }

        constructor(activity: AppCompatActivity) {
            this.fragment = null
            this.activity = activity
        }

        fun setTitle(title: String): Builder {
            return this.apply {
                bundle.putString(TITLE_KEY, title)
            }
        }

        fun setMessage(message: String): Builder {
            return this.apply {
                bundle.putString(MESSAGE_KEY, message)
            }
        }

        fun setPositiveButton(buttonText: String, listener: (() -> Unit)? = null): Builder {
            if (fragment != null) {
                fragment.childFragmentManager
                    .setFragmentResultListener(
                        REQUEST_POSITIVE_BUTTON_KEY,
                        fragment.viewLifecycleOwner
                    ) { _, _ ->
                        listener?.invoke()
                    }
            } else if (activity != null) {
                activity.supportFragmentManager
                    .setFragmentResultListener(
                        REQUEST_POSITIVE_BUTTON_KEY,
                        activity
                    ) { _, _ ->
                        listener?.invoke()
                    }
            }
            return this.apply {
                bundle.putString(POSITIVE_BUTTON_TEXT_KEY, buttonText)
            }
        }

        fun setNegativeButton(buttonText: String, listener: (() -> Unit)? = null): Builder {
            if (fragment != null) {
                fragment.childFragmentManager
                    .setFragmentResultListener(
                        REQUEST_NEGATIVE_BUTTON_KEY,
                        fragment.viewLifecycleOwner
                    ) { _, _ ->
                        listener?.invoke()
                    }
            } else if (activity != null) {
                activity.supportFragmentManager
                    .setFragmentResultListener(
                        REQUEST_NEGATIVE_BUTTON_KEY,
                        activity
                    ) { _, _ ->
                        listener?.invoke()
                    }
            }
            return this.apply {
                bundle.putString(NEGATIVE_BUTTON_TEXT_KEY, buttonText)
            }
        }

        fun build(): CustomDialog {
            return CustomDialog().apply {
                arguments = bundle
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        // ダイアログの背景を透過にする
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        arguments?.let {
            title = it.getString(TITLE_KEY, "")
            message = it.getString(MESSAGE_KEY, "")
            positiveButtonText =
                it.getString(POSITIVE_BUTTON_TEXT_KEY, DEFAULT_POSITIVE_BUTTON_TEXT)
            negativeButtonText =
                it.getString(NEGATIVE_BUTTON_TEXT_KEY, DEFAULT_NEGATIVE_BUTTON_TEXT)
        }
        val binding = LayoutCustomDialogBinding.inflate(requireActivity().layoutInflater)
        binding.title.text = title
        binding.message.text = message
        binding.positiveButton.text = positiveButtonText
        binding.negativeButton.text = negativeButtonText
        binding.positiveButton.setOnClickListener {
            dismiss()
            setFragmentResult(
                REQUEST_POSITIVE_BUTTON_KEY,
                bundleOf()
            )
        }
        binding.negativeButton.setOnClickListener {
            dismiss()
            setFragmentResult(
                REQUEST_NEGATIVE_BUTTON_KEY,
                bundleOf()
            )
        }
        // ポジティブボタンのフォーカスを可能にする
        binding.positiveButton.isFocusable = true
        // タッチモードでもポジティブボタンのフォーカスを可能にする
        binding.positiveButton.isFocusableInTouchMode = true
        // ポジティブボタンの初期のテキストカラーを設定
        binding.positiveButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.purple_200
            )
        )
        // ポジティブボタンを初期フォーカスに設定
        binding.positiveButton.requestFocus()

        // ネガティブボタンにフォーカスを設定
        binding.negativeButton.isFocusable = true
        // タッチモードでもネガティブボタンのフォーカスを可能にする
        binding.negativeButton.isFocusableInTouchMode = true

        // ダイアログにレイアウトを設定
        dialog.setContentView(binding.root)
        // ダイアログ外をタップしても終了しないように設定
        dialog.setCanceledOnTouchOutside(false)

        dialog.setContentView(binding.root)
        // キーリスナーを設定
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // 十字キーの左ボタンが押された場合
                        if (binding.positiveButton.isFocused) {
                            // ポジティブボタンがフォーカスされている場合、ネガティブボタンにフォーカスを移動
                            binding.negativeButton.requestFocus()
                            // フォーカス当たってるテキストにテキストカラーを紫に変更
                            binding.negativeButton.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.purple_200
                                )
                            )
                            // フォーカスが外れたテキストにテキストカラーをグレーに変更
                            binding.positiveButton.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.gray
                                )
                            )
                            return@setOnKeyListener true
                        }
                    }
                    // 十字キーの右ボタンが押された場合
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (binding.negativeButton.isFocused) {
                            // ネガティブボタンがフォーカスされている場合、ポジティブボタンにフォーカスを移動
                            binding.positiveButton.requestFocus()
                            // フォーカス当たってるテキストにテキストカラーを紫に変更
                            binding.positiveButton.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.purple_200
                                )
                            )
                            // フォーカスが外れたテキストにテキストカラーをグレーに変更
                            binding.negativeButton.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.gray
                                )
                            )
                            return@setOnKeyListener true
                        }
                    }
                }
            }
            false
        }

        return dialog
    }


}