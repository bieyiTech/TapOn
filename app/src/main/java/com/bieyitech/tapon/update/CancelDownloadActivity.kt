package com.bieyitech.tapon.update

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bieyitech.tapon.helpers.printLog

class CancelDownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printLog("取消下载Activity")
        stopService(Intent(this.applicationContext, UpdateService::class.java))
        finish()
    }
}