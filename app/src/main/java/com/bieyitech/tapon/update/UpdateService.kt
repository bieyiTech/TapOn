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
import com.bieyitech.tapon.R
import com.bieyitech.tapon.helpers.printLog
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * 下载新Apk的后台服务
 */
class UpdateService : Service() {

    companion object {
        private const val CHANNEL_TAG = "下载应用更新提醒"
        private const val EXTRA_UPDATE_INFO = "UpdateInfo"
        private const val notificationId = 1

        fun newIntent(context: Context, updateVersionInfo: UpdateVersionInfo) = Intent(context, UpdateService::class.java).apply {
            putExtra(EXTRA_UPDATE_INFO, updateVersionInfo)
        }
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var updateVersionInfo: UpdateVersionInfo
    private var updateThread: UpdateThread? = null

    override fun onCreate() {
        super.onCreate()

        // 变为前台服务，显示在通知栏上
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
            .setShowWhen(false)
            .setOngoing(true) // 常驻
            .setProgress(0, 0, true)
            .addAction(R.drawable.ic_arrow_back, "取消下载",
                PendingIntent.getActivity(this, 4, Intent(this,
                    CancelDownloadActivity::class.java), 0)
            )

        startForeground(notificationId, notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        printLog("onStartCommand：开启服务：$startId")
        if(intent != null && intent.hasExtra(EXTRA_UPDATE_INFO)){
            updateVersionInfo = intent.getSerializableExtra(EXTRA_UPDATE_INFO) as UpdateVersionInfo
            updateThread = UpdateThread().apply {
                start()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        printLog("onDestroy：销毁服务")
        updateThread?.interrupt() // 中断线程，置isInterrupted()为true
        super.onDestroy()
    }

    // 下载线程
    private inner class UpdateThread : Thread(){

        override fun run() {
            val apkFile = getDownloadApkFilePath(this@UpdateService, updateVersionInfo.versionName)

            try {
                if(apkFile.exists()){ // todo 因为无法判断大小，先暂时删除，后面在修改
                    apkFile.delete()
                }

                if(!apkFile.exists()) {
                    if(apkFile.createNewFile()){
                        // val url = URL(updateVersionInfo.apkUrl)
                        val url = URL("https://raw.githubusercontent.com/GizFei/RecordCell/master/assets/%E6%96%B9%E6%A0%BCv1.0.apk")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connect() // 连接

                        if(connection.responseCode == HttpsURLConnection.HTTP_OK) {
                            val totalSize = connection.contentLength
                            printLog("总大小：$totalSize")
                            val inputStream = BufferedInputStream(connection.inputStream)
                            val outputStream = apkFile.outputStream()

                            val byteArray = ByteArray(1024)
                            var len = inputStream.read(byteArray)
                            var downloadSize = 0
                            while (len > -1) {
                                outputStream.write(byteArray, 0, byteArray.size)

                                // 更新进度
                                downloadSize += len
                                val progress = downloadSize / totalSize.toFloat() * 100
                                notificationBuilder.setProgress(100, progress.toInt(), false)
                                notificationManager.notify(notificationId, notificationBuilder.build())

                                // 继续读取
                                len = inputStream.read(byteArray)

                                if(isInterrupted) { // 退出线程
                                    printLog("退出线程")
                                    connection.disconnect()
                                    inputStream.close()
                                    outputStream.close()
                                    // 取消通知
                                    notificationManager.cancel(notificationId)
                                    return
                                }
                            }

                            outputStream.flush()
                            inputStream.close()
                            outputStream.close()

                            printLog("下载完成")
                            stopForeground(true)
                            // 下载完成，更新Intent为点击下载
                            val installIntent = getInstallIntent(this@UpdateService, apkFile)
                            notificationBuilder = NotificationCompat.Builder(this@UpdateService, packageName)
                                .setTicker("下载更新")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("下载更新")
                                .setContentText("下载完成，点击安装")
                                .setAutoCancel(true)
                                .setProgress(0, 0, false)
                                .setContentIntent(
                                    PendingIntent.getActivity(this@UpdateService, 2, installIntent, 0)
                                )
                            notificationManager.notify(notificationId, notificationBuilder.build())
                        }
                    }
                }else{
                    val installIntent = getInstallIntent(this@UpdateService, apkFile)
                    notificationBuilder.setContentIntent(
                        PendingIntent.getActivity(this@UpdateService, 2, installIntent, 0)
                    ).setContentText("已下载，点击安装")
                        .setAutoCancel(true)
                    notificationManager.notify(notificationId, notificationBuilder.build())
                }
            }catch (e: Exception){
                printLog("下载错误：${e.message}")
                e.printStackTrace()
            }finally {
                // 关闭服务
                // this@UpdateService.stopSelf()
            }
        }
    }
}