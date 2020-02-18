package com.bieyitech.tapon.ui.ar

import android.content.Context
import android.util.Log
import androidx.core.util.Preconditions
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

/**
 * 管理与Firebase通信的辅助类
 */
class FirebaseManager(context: Context) {
    companion object {
        private const val TAG = ""

        // Firebase数据库中的节点名称
        private const val ROOT_FIREBASE_HOTSPOTS = "hotspot_list"
        private const val ROOT_LAST_ROOM_CODE = "last_room_code"

        // Firebase数据库中的一些通用键值
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ANCHOR_ID = "hosted_anchor_id"
        private const val KEY_TIMESTAMP = "updated_at_timestamp"
        private const val DISPLAY_NAME_VALUE = "Android EAP Sample"
    }

    /**
     * 新房间号监听器
     */
    interface RoomCodeListener {
        // 当有新房间号可用时调用
        fun onNewRoomCode(newRoomCode: Long)
        // 出错时调用
        fun onError(error: DatabaseError?)
    }

    /**
     * 新云锚点ID监听器
     */
    interface CloudAnchorIdListener {
        // 当有新的可用ID时调用
        fun onNewCloudAnchorId(cloudAnchorId: String)
    }

    private val app: FirebaseApp? = FirebaseApp.initializeApp(context) // 信息在google-services.json中
    private val hotspotListRef: DatabaseReference?
    private val roomCodeRef: DatabaseReference?

    private var currentRoomRef: DatabaseReference? = null
    private var currentRoomListener: ValueEventListener? = null

    init {
        if(app != null){
            val rootRef = FirebaseDatabase.getInstance(app).reference
            hotspotListRef = rootRef.child(ROOT_FIREBASE_HOTSPOTS)
            roomCodeRef = rootRef.child(ROOT_LAST_ROOM_CODE)
        }else {
            Log.d(TAG, "无法连接到Firebase数据库")
            hotspotListRef = null
            roomCodeRef = null
        }
    }

    /**
     * 从Firebase数据库中获得新的可用房间号，等于最新房间号+1
     */
    fun getNewRoomCode(listener: RoomCodeListener?) {
        Preconditions.checkNotNull(app, "Firebase App不能为空")
        Log.d(TAG, "正在获取房间号")
        roomCodeRef?.runTransaction(object : Transaction.Handler {
            override fun doTransaction(p0: MutableData): Transaction.Result {
                var nextCode = 1L
                val currentVal = p0.value
                if(currentVal != null) {
                    val lastCode = currentVal.toString().toLong()
                    nextCode = lastCode + 1
                }
                p0.value = nextCode
                return Transaction.success(p0)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if(!committed) {
                    listener?.onError(error)
                    return
                }
                val roomCode = currentData?.getValue(Long::class.java) ?: 1L
                listener?.onNewRoomCode(roomCode)
            }
        })
    }

    /**
     * 在给定的房间号中存储云锚点ID
     */
    fun storeAnchorIdRoom(roomCode: Long, cloudAnchorId: String) {
        Preconditions.checkNotNull(app, "Firebase App不能为空")
        val roomRef = hotspotListRef?.child(roomCode.toString())
        roomRef?.apply {
            child(KEY_DISPLAY_NAME).setValue(DISPLAY_NAME_VALUE)
            child(KEY_ANCHOR_ID).setValue(cloudAnchorId)
            child(KEY_TIMESTAMP).setValue(System.currentTimeMillis())
        }
    }

    /**
     * 注册房间监听器，当房间号对应的数据改变时，触发该监听器
     * 或者说，从roomCode获取anchorId
     */
    fun registerNewListenerForRoom(roomCode: Long, listener: CloudAnchorIdListener) {
        Preconditions.checkNotNull(app, "Firebase App不能为空")
        clearRoomListener()
        currentRoomRef = hotspotListRef?.child(roomCode.toString())
        currentRoomListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val valObj = dataSnapshot.child(KEY_ANCHOR_ID).value
                if(valObj != null) {
                    val anchorId = valObj.toString()
                    if(anchorId.isNotEmpty()){
                        listener.onNewCloudAnchorId(anchorId)
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.w(TAG, "Firebase操作被取消了。", p0.toException())
            }
        }.also {
            currentRoomRef?.addValueEventListener(it)
        }
    }

    // 清除房间监听器
    fun clearRoomListener() {
        if (currentRoomListener != null && currentRoomRef != null) {
            currentRoomRef!!.removeEventListener(currentRoomListener!!)
            currentRoomListener = null
            currentRoomRef = null
        }
    }
}