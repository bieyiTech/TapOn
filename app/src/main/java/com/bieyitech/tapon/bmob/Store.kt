package com.bieyitech.tapon.bmob

import android.content.Context
import cn.bmob.v3.BmobObject
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.bieyitech.tapon.R
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.ui.store.StoreFragment
import com.bieyitech.tapon.widgets.WaitProgressDialog

/**
 * 商铺类，一个用户可以申请创建一个商铺成为商家。
 * 一个用户对应一个商铺
 * 用objectId标识一个商铺，生成二维码
 */
data class Store(var user: TaponUser,   // 所属用户
                 var name: String       // 商铺名称
) : BmobObject() {

    constructor(): this(TaponUser(), "")

    fun saveStore(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context).apply { show() }
        save(object : SaveListener<String>() {
            override fun done(p0: String?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    context.showToast(R.string.bmob_save_success_text)
                    onSuccess()
                    context.printLog("保存成功：${this@Store}")
                }else{
                    context.showToast(R.string.bmob_save_failure_text)
                    context.printLog("保存失败：$p1")
                }
            }
        })
    }

    // 更新
    fun updateStore(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context)
        update(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_update_success_text)
                    onSuccess()
                    context.printLog("更新商铺成功: ${this@Store}")
                }else{
                    context.showToast(R.string.bmob_update_failure_text)
                    context.printLog("更新商铺失败：$p0")
                }
            }
        })
    }

    // 删除
    fun deleteStore(context: Context, onSuccess: () -> Unit){
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        delete(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_delete_success_text)
                    onSuccess()
                    context.printLog("删除成功：${this@Store}")
                }else{
                    context.showToast(R.string.bmob_delete_failure_text)
                    context.printLog("删除失败：$p0")
                }
            }
        })
    }

}