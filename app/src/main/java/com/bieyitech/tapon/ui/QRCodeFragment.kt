/**
 * @author GizFei
 */
package com.bieyitech.tapon.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import cn.bingoogolapple.qrcode.core.BarcodeType
import cn.bingoogolapple.qrcode.core.QRCodeView
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.QueryListener
import com.bieyitech.tapon.MainActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.Store
import com.bieyitech.tapon.databinding.FragmentQrcodeBinding
import com.bieyitech.tapon.helpers.FileUriUtils
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.helpers.toggleVisibility
import com.bieyitech.tapon.widgets.WaitProgressDialog

/**
 * 扫描二维码的Fragment
 */
class QRCodeFragment : Fragment() {

    private var _binding: FragmentQrcodeBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentQrcodeBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding.qrcodeScanAgainBtn.enableOnPressScaleTouchListener {
            viewBinding.qrcodeScanAgainBtn.toggleVisibility(requireContext()){ false }
            // 重新开始扫描
            viewBinding.qrcodeZxingView.apply {
                startCamera()
                showScanRect()
                startSpot()
            }
        }
        viewBinding.qrcodeFromFileBtn.setOnClickListener {
            // 从文件夹中选择图片
            selectImgFromFile()
        }
        viewBinding.qrcodeZxingView.apply {
            setType(BarcodeType.ONLY_QR_CODE, mapOf())
            setDelegate(object : QRCodeView.Delegate {
                override fun onScanQRCodeSuccess(result: String?) {
                    context.printLog("扫描结果为：$result")
                    if(result == null){
                        context.showToast("未找到二维码")
                    }else{
                        // 查询商铺信息
                        viewBinding.qrcodeZxingView.stopCamera()
                        queryStore(result.trim())
                    }
                }
                override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {}
                override fun onScanQRCodeOpenCameraError() { context.printLog("打开相机错误") }
            })
        }
    }

    /**
     * 查询商铺信息
     * @param storeId 商铺代码
     */
    private fun queryStore(storeId: String){
        val waitProgressDialog = WaitProgressDialog(requireContext()).apply { show() }
        BmobQuery<Store>().getObject(storeId, object : QueryListener<Store>() {
            override fun done(p0: Store?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null && p0 != null){
                    enterStore(p0)
                }else{
                    context?.printLog("查询商铺错误：$p1")
                    context?.showToast(if(p1?.errorCode == 9016) "网络不可用" else "无效的二维码信息")
                    viewBinding.qrcodeScanAgainBtn.toggleVisibility(requireContext()){ true }
                }
            }
        })
    }

    /**
     * 扫描二维码成功并查询到商铺信息，则进入商铺
     */
    private fun enterStore(store: Store) {
        viewBinding.qrcodeZxingView.stopCamera() // 关闭相机，画面停留
        // context?.showToast("进入商铺：$storeCode")
        if(activity is MainActivity) {
            (activity as MainActivity).enterStoreState(store)
        }else{
            context?.printLog("关联活动不是MainActivity")
        }
    }

    /**
     * 从文件夹中选择图片
     */
    private fun selectImgFromFile() {
        // 选择“文件管理”后默认打开/storage/emulated/0/Android/media/com.bieyitech.tapon/文件夹
        val uri = Uri.parse(requireContext().externalMediaDirs[0].path)
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(uri, "image/*")
        startActivityForResult(Intent.createChooser(intent, "选择二维码图片"), 1)
    }

    // 返回图片结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == AppCompatActivity.RESULT_OK){
            if(requestCode == 1){
                val path = FileUriUtils.getFilePathByUri(requireContext(), data?.data)
                if(path != null){
                    context?.printLog("图片路径：$path")
                    viewBinding.qrcodeZxingView.decodeQRCode(path)
                }else{
                    context?.showToast("未获取到图片")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        context?.printLog("Fragment onStart")
        viewBinding.qrcodeZxingView.startCamera() // 打开相机
    }

    override fun onResume() {
        super.onResume()
        context?.printLog("Fragment onResume")
        viewBinding.qrcodeZxingView.showScanRect()
        viewBinding.qrcodeZxingView.startSpot() // 开始识别
    }

    override fun onPause() {
        super.onPause()
        context?.printLog("Fragment onPause")
        viewBinding.qrcodeZxingView.stopSpot() // 停止识别
    }

    override fun onStop() {
        super.onStop()
        context?.printLog("Fragment onStop")
        viewBinding.qrcodeZxingView.stopCamera() // 关闭相机
    }

    // 显示或隐藏时恢复或暂停扫描
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(viewBinding.qrcodeScanAgainBtn.visibility == View.GONE) {
            if(hidden) {
                onPause()
            }else{
                onResume()
            }
        }
    }
}