package com.bieyitech.tapon.bmob

import android.content.Context
import cn.bmob.v3.BmobObject
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.UpdateListener
import com.bieyitech.tapon.R
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.widgets.WaitProgressDialog

/**
 * 顾客持有的奖励（如优惠券）
 * 因为商家可能删除先前的奖品，所以这里要重新拷贝一份奖品信息
 */
data class RewardObject(var user: TaponUser,            // 所属用户
                        var store: Store,               // 来源商铺
                        var storeObjectId: String,      // 对应奖品objectId
                        var name: String,               // 奖品名称
                        var intro: String,              // 奖品简介
                        var imgCode: Int                // 奖品图片
) : BmobObject() {

    constructor(): this(TaponUser(), Store(), "", "", "", -1)

    // 删除
    fun deleteRewardObject(context: Context, onSuccess: () -> Unit){
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        delete(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_delete_success_text)
                    onSuccess()
                    context.printLog("删除成功：${this@RewardObject}")
                }else{
                    context.showToast(R.string.bmob_delete_failure_text)
                    context.printLog("删除失败：$p0")
                }
            }
        })
    }

}