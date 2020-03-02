package com.bieyitech.tapon.widgets

import android.content.Context
import android.text.InputFilter
import android.widget.EditText
import android.widget.FrameLayout
import com.bieyitech.tapon.helpers.hideSoftInputKeyboard
import com.giz.android.toolkit.dp2pxSize
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 输入框
 */
class InputTextDialog private constructor(context: Context,
                                          title: String,
                                          maxLen: Int,
                                          listener: (String) -> Unit){

    private val dialog: MaterialAlertDialogBuilder

    init {
        val et = EditText(context).apply {
            if(maxLen != -1){
                filters = arrayOf(InputFilter.LengthFilter(maxLen))
            }
            requestFocus()
        }
        val wrapLayout = FrameLayout(context).apply {
            setPadding(dp2pxSize(context, 20f), dp2pxSize(context, 8f), dp2pxSize(context, 20f), 0)
            addView(et)
        }
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(wrapLayout)
            .setNegativeButton(android.R.string.cancel,  null)
            .setPositiveButton(android.R.string.ok) {_, _ ->
                context.hideSoftInputKeyboard(et)
                listener(et.text.toString())
            }
    }

    private fun show() {
        dialog.show()
    }

    class Builder(private val context: Context) {

        private var title: String = ""
        private var maxLen: Int = -1
        private var textInputedListener: (String) -> Unit = {}

        fun title(t: String) = this.apply {
            title = t
        }

        fun maxLength(len: Int) = this.apply {
            maxLen = len
        }

        fun onTextInputed(listener: (String) -> Unit) = this.apply {
            textInputedListener = listener
        }

        fun build() = InputTextDialog(context, title, maxLen, textInputedListener)

        fun show() = build().show()
    }

}