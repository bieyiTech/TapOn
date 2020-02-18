package com.bieyitech.tapon.bmob

import cn.bmob.v3.BmobObject

/**
 * 放置在商铺中物品（如宝箱），一个商铺可以有很多个物品
 */
data class StoreObject(var user: TaponUser,     // 所属用户
                       var store: Store,        // 所属商铺
                       var objectCode: Int,     // 对应Firebase中存放锚点的代码（roomCode）
                       var name: String,
                       var intro: String
): BmobObject() {

    companion object {
        const val INVALID_OBJECT_CODE = -1 // 无效的奖品代码
    }

    constructor(): this(TaponUser(), Store(), -1, "", "")

}