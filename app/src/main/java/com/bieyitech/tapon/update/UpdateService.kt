package com.bieyitech.tapon.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bieyitech.tapon.MainActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.helpers.printLog
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * 下载新Apk的后台服务
 */
class UpdateService : Service() {

    companion object {
        private const val CHANNEL_TAG = "下载应用更新提醒"
        private const val EXTRA_UPDATE_INFO = "UpdateInfo"

        fun newIntent(context: Context, updateVersionInfo: UpdateVersionInfo) = Intent(context, UpdateService::class.java).apply {
            putExtra(EXTRA_UPDATE_INFO, updateVersionInfo)
        }
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var updateVersionInfo: UpdateVersionInfo

    override fun onCreate() {
        super.onCreate()
        // 变为前台服务，显示在通知栏上
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        notificationManager = NotificationManagerCompat.from(this)
        // 兼容O以上版本
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 添加一个通知频道
            if(notificationManager.getNotificationChannel(packageName) == null){
                val channel = NotificationChannel(packageName, CHANNEL_TAG, NotificationManager.IMPORTANCE_DEFAULT)
                channel.setShowBadge(true)
                notificationManager.createNotificationChannel(channel)
            }
        }

        // 创建一个通知
        notificationBuilder = NotificationCompat.Builder(this, packageName)
            .setTicker("下载更新")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("下载更新")
            .setContentText("正在下载TapOn应用新版本")
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null && intent.hasExtra(EXTRA_UPDATE_INFO)){
            updateVersionInfo = intent.getSerializableExtra(EXTRA_UPDATE_INFO) as UpdateVersionInfo
            UpdateThread().start()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 下载线程
    private inner class UpdateThread : Thread(){

        override fun run() {
            val apkFile = getDownloadApkFilePath(this@UpdateService, updateVersionInfo.versionName)

            try {
                if(!apkFile.exists()) {
                    if(apkFile.createNewFile()){
                        val url = URL(updateVersionInfo.apkUrl)
                        val connection = url.openConnection() as HttpsURLConnection
                        connection.connect() // 连接

                        if(connection.responseCode == HttpsURLConnection.HTTP_OK) {
                            val totalSize = connection.contentLength
                            val inputStream = connection.inputStream
                            val outputStream = apkFile.outputStream()

                            val byteArray = ByteArray(512)
                            var len = inputStream.read(byteArray)
                            var downloadSize = 0
                            while (len > -1) {
                                outputStream.write(byteArray, 0, byteArray.size)

                                // 更新进度
                                downloadSize += len
                                val progress = downloadSize / totalSize.toFloat() * 100
                                notificationBuilder.setProgress(100, progress.toInt(), false)
                                notificationManager.notify(1, notificationBuilder.build())

                                // 继续读取
                                len = inputStream.read(byteArray)
                            }

                            inputStream.close()
                            outputStream.close()

                            // 下载完成，更新Intent为点击下载
                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                            }
                            notificationBuilder.setContentIntent(
                                PendingIntent.getActivity(this@UpdateService, 2, installIntent, 0)
                            ).setContentText("下载完成，点击安装")
                                .setAutoCancel(true)
                                .setProgress(100, 100, false)
                            notificationManager.notify(1, notificationBuilder.build())
                        }
                    }
                }else{
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                    }
                    notificationBuilder.setContentIntent(
                        PendingIntent.getActivity(this@UpdateService, 2, installIntent, 0)
                    ).setContentText("已下载，点击安装")
                        .setAutoCancel(true)
                    notificationManager.notify(1, notificationBuilder.build())

                    // 关闭服务
                    this@UpdateService.stopSelf()
                }
            }catch (e: Exception){
                printLog("下载错误：${e.message}")
            }
        }
    }
}