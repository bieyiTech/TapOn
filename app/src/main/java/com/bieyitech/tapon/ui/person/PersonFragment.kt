/**
 * @author GizFei
 */
package com.bieyitech.tapon.ui.person

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.UpdateListener
import com.bieyitech.tapon.LoginActivity
import com.bieyitech.tapon.PutTreasureBoxActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.RewardObject
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.bmob.TaponUser
import com.bieyitech.tapon.data.CommonAdapter
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showLongToast
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.widgets.InputTextDialog
import com.bieyitech.tapon.widgets.ShadeImageButton
import com.bieyitech.tapon.widgets.ShadeTextView
import com.bieyitech.tapon.widgets.WaitProgressDialog
import com.giz.android.toolkit.drawable2Bitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.lang.Exception
import kotlin.math.log

class PersonFragment : Fragment() {

    // 当前用户
    private val taponUser by lazy { BmobUser.getCurrentUser(TaponUser::class.java) }
    // 所持商铺
    private var mStore: Store? = null

    // UI组件
    private lateinit var createStoreBtn: ShadeTextView
    private lateinit var decorateStoreBtn: ShadeTextView
    private lateinit var putBoxBtn: ShadeTextView
    private lateinit var logoutBtn: ShadeImageButton
    private lateinit var userNicknameTv: TextView
    private lateinit var userTypeTv: TextView
    private lateinit var storeInfoContainer: LinearLayout
    private lateinit var storeNameTv: TextView
    private lateinit var storeQrCodeImg: ImageView
    private lateinit var myRewardsTitle: ShadeTextView
    private lateinit var rewardObjectsRv: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_person, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        createStoreBtn = view.findViewById(R.id.person_create_store_btn)
        decorateStoreBtn = view.findViewById(R.id.person_decorate_store_btn)
        putBoxBtn = view.findViewById(R.id.person_put_box_btn)
        logoutBtn = view.findViewById(R.id.person_logout_btn)
        userNicknameTv = view.findViewById(R.id.person_nickname_tv)
        userTypeTv = view.findViewById(R.id.person_user_type_tv)
        storeInfoContainer = view.findViewById(R.id.person_store_info_container)
        storeNameTv = view.findViewById(R.id.person_store_name_tv)
        storeQrCodeImg = view.findViewById(R.id.person_store_qrcode)
        myRewardsTitle = view.findViewById(R.id.person_my_rewards_title)
        rewardObjectsRv = view.findViewById(R.id.person_reward_objects_rv)

        setLayoutVisibility()
        setupButtonClickListeners()
        fillUserInfo()
    }

    /**
     * 根据用户类型设置启用哪些按钮，展示哪些布局
     */
    private fun setLayoutVisibility() {
        if(taponUser.isCustomer()){ // 顾客
            createStoreBtn.visibility = View.VISIBLE
            decorateStoreBtn.visibility = View.GONE
            putBoxBtn.visibility = View.GONE
        }else{  // 商家
            createStoreBtn.visibility = View.GONE
            decorateStoreBtn.visibility = View.VISIBLE
            putBoxBtn.visibility = View.VISIBLE
        }
    }

    /**
     * 为按钮设置点击事件
     */
    private fun setupButtonClickListeners() {
        createStoreBtn.enableOnPressScaleTouchListener { createStore() }
        decorateStoreBtn.enableOnPressScaleTouchListener { decorateStore() }
        putBoxBtn.enableOnPressScaleTouchListener { putOneBox() }
        storeQrCodeImg.setOnLongClickListener {
            saveStoreQrCodeImg()
            true
        }
        logoutBtn.setOnClickListener {
            // 登出
            MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialog)
                .setTitle("确认退出登录吗？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok){ _, _ ->
                    BmobUser.logOut()
                    startActivity(LoginActivity.newIntent(requireContext()))
                    requireActivity().finish()
                }.show()
        }
    }

    /**
     * 填充用户信息并获取奖品信息、商铺信息
     */
    private fun fillUserInfo() {
        taponUser.apply {
            userNicknameTv.text = nickname
            userTypeTv.text = if(isCustomer()) "顾客" else "商家"
        }
        fetchRewardObjects()
        if(taponUser.isMerchant()) {
            fetchStoreInfo()
        }
    }

    /**
     * 查询奖品信息
     */
    private fun fetchRewardObjects() {
        BmobQuery<RewardObject>().apply {
            addWhereEqualTo("user", taponUser)
            include("storeObject,storeObject.store")
        }.findObjects(object : FindListener<RewardObject>() {
            override fun done(p0: MutableList<RewardObject>?, p1: BmobException?) {
                if(p1 == null && p0 != null){
                    fillRewardObjectsContent(p0)
                }else{
                    context?.apply {
                        showToast("未找到奖品")
                        printLog("未找到奖品：$p1")
                    }
                }
            }
        })
    }

    /**
     * 填充奖品信息
     */
    private fun fillRewardObjectsContent(rewardList: MutableList<RewardObject>) {
        // 数量
        myRewardsTitle.text = resources.getString(R.string.person_my_reward_hint, rewardList.size)
        // 列表
        rewardObjectsRv.adapter = object : CommonAdapter<RewardObject>(requireContext(), rewardList, R.layout.item_reward_object) {
            override fun bindData(holder: CommonViewHolder, data: RewardObject, position: Int) {
                with(holder){
                    setText(R.id.reward_object_name_tv, data.storeObject.name)
                    setText(R.id.reward_object_intro_tv, data.storeObject.intro)
                    setText(R.id.reward_object_store_name_tv, resources.getString(R.string.reward_object_store_name_hint,
                        data.storeObject.store.name))
                }
            }
        }
    }

    /**
     * 获取商铺信息
     */
    private fun fetchStoreInfo() {
        val waitProgressDialog = WaitProgressDialog(requireContext()).apply { show() }
        val storeQuery = BmobQuery<Store>()
        storeQuery.addWhereEqualTo("user", taponUser)
        storeQuery.findObjects(object : FindListener<Store>() {
            override fun done(p0: MutableList<Store>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null && p0 != null && p0.isNotEmpty()){
                    if(p0.size > 1){
                        context?.showToast("该用户商铺数量大于1")
                        context?.printLog("该用户商铺数量大于1")
                    }
                    mStore = p0[0]
                    fillStoreInfo()
                }else{
                    context?.showToast("获取商铺信息失败")
                    context?.printLog("获取商铺信息失败：$p1")
                }
            }
        })
    }

    /**
     * 填充商铺信息
     */
    private fun fillStoreInfo() {
        mStore?.apply {
            if(taponUser.isMerchant()) {
                storeInfoContainer.visibility = View.VISIBLE
                storeNameTv.text = this.name
                // 生成店铺二维码，如果已经保存了，则使用缓存文件
                val qrCodeFile = getStoreQRCodeFile()
                if(qrCodeFile.exists()){
                    context?.printLog("从手机文件中获取二维码")
                    storeQrCodeImg.setImageBitmap(BitmapFactory.decodeFile(qrCodeFile.path))
                }else{
                    Thread(Runnable {
                        QRCodeEncoder.syncEncodeQRCode(this.objectId, 300).let {
                            requireActivity().runOnUiThread {
                                storeQrCodeImg.setImageBitmap(it)
                            }
                        }
                    }).start()
                }
            }else{
                storeInfoContainer.visibility = View.GONE
            }
        }
    }

    /**
     * 创建商铺
     */
    private fun createStore() {
        InputTextDialog.Builder(requireContext())
            .title("输入商铺名")
            .onTextInputed {
                Store(
                    taponUser,
                    it
                ).saveStore(requireContext()){
                    // 更新用户信息
                    val waitProgressDialog = WaitProgressDialog(requireContext()).apply { show() }
                    taponUser.type = TaponUser.UserType.MERCHANT.ordinal
                    taponUser.update(object : UpdateListener() {
                        override fun done(p0: BmobException?) {
                            waitProgressDialog.dismiss()
                            if(p0 == null){
                                fillUserInfo()
                                setLayoutVisibility()
                                fetchStoreInfo()
                            }else{
                                context?.showToast("更新用户信息失败")
                                context?.printLog("更新用户信息失败")
                            }
                        }
                    })
                }
            }.show()
    }

    private fun decorateStore() {

    }

    /**
     * 放置宝箱
     */
    private fun putOneBox() {
        startActivity(PutTreasureBoxActivity.newIntent(requireContext(), mStore!!))
    }

    /**
     * 保存商铺二维码图片
     */
    private fun saveStoreQrCodeImg() {
        val bitmap = drawable2Bitmap(storeQrCodeImg.drawable)
        if(bitmap == null){
            context?.apply {
                showToast("保存失败")
                printLog("保存失败，drawable为空")
            }
            return
        }
        val file = getStoreQRCodeFile()
        try {
            if(!file.exists()){
                file.createNewFile()
            }
            val outputStream = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            context?.apply {
                showLongToast("成功保存二维码至：${file.path}")
                printLog("保存成功，图片路径：${file.path}")
            }
        }catch (e: Exception) {
            context?.apply {
                showToast("保存商铺二维码图片错误")
                printLog("保存商铺二维码图片错误：$e")
            }
        }
    }
    // 获得存储商铺二维码图片的文件路径
    // private fun getStoreQRCodeFile() = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    //      "${mStore?.name}_store_qrcode.png")
    // 存到emulated/0/Android/media/com.bieyitech.tapon中
    private fun getStoreQRCodeFile() = File(requireContext().externalMediaDirs[0],
        "${mStore?.name}_store_qrcode.png")
}