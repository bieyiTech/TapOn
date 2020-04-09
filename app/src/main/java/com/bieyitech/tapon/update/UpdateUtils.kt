package com.bieyitech.tapon.update

import android.content.Context
import java.io.File

// 检查与更新Apk的工具包

// Github上保存最新版本信息的json文件
const val versionJsonUrl = "https://github.com/bieyiTech/TapOn/blob/v1.2.0/app/version.json"

fun getVersionCode(context: Context): Int {
    // 通过PackageManager获取当前应用的包信息
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionCode
}

fun getVersionName(context: Context): String = context.packageManager
    .getPackageInfo(context.packageName, 0).versionName

fun getDownloadApkFilePath(context: Context, versionName: String)
        = File(context.externalCacheDir, "TapOn-${versionName}.apk")