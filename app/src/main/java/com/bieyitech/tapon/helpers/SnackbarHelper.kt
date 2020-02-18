package com.bieyitech.tapon.helpers

import android.app.Activity
import android.widget.TextView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/**
 * 显示Snackbar的助手类，隐藏Android样板代码，更方便地显示信息
 */
class SnackbarHelper {
    private val BACKGROUND_COLOR = 0xbf323232.toInt()
    private var messageSnackbar: Snackbar? = null
    private enum class DismissBehavior { HIDE, SHOW, FINISH }
    private var maxLines = 2
    private var lastMessage = ""

    fun isShowing() = messageSnackbar != null

    fun showMassage(activity: Activity, message: String) {
        if(message.isNotEmpty() && (!isShowing() || lastMessage != message)){
            lastMessage = message
            show(activity, message, DismissBehavior.HIDE)
        }
    }

    // 显示带有取消按钮的Snackbar
    fun showMessageWithDismiss(activity: Activity, message: String) {
        show(activity, message, DismissBehavior.SHOW)
    }

    // 显示错误信息
    fun showError(activity: Activity, message: String) {
        show(activity, message, DismissBehavior.FINISH)
    }

    fun hide(activity: Activity) {
        if(!isShowing()){
            return
        }
        lastMessage = ""
        val messageSnackbarToHide = messageSnackbar
        messageSnackbar = null
        activity.runOnUiThread {
            messageSnackbarToHide?.dismiss()
        }
    }

    fun setMaxLines(lines: Int) { maxLines = lines }

    private fun show(activity: Activity, message: String, dismissBehavior: DismissBehavior) {
        activity.runOnUiThread {
            messageSnackbar = Snackbar.make(activity.findViewById(android.R.id.content),
                message,
                Snackbar.LENGTH_INDEFINITE).apply {
                view.setBackgroundColor(BACKGROUND_COLOR)
                if(dismissBehavior != DismissBehavior.HIDE) {
                    setAction("关闭") { dismiss() }
                    if(dismissBehavior == DismissBehavior.FINISH){
                        addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                activity.finish()
                            }
                        })
                    }
                }
                view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines = maxLines
            }.also { it.show() }
        }
    }
}