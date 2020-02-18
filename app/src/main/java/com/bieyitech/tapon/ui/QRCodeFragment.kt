/**
 * @author GizFei
 */
package com.bieyitech.tapon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import cn.bingoogolapple.qrcode.core.BarcodeType
import cn.bingoogolapple.qrcode.core.QRCodeView
import cn.bingoogolapple.qrcode.zxing.ZXingView
import com.bieyitech.tapon.MainActivity
import com.bieyitech.tapon.R
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.helpers.toggleVisibility

/**
 * 扫描二维码的Fragment
 */
class QRCodeFragment : Fragment() {

    private lateinit var qrCodeView: ZXingView
    private lateinit var scanAgainBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_qrcode, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scanAgainBtn = view.findViewById<Button>(R.id.qrcode_scan_again_btn).apply {
            setOnClickListener {
                it.toggleVisibility(requireContext()) { false }
                qrCodeView.startCamera()
                qrCodeView.showScanRect()
                qrCodeView.startSpot()
            }
        }
        qrCodeView = view.findViewById<ZXingView>(R.id.home_zxing_view).apply {
            setType(BarcodeType.ONLY_QR_CODE, mapOf())
            setDelegate(object : QRCodeView.Delegate {
                override fun onScanQRCodeSuccess(result: String?) {
                    context.printLog("扫描结果为：$result")
                    if(result == null){
                        context.showToast("二维码内容为空")
                    }else{
                        // 进入商铺
                        enterStore(result.trim())
                    }
                }
                override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {}
                override fun onScanQRCodeOpenCameraError() { context.printLog("打开相机错误") }
            })
        }
    }

    /**
     * 扫描二维码成功后，进入商铺
     */
    private fun enterStore(storeId: String) {
        qrCodeView.stopCamera() // 关闭相机，画面停留
        // context?.showToast("进入商铺：$storeCode")
        if(activity is MainActivity) {
            (activity as MainActivity).enterStoreState(storeId)
        }else{
            context?.printLog("关联活动不是MainActivity")
        }
    }

    override fun onStart() {
        super.onStart()
        context?.printLog("Fragment onStart")
        qrCodeView.startCamera() // 打开相机
    }

    override fun onResume() {
        super.onResume()
        context?.printLog("Fragment onResume")
        qrCodeView.showScanRect()
        qrCodeView.startSpot() // 开始识别
    }

    override fun onPause() {
        super.onPause()
        context?.printLog("Fragment onPause")
        qrCodeView.stopSpot() // 停止识别
    }

    override fun onStop() {
        super.onStop()
        context?.printLog("Fragment onStop")
        qrCodeView.stopCamera() // 关闭相机
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodeView.onDestroy() // 销毁视图
    }

    // 显示或隐藏时恢复或暂停扫描
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(scanAgainBtn.visibility == View.GONE) {
            if(hidden) {
                onPause()
            }else{
                onResume()
            }
        }
    }
}