package com.bieyitech.tapon.update

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bieyitech.tapon.helpers.printLog
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * 检查版本更新的线程
 */
class CheckUpdateThread(private val context: Context) : Thread() {

    /**
     * 检测到新版本的更新回调，如果没有新版本或其他错误，返回的apkUrl为空
     */
    interface OnUpdateListener {
        fun onUpdate(updateVersionInfo: UpdateVersionInfo?)
    }

    var onUpdateListener: OnUpdateListener? = null

    override fun run() {
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(
            JsonObjectRequest(Request.Method.GET, versionJsonUrl, null,
                Response.Listener {

                    try {
                        val newVersionCode = it.getInt("versionCode")
                        if(newVersionCode > getVersionCode(context)){ // 更新
                            onUpdateListener?.onUpdate(Gson().fromJson(it.toString(), UpdateVersionInfo::class.java))
                        }else{
                            onUpdateListener?.onUpdate(null)
                        }
                    } catch (e: Exception) {
                        context.printLog("读取JsonObject错误")
                        onUpdateListener?.onUpdate(null)
                    }

                }, null)
        )
    }

}