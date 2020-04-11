package com.bieyitech.tapon.update

import android.content.Context
import com.bieyitech.tapon.helpers.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TapOnUpdateListener(private val context: Context,
                          private val forceCheck: Boolean // 是否强制检查更新，即不管之前是否忽略——不是自动检测
): CheckUpdateThread.OnUpdateListener {

    override fun onUpdate(updateVersionInfo: UpdateVersionInfo?, error: String?) {
        if(forceCheck) {
            if(updateVersionInfo != null) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("更新信息-${updateVersionInfo.date}")
                    .setMessage(updateVersionInfo.updateInfo)
                    .setPositiveButton("下载更新"){_, _ ->
                        // 开启后台下载
                        context.showToast("后台开始下载")
                        context.startService(UpdateIntentService.newIntent(context, updateVersionInfo))
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }else {
                if(error != null) {
                    context.showToast(error)
                }
            }
        }else{
            if(updateVersionInfo != null && !isIgnoreVersionUpdate(context, updateVersionInfo.versionCode)){ // 判断是否忽略
                MaterialAlertDialogBuilder(context)
                    .setTitle("更新信息-${updateVersionInfo.date}")
                    .setMessage(updateVersionInfo.updateInfo)
                    .setPositiveButton("下载更新"){_, _ ->
                        // 开启后台下载
                        context.showToast("后台开始下载")
                        context.startService(UpdateIntentService.newIntent(context, updateVersionInfo))
                    }
                    .setNeutralButton("忽略本次更新"){_, _ ->
                        setIgnoreVersionUpdate(context, updateVersionInfo.versionCode)
                    }
                    .setNegativeButton("下次提醒", null)
                    .show()
            }
        }
    }

}