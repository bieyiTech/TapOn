package com.bieyitech.tapon.ui.ar

import android.os.SystemClock
import androidx.core.util.Preconditions
import com.google.ar.core.Anchor
import com.google.ar.core.Session

/**
 * 处理云锚点相关逻辑的辅助类，相当于在现有的ARCore API顶层再加一层类回调函数的机制。
 */
class CloudAnchorManager {

    private var deadlineForMessageMillis = 0L

    private var session: Session? = null
    private val pendingHostAnchors = HashMap<Anchor?, CloudAnchorHostListener?>()
    private val pendingResolveAnchors = HashMap<Anchor?, CloudAnchorResolveListener?>()

    /**
     * 设置会话。因为在CloudAnchorManager创建时会话可能还不能用
     */
    @Synchronized fun setSession(session: Session?) { this.session = session }

    /**
     * 托管一个锚点，当返回结果时，调用[listener]
     */
    @Synchronized fun hostCloudAnchor(anchor: Anchor, listener: CloudAnchorHostListener?) {
        Preconditions.checkNotNull(session, "会话session不能为空")
        val newAnchor = session?.hostCloudAnchor(anchor)
        pendingHostAnchors[newAnchor] = listener
    }

    /**
     * 解析一个锚点，返回结果时调用[listener]
     */
    @Synchronized fun resolveCloudAnchor(anchorId: String,
                                         listener: CloudAnchorResolveListener,
                                         startTimeMillis: Long) {
        Preconditions.checkNotNull(session, "会话session不能为空")
        val newAnchor = session?.resolveCloudAnchor(anchorId)
        deadlineForMessageMillis = startTimeMillis + DURATION_FOR_NO_RESOLVE_RESULT_MS
        pendingResolveAnchors[newAnchor] = listener
    }

    /**
     * 在[Session.update]函数之后调用
     */
    @Synchronized fun onUpdate() {
        Preconditions.checkNotNull(session, "会话session不能为空")
        val hostIterator =  pendingHostAnchors.entries.iterator()
        while (hostIterator.hasNext()) {
            val entry: MutableMap.MutableEntry<Anchor?, CloudAnchorHostListener?> = hostIterator.next()
            entry.key?.let {
                if(isReturnableState(it.cloudAnchorState)){
                    val listener = entry.value
                    listener?.onCloudTaskComplete(it)
                    hostIterator.remove()
                }
            }
        }

        val resolveIterator = pendingResolveAnchors.entries.iterator()
        while (resolveIterator.hasNext()) {
            val entry: MutableMap.MutableEntry<Anchor?, CloudAnchorResolveListener?> = resolveIterator.next()
            val listener = entry.value
            entry.key?.let {
                if(isReturnableState(it.cloudAnchorState)){
                    listener?.onCloudTaskComplete(it)
                    resolveIterator.remove()
                }
                if(deadlineForMessageMillis > 0 && SystemClock.uptimeMillis() > deadlineForMessageMillis){
                    listener?.onShowResolveMessage() // 超时显示信息
                    deadlineForMessageMillis = 0
                }
            }
        }
    }

    @Synchronized fun clearListeners() {
        pendingHostAnchors.clear()
        deadlineForMessageMillis = 0
    }

    /** 处理托管操作结果的监听函数 */
    interface CloudAnchorHostListener {
        fun onCloudTaskComplete(anchor: Anchor)
    }

    /** 处理解析操作结果的监听函数 */
    interface CloudAnchorResolveListener {
        fun onCloudTaskComplete(anchor: Anchor)
        fun onShowResolveMessage()
    }

    companion object {
        private const val TAG = "CloudAnchorManager"
        private const val DURATION_FOR_NO_RESOLVE_RESULT_MS = 10000 // 等待解析时长：10s

        private fun isReturnableState(cloudState: Anchor.CloudAnchorState) = when(cloudState) {
            Anchor.CloudAnchorState.NONE, Anchor.CloudAnchorState.TASK_IN_PROGRESS -> false
            else -> true
        }
    }
}