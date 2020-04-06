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
 * 放置在商铺中物品（如宝箱），一个商铺可以有很多个物品
 */
data class StoreObject(var user: TaponUser,     // 所属用户
                       var store: Store,        // 所属商铺
                       var objectCode: Int,     // 对应Firebase中存放锚点的代码（roomCode）
                       var name: String,
                       var intro: String,
                       var imgCode: Int         // 奖品图片代码，从0开始
): BmobObject() {

    companion object {
        const val INVALID_OBJECT_CODE = -1 // 无效的奖品代码
    }

    constructor(): this(TaponUser(), Store(), -1, "", "", -1)

    // 删除
    fun deleteStoreObject(context: Context, onSuccess: () -> Unit){
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        delete(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_delete_success_text)
                    onSuccess()
                    context.printLog("删除成功：${this@StoreObject}")
                }else{
                    context.showToast(R.string.bmob_delete_failure_text)
                    context.printLog("删除失败：$p0")
                }
            }
        })
    }

}