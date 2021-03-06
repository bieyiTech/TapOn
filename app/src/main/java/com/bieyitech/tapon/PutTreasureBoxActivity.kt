package com.bieyitech.tapon

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.GuardedBy
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Preconditions
import androidx.viewpager.widget.PagerAdapter
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.bmob.TaponUser
import com.bieyitech.tapon.databinding.ActivityPutTreasureBoxBinding
import com.bieyitech.tapon.databinding.ItemPutBoxImgBinding
import com.bieyitech.tapon.helpers.*
import com.bieyitech.tapon.model.RewardModelFactory
import com.bieyitech.tapon.ui.ar.CloudAnchorArFragment
import com.bieyitech.tapon.ui.ar.CloudAnchorManager
import com.bieyitech.tapon.ui.ar.FirebaseManager
import com.bieyitech.tapon.widgets.CoverFlowEffectTransformer
import com.bieyitech.tapon.widgets.ShadeTextView
import com.bieyitech.tapon.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.database.DatabaseError

class PutTreasureBoxActivity : AppCompatActivity() {

    companion object {
        private const val MIN_OPENGL_VERSION = 3.0
        private const val EXTRA_STORE = "Store"

        fun newIntent(context: Context, store: Store) = Intent(context, PutTreasureBoxActivity::class.java)
            .apply {
                putExtra(EXTRA_STORE, store)
            }
    }

    // 状态
    private enum class HostResolveMode { NONE, HOSTING }

    // UI控件——编辑奖品信息界面，放置奖品界面
    private lateinit var viewBinding: ActivityPutTreasureBoxBinding

    // AR相关的Fragment，会话
    private var cloudAnchorArFragment: CloudAnchorArFragment? = null
    private var session: Session? = null

    // 锚点及节点（与模型链接）
    private val anchorLock = Object()
    @GuardedBy("anchorLock")
    private var anchor: Anchor? = null
    private var anchorNode: AnchorNode? = null
    private var treasureBoxNode: TransformableNode? = null
    // 可渲染物体（3D模型）
    private var boxRenderable: ModelRenderable? = null

    // 云锚点相关组件
    private var firebaseManager: FirebaseManager? = null
    private val cloudManager = CloudAnchorManager()
    private var currentMode: HostResolveMode = HostResolveMode.NONE
    private var hostListener: RoomCodeAndCloudAnchorIdListener? = null

    // StoreObject相关
    private lateinit var store: Store
    private val storeObject = StoreObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        viewBinding = ActivityPutTreasureBoxBinding.inflate(LayoutInflater.from(this))
        setContentView(viewBinding.root)

        // 获取店铺
        store = intent.getSerializableExtra(EXTRA_STORE) as Store
        storeObject.store = store
        storeObject.user = BmobUser.getCurrentUser(TaponUser::class.java)
        // 初始化云锚点相关变量
        firebaseManager = FirebaseManager(this)

        setupViews()
    }

    /**
     * 初始化UI控件和事件
     */
    private fun setupViews() {
        viewBinding.putBoxEnterPutBtn.enableOnPressScaleTouchListener {
            if(saveStoreObjectInfo()){
                enterPutMode()
            }
        }
        viewBinding.putBoxUploadBtn.enableOnPressScaleTouchListener {
            onUploadButtonPress()
        }
        viewBinding.putBoxHelpBtn.setOnClickListener {
            // 显示帮助信息
            MaterialAlertDialogBuilder(this)
                .setTitle("提示")
                .setMessage(R.string.put_box_help_text)
                .show()
        }
        viewBinding.putBoxStoreNameTv.text = resources.getString(R.string.put_box_store_name_text, store.name)

        with(viewBinding.putBoxRewardImgVp) {
            adapter = object : PagerAdapter() {
                private val imgList = RewardModelFactory.getRewardImgList()

                override fun isViewFromObject(view: View, `object`: Any): Boolean  = view == `object`

                override fun getCount(): Int = imgList.size

                override fun instantiateItem(container: ViewGroup, position: Int): Any {
                    val view = ItemPutBoxImgBinding.inflate(layoutInflater)
                    view.rewardImg.setImageResource(imgList[position])

                    container.addView(view.root)
                    return view.root
                }
            }
            setPageTransformer(false, CoverFlowEffectTransformer(this@PutTreasureBoxActivity))
        }
    }

    /**
     * 保存StoreObject信息
     */
    private fun saveStoreObjectInfo(): Boolean {
        val name = viewBinding.putBoxObjectNameEt.text.toString()
        val intro = viewBinding.putBoxObjectIntroEt.text.toString()
        if(name.isEmpty() || intro.isEmpty()){
            showToast("名称和内容不能为空")
            return false
        }
        storeObject.apply {
            this.name = name
            this.intro = intro
            this.imgCode = viewBinding.putBoxRewardImgVp.currentItem
        }
        return true
    }

    /**
     * 填写并保存信息后进入放置奖品的AR模式
     */
    private fun enterPutMode() {
        viewBinding.putBoxInfoContainer.toggleVisibility(this) { false }
        viewBinding.putBoxArFragmentContainer.toggleVisibility(this) { true }

        load3DObject()
        setupArFragment()
        setupConfiguration()
    }

    /**
     * 加载3D模型
     */
    private fun load3DObject() {
        ModelRenderable.builder()
            .setSource(this, R.raw.treasurebox)
            .build()
            .thenAccept { boxRenderable = it }
            .exceptionally {
                showToast("无法加载3D模型文件")
                null
            }
    }

    /**
     * 初始化ArFragment
     */
    private fun setupArFragment() {
        cloudAnchorArFragment = CloudAnchorArFragment().apply {
            // 点击平面旋转奖品
            setOnTapArPlaneListener { hitResult, plane, motionEvent ->
                printLog("点击平面")
                if (boxRenderable == null || currentMode != HostResolveMode.HOSTING) {
                    printLog("模型为空或不在托管模式")
                    if (currentMode == HostResolveMode.NONE) {
                        setFakeSnackBarText("配置完成后，才能放置奖品")
                    }
                    return@setOnTapArPlaneListener
                }
                synchronized(anchorLock) {
                    if (anchor == null) {
                        checkState(
                            currentMode == HostResolveMode.HOSTING,
                            "需要在托管模式下才能放置奖品"
                        )
                        printLog("托管模式下，点击平面，anchor != null")
                        // 创建放置奖品的锚点
                        val anchor = hitResult.createAnchor()
                        setNewAnchor(anchor)
                        create3DObjectNode()
                        // 显示托管到云端的按钮
                        viewBinding.putBoxUploadBtn.toggleVisibility(this@PutTreasureBoxActivity){ true }
                    }
                }
            }
            setOnSessionInitializationListener { session ->
                // 为CloudAnchorManager设置会话
                cloudManager.setSession(session)
                this@PutTreasureBoxActivity.session = session
            }
            setOnUpdateListener(object : CloudAnchorArFragment.OnUpdateListener {
                override fun onUpdate(frameTime: FrameTime?) {
                    // Log.d(TAG, "update Frame: ${frameTime?.deltaSeconds}")
                    cloudManager.onUpdate()
                }
            })
        }.also {
            supportFragmentManager.beginTransaction()
                .add(R.id.put_box_ar_fragment, it)
                .commit()
        }
    }

    /**
     * 进行数据库的相关配置，分配一个ID给StoreObject（即云端的roomCode）
     */
    private fun setupConfiguration() {
        // printLog("配置Firebase数据库")
        setFakeSnackBarText("正在进行相关配置。如果长时间没有响应，请确认是否有科学上网。")
        hostListener = RoomCodeAndCloudAnchorIdListener() // 分配房间号的监听器
        firebaseManager?.getNewRoomCode(hostListener)
    }

    /**
     * 托管奖品/物品位置（即云锚点）到云端
     */
    private fun onUploadButtonPress() {
        if (anchor == null) {
            printLog("锚点为空")
            return
        }

        viewBinding.putBoxUploadBtn.text = "上传中......"
        viewBinding.putBoxUploadBtn.isEnabled = false
        treasureBoxNode?.translationController?.isEnabled = false // 不能移动奖品
        // 将锚点托管到云端0
        checkNotNull(hostListener, "托管监听器不能为空")
        cloudManager.hostCloudAnchor(anchor!!, hostListener) // 核心函数
        setFakeSnackBarText("正在上传奖品位置")
    }

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

    private fun create3DObjectNode() {
        if (anchor == null) {
            return
        }
        anchorNode = AnchorNode(anchor)
        anchorNode?.setParent(cloudAnchorArFragment?.arSceneView?.scene)
        // 为锚点连接一个可变换的模型
        treasureBoxNode = TransformableNode(cloudAnchorArFragment?.transformationSystem).apply {
            setParent(anchorNode)
            localScale = Vector3(6.0f, 6.0f, 6.0f)
            this.renderable = boxRenderable
            select()
            setOnTapListener { _, e ->
                // 更新锚点位置
                printLog("点击模型")
                if (e.action == MotionEvent.ACTION_UP) {
                    // 手指提起，改变锚点位置
                    val an = this.parent as AnchorNode?
                    printLog("Anchor: ${an?.anchor?.pose}")
                    setNewAnchor(an?.anchor)
                }
            }
        }
    }

    /**
     * 分配房间号（StoreObject的objectCode）与托管云锚点的回调函数
     */
    private inner class RoomCodeAndCloudAnchorIdListener :
        CloudAnchorManager.CloudAnchorHostListener,
        FirebaseManager.RoomCodeListener {

        private var roomCode: Long? = null
        private var cloudAnchorId: String = ""

        // 托管锚点到云端完成后调用
        override fun onCloudTaskComplete(anchor: Anchor) {
            val cloudState = anchor.cloudAnchorState
            if (cloudState.isError) {
                printLog("托管锚点时发生错误：$cloudState")
                setFakeSnackBarText("托管错误：$cloudState")
                return
            }
            checkState(cloudAnchorId.isEmpty(), "不能预先设置云锚点ID")
            cloudAnchorId = anchor.cloudAnchorId
            checkAndMaybeShare()
        }

        // 获得房间号后调用
        override fun onNewRoomCode(newRoomCode: Long) {
            checkState(roomCode == null, "不能预先设置房间号")
            roomCode = newRoomCode

            // 设置objectCode
            storeObject.objectCode = newRoomCode.toInt()

            setFakeSnackBarText("准备完毕，可以放置一个奖品")
            checkAndMaybeShare()
            // 当接收到可用房间号后，切换到HOSTING状态。
            currentMode = HostResolveMode.HOSTING
        }

        override fun onError(error: DatabaseError?) {
            setFakeSnackBarText("Firebase出现错误：$error")
        }

        // 托管云锚点函数
        private fun checkAndMaybeShare() {
            if (roomCode == null || cloudAnchorId.isEmpty()) {
                return
            }
            firebaseManager?.storeAnchorIdRoom(roomCode!!, cloudAnchorId)
            viewBinding.putBoxUploadBtn.apply {
                setText(R.string.put_box_upload_hint)
                isEnabled = true
                toggleVisibility(this@PutTreasureBoxActivity){ false }
            }
            setFakeSnackBarText("上传成功")
            saveStoreObject()
        }
    }

    /**
     * 保存奖品
     */
    private fun saveStoreObject() {
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        storeObject.save(object : SaveListener<String>() {
            override fun done(p0: String?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    showToast(R.string.bmob_save_success_text)
                    printLog("保存成功")
                    finish()
                }else{
                    showToast(R.string.bmob_save_failure_text)
                    printLog("保存失败：$p1")
                }
            }
        })
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

    private fun setFakeSnackBarText(text: String) {
        viewBinding.putBoxSnackbarTv.toggleVisibility(this){ true }
        viewBinding.putBoxSnackbarTv.text = text
    }
}