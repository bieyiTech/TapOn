package com.bieyitech.tapon.ui.person

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.UpdateListener
import com.bieyitech.tapon.PutTreasureBoxActivity
import com.bieyitech.tapon.bmob.Store
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
import kotlin.math.max

/**
 * “我的”页面中的“商铺”标签页内容
 */
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
            // 保存二维码
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
                }else{
                    // context.showToast("获取商铺信息失败")
                    context.printLog("获取商铺信息失败：$p1")
                }
                fillStoreInfo()
            }
        })
    }

    /**
     * 填充商铺信息
     */
    private fun fillStoreInfo() {
        mViewBinding.taponUser = taponUser
        if(taponUser.isMerchant()) {
            if (::mStore.isInitialized) {
                mStore.apply {
                    mViewBinding.personStoreNameTv.text = this.name
                    // 生成店铺二维码，如果已经保存了，则使用缓存文件
                    val qrCodeFile = getCachedStoreQRCodeFile()
                    if (qrCodeFile.exists()) {
                        // context.printLog("从手机文件中获取二维码")
                        mViewBinding.personStoreQrcode.setImageBitmap(
                            BitmapFactory.decodeFile(
                                qrCodeFile.path
                            )
                        )
                    } else {
                        Thread(Runnable {
                            QRCodeEncoder.syncEncodeQRCode(this.objectId, 300).let {
                                saveCacheQrCodeImg(it)
                                personFragment.activity?.runOnUiThread {
                                    // 回到主线程设置二维码图片
                                    mViewBinding.personStoreQrcode.setImageBitmap(it)
                                }
                            }
                        }).start()
                    }
                }
            } else {
                mViewBinding.personStoreInfoContainer.visibility = View.GONE
                context.showToast("没有获取到商铺信息")
            }
        }
    }

    /**
     * 创建商铺
     */
    private fun createStore() {
        InputTextDialog.Builder(context)
            .title("输入商铺名")
            .maxLength(10)  // 最大长度为10
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
     * 缓存商铺二维码图片
     * @param bp Bitmap
     */
    private fun saveCacheQrCodeImg(bp: Bitmap) {
        val file = getCachedStoreQRCodeFile()
        try {
            if(!file.exists()){
                file.createNewFile()
            }
            val outputStream = file.outputStream()
            bp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            context.apply {
                printLog("保存成功，缓存图片路径：${file.path}")
            }
        }catch (e: Exception) {
            context.apply {
                printLog("保存缓存商铺二维码图片错误：$e")
            }
        }
    }

    /**
     * 保存商铺二维码图片，包含商铺名称。
     */
    private fun saveStoreQrCodeImg() {
        val qrCodeBitmap = drawable2Bitmap(mViewBinding.personStoreQrcode.drawable)
        if(qrCodeBitmap == null){
            context.apply {
                showToast("保存失败")
                printLog("保存失败，drawable为空")
            }
            return
        }

        // 创建文字笔刷，并测量文字宽高
        val textPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
            color = Color.BLACK
            textSize = 24f
        }
        val textRect = Rect()
        textPaint.getTextBounds(mStore.name, 0, mStore.name.length, textRect)
        context.printLog("保存二维码。宽高：${qrCodeBitmap.width}, ${qrCodeBitmap.height}。文字宽高：${textRect.width()}, ${textRect.height()}")

        // 新建空白图片
        val saveImg = Bitmap.createBitmap(
            max(textRect.width(), qrCodeBitmap.width) + 20,
            textRect.height() + qrCodeBitmap.height + 48,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(saveImg)
        // 绘制白色背景
        canvas.drawARGB(255, 255, 255, 255)
        // 绘制二维码
        canvas.drawBitmap(
            qrCodeBitmap,
            (saveImg.width - qrCodeBitmap.width) / 2f,
            16f,
            Paint()
        )
        // 绘制店铺文字
        canvas.drawText(
            mStore.name,
            (saveImg.width - textRect.width()) / 2.0f,
            32f + qrCodeBitmap.height + textRect.height(),
            textPaint
        )

        // 存入文件
        val file = getSavedStoreQRCodeFile()
        try {
            if(!file.exists()){
                file.createNewFile()
            }
            val outputStream = file.outputStream()
            saveImg.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
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
    // 存到/storage/emulated/0/Android/data/com.bieyitech.tapon/cache/中
    private fun getCachedStoreQRCodeFile() = File(context.externalCacheDir,
        "${mStore.name}_store_qrcode.png")

    // 获得保存商铺信息（二维码+商铺名称）图片的文件路径
    // 存到emulated/0/Android/media/com.bieyitech.tapon中
    private fun getSavedStoreQRCodeFile() = File(context.externalMediaDirs[0],
        "${mStore.name}_二维码信息.png")
}