/**
 * @author GizFei
 */
package com.bieyitech.tapon

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.ImageView
import androidx.annotation.GuardedBy
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Preconditions
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.bieyitech.tapon.bmob.RewardObject
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.bmob.TaponUser
import com.bieyitech.tapon.helpers.SnackbarHelper
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.ui.ar.CloudAnchorArFragment
import com.bieyitech.tapon.ui.ar.CloudAnchorManager
import com.bieyitech.tapon.ui.ar.FirebaseManager
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.AnimationData
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable

class FindTreasureBoxActivity : AppCompatActivity() {

    companion object {
        private const val MIN_OPENGL_VERSION = 3.0
        private const val EXTRA_STORE_OBJECT = "StoreObject"

        fun newIntent(context: Context, storeObject: StoreObject) = Intent(context, FindTreasureBoxActivity::class.java)
            .apply {
                putExtra(EXTRA_STORE_OBJECT, storeObject)
            }
    }

    // 状态
    private enum class HostResolveMode { NONE, RESOLVING }

    // AR相关的Fragment，会话
    private var cloudAnchorArFragment: CloudAnchorArFragment? = null
    private var session: Session? = null

    // 锚点及节点（与模型链接）
    private val anchorLock = Object()
    @GuardedBy("anchorLock")
    private var anchor: Anchor? = null
    private var anchorNode: AnchorNode? = null
    private var treasurBoxCanOpenNode: Node? = null
    // 可渲染物体（3D模型）
    private var boxOpenRenderable: ModelRenderable? = null
    private var boxOpenTextRenderable: ViewRenderable? = null
    private var isBoxOpened = false

    // 云锚点相关组件
    private var firebaseManager: FirebaseManager? = null
    private val cloudManager = CloudAnchorManager()
    private var currentMode: HostResolveMode = HostResolveMode.NONE

    // 底部通知条，商铺奖品代码
    private val snackbarHelper = SnackbarHelper()
    private var objectCode = StoreObject.INVALID_OBJECT_CODE
    private lateinit var storeObject: StoreObject


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!checkIsSupportedDeviceOrFinish(this)){
            return
        }
        setContentView(R.layout.activity_find_treasure_box)

        // 获取商铺代码
        storeObject = intent.getSerializableExtra(EXTRA_STORE_OBJECT) as StoreObject
        objectCode = storeObject.objectCode
        if(objectCode == StoreObject.INVALID_OBJECT_CODE) {
            showToast("无效奖品代码")
            finish()
        }

        // 初始化云锚点相关变量
        firebaseManager = FirebaseManager(this)

        load3DObject()
        setupArFragment()
        startFindBox(objectCode.toLong())
    }

    /**
     * 加载3D模型和场景中的2D视图
     */
    private fun load3DObject() {
        // 加载含有打开动画的宝箱3D模型
        ModelRenderable.builder()
            .setSource(this, R.raw.treasurebox_open2)
            .build()
            .thenAccept {
                boxOpenRenderable = it
            }
            .exceptionally {
                showToast("无法加载3D模型文件")
                null
            }

        // 打开宝箱后显示的优惠券视图
        ViewRenderable.builder()
            .setView(this, R.layout.info_find_box)
            .build()
            .thenAccept {
                boxOpenTextRenderable = it
                boxOpenTextRenderable?.view?.
                    findViewById<ImageView>(R.id.info_treasure_img)?.apply {
                    setOnClickListener {
                        this.animate().rotation(360f).start()
                    }
                }
            }
            .exceptionally {
                showToast("无法加载视图可渲染文件")
                null
            }
    }

    /**
     * 初始化ArFragment
     */
    private fun setupArFragment() {
        cloudAnchorArFragment = supportFragmentManager.findFragmentById(R.id.find_box_ar_fragment) as CloudAnchorArFragment
        cloudAnchorArFragment?.setOnSessionInitializationListener { session ->
            // 为CloudAnchorManager设置会话
            cloudManager.setSession(session)
            this.session = session
        }
        cloudAnchorArFragment?.setOnUpdateListener(object : CloudAnchorArFragment.OnUpdateListener {
            override fun onUpdate(frameTime: FrameTime?) {
                // Log.d(TAG, "update Frame: ${frameTime?.deltaSeconds}")
                cloudManager.onUpdate()
            }
        })
    }

    /**
     * 开始寻找宝箱
     */
    private fun startFindBox(storeCode: Long) {
        currentMode = HostResolveMode.RESOLVING
        snackbarHelper.showMessageWithDismiss(this, "正在寻找宝箱，点击‘取消’退出")

        // 注册房间号监听器
        firebaseManager?.registerNewListenerForRoom(storeCode, object : FirebaseManager.CloudAnchorIdListener {
            override fun onNewCloudAnchorId(cloudAnchorId: String) {
                val resolveListener = CloudAnchorResolveStateListener(storeCode) {
                    // 找到宝箱锚点
                    setNewAnchor(it)
                    createOpenTreasureBox()
                }
                Preconditions.checkNotNull(resolveListener, "解析监听器不能为空。")
                cloudManager.resolveCloudAnchor(cloudAnchorId, resolveListener, SystemClock.uptimeMillis())
            }
        })
    }

//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
//    }

    /**
     * 设置新的锚点，解除旧锚点
     */
    private fun setNewAnchor(newAnchor: Anchor?) {
        synchronized(anchorLock) {
            if (anchor != null && anchor != newAnchor) {
                anchor?.detach()
            }
            printLog("set new anchor: pose: ${newAnchor?.pose}")
            anchor = newAnchor
        }
    }

    // 在锚点处创建一个可以打开的宝箱
    private fun createOpenTreasureBox() {
        if(anchor == null){
            return
        }
        anchorNode = AnchorNode(anchor)
        anchorNode?.setParent(cloudAnchorArFragment?.arSceneView?.scene)
        // 为锚点连接一个可变换的模型
        if(boxOpenRenderable == null){
            showToast("未成功加载宝箱模型")
            return
        }
        // 可以打开的宝箱
        treasurBoxCanOpenNode = Node().apply {
            setParent(anchorNode)
            this.renderable = boxOpenRenderable
            localScale = Vector3(3.0f, 3.0f, 3.0f)
            // 动画部分
            val openAnimData: AnimationData? = boxOpenRenderable?.getAnimationData("treasurebox_open2")
            val modelAnimator = ModelAnimator(openAnimData, boxOpenRenderable)
            setOnTapListener { _, e ->
                // 点击宝箱，播放动画
                if(e.action == MotionEvent.ACTION_UP){
                    if(!isBoxOpened){
                        printLog("Open box: ${modelAnimator.duration}")
                        isBoxOpened = true
                        modelAnimator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                Node().apply {
                                    setParent(treasurBoxCanOpenNode)
                                    localScale = Vector3(0.1f, 0.1f, 0.1f)
                                    this.renderable = boxOpenTextRenderable
                                }
                            }
                        })
                        modelAnimator.start() // 播放动画
                        showToast("打开宝箱！！！")
                        // 保存奖品
                        saveRewardObject()
                    }else{
                        showToast("宝箱已打开。")
                    }
                }
            }
        }
    }

    /**
     * 保存奖品信息
     */
    private fun saveRewardObject() {
        RewardObject(
            BmobUser.getCurrentUser(TaponUser::class.java),
            storeObject
        ).save(object : SaveListener<String>() {
            override fun done(p0: String?, p1: BmobException?) {
                if(p1 == null){
                    showToast(R.string.bmob_save_success_text)
                    finish()
                }else{
                    showToast(R.string.bmob_save_failure_text)
                    printLog("保存失败：$p1")
                }
            }
        })
    }

    // 解析云锚点状态的回调函数类
    private inner class CloudAnchorResolveStateListener(val roomCode: Long,
                                                        val onFind: (Anchor) -> Unit // 找到锚点后的回调函数
    ) : CloudAnchorManager.CloudAnchorResolveListener {

        override fun onCloudTaskComplete(anchor: Anchor) {
            val cloudState = anchor.cloudAnchorState
            if(cloudState.isError) {
                printLog("房间${roomCode}的锚点不能被解析，错误状态是：$cloudState")
                snackbarHelper.showMessageWithDismiss(this@FindTreasureBoxActivity, "解析错误：$cloudState")
                return
            }
            snackbarHelper.showMessageWithDismiss(this@FindTreasureBoxActivity, "成功找到宝箱！")
            // 找到锚点位置
            onFind(anchor)
        }

        override fun onShowResolveMessage() {
            snackbarHelper.setMaxLines(4)
            snackbarHelper.showMessageWithDismiss(this@FindTreasureBoxActivity, "仍然在寻找中。请确保你对准了先前放置宝箱的位置或重进房间")
        }
    }

    /**
     * 检查设备是否符合要求
     */
    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            printLog("Sceneform requires OpenGL ES 3.0 later")
            showToast("Sceneform requires OpenGL ES 3.0 or later")
            activity.finish()
            return false
        }
        return true
    }
}

/*
private fun resetMode() {
        currentMode = HostResolveMode.NONE
        firebaseManager?.clearRoomListener()
        isBoxOpened = false

        if(treasureBox != null){
            anchorNode?.removeChild(treasureBox)
            treasureBox = null
        }
        if(treasurBoxCanOpen != null){
            anchorNode?.removeChild(treasurBoxCanOpen)
            treasurBoxCanOpen = null
            // 重新加载宝箱
            ModelRenderable.builder()
                .setSource(this, Uri.parse("treasurebox_open2.sfb"))
                .build()
                .thenAccept { boxOpenRenderable = it }
                .exceptionally {
                    showToast("无法加载3D模型文件")
                    null
                }
        }
        if(anchorNode != null){
            cloudAnchorArFragment?.arSceneView?.scene?.removeChild(anchorNode)
            anchorNode = null
        }
        setNewAnchor(null)

        snackbarHelper.hide(this)
        cloudManager.clearListeners()
    }
 */
