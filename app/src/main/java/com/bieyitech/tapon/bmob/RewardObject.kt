package com.bieyitech.tapon.bmob

import cn.bmob.v3.BmobObject

/**
 * 顾客持有的奖励（如优惠券）
 */
data class RewardObject(var user: TaponUser,            // 所属用户
                        var storeObject: StoreObject    // 对应的商铺物品
) : BmobObject() {

    constructor(): this(TaponUser(), StoreObject())

}