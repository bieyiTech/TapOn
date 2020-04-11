package com.bieyitech.tapon.update

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bieyitech.tapon.helpers.printLog
import com.google.gson.Gson

/**
 * 检查版本更新的线程
 */
class CheckUpdateThread(private val context: Context) : Thread() {

    /**
     * 检测到新版本的更新回调，如果没有新版本或其他错误，返回的apkUrl为空
     */
    interface OnUpdateListener {
        fun onUpdate(updateVersionInfo: UpdateVersionInfo?, error: String?)
    }

    var onUpdateListener: OnUpdateListener? = null

    override fun run() {
        context.printLog("检查更新")
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(
            JsonObjectRequest(Request.Method.GET, versionJsonUrl, null,
                Response.Listener {
                    try {
                        val newVersionCode = it.getInt("versionCode")
                        if(newVersionCode > getVersionCode(context)){ // 更新
                            onUpdateListener?.onUpdate(Gson().fromJson(it.toString(), UpdateVersionInfo::class.java), null)
                        }else{
                            onUpdateListener?.onUpdate(null, "已是最新版本")
                        }
                    } catch (e: Exception) {
                        context.printLog("读取JsonObject错误")
                        onUpdateListener?.onUpdate(null, "读取JsonObject错误")
                    }

                }, Response.ErrorListener {
                    context.printLog("检查更新错误：${it.message}")
                    // it.printStackTrace()
                    onUpdateListener?.onUpdate(null, "网络错误")
                })
        )
    }

}