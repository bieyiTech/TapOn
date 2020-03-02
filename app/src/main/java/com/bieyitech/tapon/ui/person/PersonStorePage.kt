package com.bieyitech.tapon.ui.person

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.UpdateListener
import com.bieyitech.tapon.PutTreasureBoxActivity
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.bmob.StoreObject
import com.bieyitech.tapon.bmob.TaponUser
import com.bieyitech.tapon.databinding.PersonTabStoreBinding
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showLongToast
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.widgets.InputTextDialog
import com.bieyitech.tapon.widgets.WaitProgressDialog
import com.giz.android.toolkit.drawable2Bitmap
import java.io.File
import java.lang.Exception

class PersonStorePage(private val personFragment: PersonFragment,
                      private val context: Context): BasePersonTabPage(){

    private val mViewBinding = PersonTabStoreBinding.inflate(LayoutInflater.from(context))

    // 所持商铺
    private lateinit var mStore: Store

    init {
        fetchStoreInfo()

        mViewBinding.personCreateStoreBtn.enableOnPressScaleTouchListener {
            createStore()
        }
        mViewBinding.personDecorateStoreBtn.enableOnPressScaleTouchListener {
            decorateStore()
        }
        mViewBinding.personPutBoxBtn.enableOnPressScaleTouchListener {
            // 放置宝箱
            context.startActivity(PutTreasureBoxActivity.newIntent(context, mStore))
        }
        mViewBinding.personStoreQrcode.setOnLongClickListener {
            saveStoreQrCodeImg()
            true
        }
    }

    override fun getView(): View = mViewBinding.root

    /**
     * 获取商铺信息
     */
    private fun fetchStoreInfo() {
        val waitProgressDialog = WaitProgressDialog(context).apply { show() }
        val storeQuery = BmobQuery<Store>()
        storeQuery.addWhereEqualTo("user", taponUser)
        storeQuery.findObjects(object : FindListener<Store>() {
            override fun done(p0: MutableList<Store>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null && p0 != null && p0.isNotEmpty()){
                    if(p0.size > 1){
                        context.showToast("该用户商铺数量大于1")
                        context.printLog("该用户商铺数量大于1")
                    }
                    mStore = p0[0]
                    context.printLog("商铺名称：${mStore.name}")
                    fillStoreInfo()
                }else{
                    context.showToast("获取商铺信息失败")
                    context.printLog("获取商铺信息失败：$p1")
                }
            }
        })
    }

    /**
     * 填充商铺信息
     */
    private fun fillStoreInfo() {
        if(::mStore.isInitialized){
            mStore.apply {
                if(taponUser.isMerchant()) {
                    mViewBinding.personStoreInfoContainer.visibility = View.VISIBLE
                    mViewBinding.personCreateStoreBtn.visibility = View.GONE
                    mViewBinding.personDecorateStoreBtn.visibility = View.VISIBLE

                    mViewBinding.personStoreNameTv.text = this.name
                    // 生成店铺二维码，如果已经保存了，则使用缓存文件
                    val qrCodeFile = getStoreQRCodeFile()
                    if(qrCodeFile.exists()){
                        context.printLog("从手机文件中获取二维码")
                        mViewBinding.personStoreQrcode.setImageBitmap(BitmapFactory.decodeFile(qrCodeFile.path))
                    }else{
                        Thread(Runnable {
                            QRCodeEncoder.syncEncodeQRCode(this.objectId, 300).let {
                                setQrCodeImg(it)
                            }
                        }).start()
                    }
                }else{
                    mViewBinding.personStoreInfoContainer.visibility = View.GONE
                    mViewBinding.personCreateStoreBtn.visibility = View.VISIBLE
                    mViewBinding.personDecorateStoreBtn.visibility = View.GONE
                }
            }
        }else{
            context.showToast("没有获取到商铺信息")
        }
    }

    private fun setQrCodeImg(bp: Bitmap) {
        mViewBinding.personStoreQrcode.setImageBitmap(bp)
    }

    /**
     * 创建商铺
     */
    private fun createStore() {
        InputTextDialog.Builder(context)
            .title("输入商铺名")
            .onTextInputed {
                Store(
                    taponUser,
                    it
                ).saveStore(context){
                    // 更新用户信息
                    val waitProgressDialog = WaitProgressDialog(context).apply { show() }
                    taponUser.type = TaponUser.UserType.MERCHANT.ordinal
                    taponUser.update(object : UpdateListener() {
                        override fun done(p0: BmobException?) {
                            waitProgressDialog.dismiss()
                            if(p0 == null){
                                personFragment.fillUserInfo()
                                fetchStoreInfo()
                            }else{
                                context.showToast("更新用户信息失败")
                                context.printLog("更新用户信息失败")
                            }
                        }
                    })
                }
            }.show()
    }

    private fun decorateStore() {

    }

    /**
     * 保存商铺二维码图片
     */
    private fun saveStoreQrCodeImg() {
        val bitmap = drawable2Bitmap(mViewBinding.personStoreQrcode.drawable)
        if(bitmap == null){
            context.apply {
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
            context.apply {
                showLongToast("成功保存二维码至：${file.path}")
                printLog("保存成功，图片路径：${file.path}")
            }
        }catch (e: Exception) {
            context.apply {
                showToast("保存商铺二维码图片错误")
                printLog("保存商铺二维码图片错误：$e")
            }
        }
    }

    // 获得存储商铺二维码图片的文件路径
    // private fun getStoreQRCodeFile() = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    //      "${mStore?.name}_store_qrcode.png")
    // 存到emulated/0/Android/media/com.bieyitech.tapon中
    private fun getStoreQRCodeFile() = File(context.externalMediaDirs[0],
        "${mStore.name}_store_qrcode.png")
}