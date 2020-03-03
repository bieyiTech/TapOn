package com.bieyitech.tapon.helpers

import android.content.Context
import android.net.ConnectivityManager

object NetworkUtils {

    /**
     * 网络是否连通
     * @param context 上下文
     */
    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // val networkInfo = connectivityManager.getNetworkInfo()
        return false
    }
}