package com.bieyitech.tapon.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.bieyitech.tapon.BuildConfig
import java.io.File

// 检查与更新Apk的工具包

// Github上保存最新版本信息的json文件
const val versionJsonUrl = "https://raw.githubusercontent.com/bieyiTech/TapOn/master/attachments/version.json"

// 获得应用当前的版本号
fun getVersionCode(context: Context): Int {
    // 通过PackageManager获取当前应用的包信息
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionCode
}

fun getVersionName(context: Context): String = context.packageManager
    .getPackageInfo(context.packageName, 0).versionName

// 保存下载Apk文件的地址：/storage/emulated/0/Android/data/com.bieyitech.tapon/cache/TapOn-v1.0.0.apk
fun getDownloadApkFilePath(context: Context, versionName: String)
        = File(context.externalCacheDir, "TapOn-${versionName}.apk")

// 获得安装应用的Intent
fun getInstallIntent(context: Context, apkFile: File) = Intent(Intent.ACTION_VIEW).apply {
    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION // 授予临时权限
    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.FileProvider", apkFile)
    setDataAndType(uri, "application/vnd.android.package-archive")
}

private const val SP_VERSION_UPDATE = "VersionUpdateSp"
private const val SP_IS_IGNORED = "IsIgnored"
private const val SP_IGNORED_VERSION = "IgnoredVersion"
fun setIgnoreVersionUpdate(context: Context, versionCode: Int) {
    context.getSharedPreferences(SP_VERSION_UPDATE, Context.MODE_PRIVATE).edit()
        .putBoolean(SP_IS_IGNORED, true)
        .putInt(SP_IGNORED_VERSION, versionCode)
        .apply()

}
fun isIgnoreVersionUpdate(context: Context, versionCode: Int): Boolean
        = context.getSharedPreferences(SP_VERSION_UPDATE, Context.MODE_PRIVATE).run {
    val version = getInt(SP_IGNORED_VERSION, 1)
    val isIgnored = getBoolean(SP_IS_IGNORED, false)
    versionCode == version && isIgnored
}