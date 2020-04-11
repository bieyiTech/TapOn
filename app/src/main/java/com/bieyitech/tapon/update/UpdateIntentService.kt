package com.bieyitech.tapon.update

import android.app.DownloadManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment

class UpdateIntentService : IntentService("DownloadApk") {

    companion object{
        private const val EXTRA_UPDATE_INFO = "UpdateInfo"

        fun newIntent(context: Context, updateVersionInfo: UpdateVersionInfo) = Intent(context,
            UpdateIntentService::class.java).apply {
                putExtra(EXTRA_UPDATE_INFO, updateVersionInfo)
            }
    }

    override fun onHandleIntent(intent: Intent?) {
        // 开始下载
        if(intent != null){
            val updateVersionInfo = intent.getSerializableExtra(EXTRA_UPDATE_INFO) as UpdateVersionInfo
            // 获取DownloadManager服务
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            // 新建请求，并配置
            val url = updateVersionInfo.apkUrl
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setDestinationInExternalFilesDir(this@UpdateIntentService,
                    Environment.DIRECTORY_DOWNLOADS, "TapOn-v${updateVersionInfo.versionName}.apk") // 文件路径
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI) // 允许Wifi下载
                // 定制Notification
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setTitle("TapOn更新")
                setDescription("下载TapOn应用更新")
                // 设置文件类型
                setMimeType("application/vnd.android.package-archive")
            }
            // 添加到下载队列
            downloadManager.enqueue(request)
        }
    }
}