package com.bieyitech.tapon.bmob

import cn.bmob.v3.BmobObject

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

}