package com.bieyitech.tapon.ui.ar

import android.util.Log
import com.bieyitech.tapon.helpers.showToast
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.BaseArFragment
import java.util.*

class CloudAnchorArFragment : BaseArFragment() {
    companion object {
        private const val TAG = "CloudAnchorArFragment"
    }

    private var onUpdateListener: OnUpdateListener? = null

    // 打开AR
    override fun isArRequired() = true

    override fun getAdditionalPermissions(): Array<String> = emptyArray()

    // 打开云锚点模式
    override fun getSessionConfiguration(session: Session?): Config {
        val config = Config(session)
        config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
        return config
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {

    }

    override fun getSessionFeatures(): MutableSet<Session.Feature> = Collections.emptySet<Session.Feature>()

    // 异常处理
    override fun handleSessionException(sessionException: UnavailableException?) {
        val message = when(sessionException) {
            is UnavailableArcoreNotInstalledException -> "请安装ARCore服务"
            is UnavailableApkTooOldException -> "请更新ARCore服务"
            is UnavailableSdkTooOldException -> "请更新应用"
            is UnavailableDeviceNotCompatibleException -> "设备不支持AR"
            else -> "创建AR会话失败"
        }
        Log.e(TAG, "错误：$message", sessionException)
        requireActivity().showToast(message)
    }

    override fun onUpdate(frameTime: FrameTime?) {
        super.onUpdate(frameTime)
        onUpdateListener?.onUpdate(frameTime)
    }

    interface OnUpdateListener {
        fun onUpdate(frameTime: FrameTime?)
    }

    fun setOnUpdateListener(onUpdateListener: OnUpdateListener?){
        this.onUpdateListener = onUpdateListener
    }
}