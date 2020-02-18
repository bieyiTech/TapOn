package com.bieyitech.tapon.bmob

import androidx.annotation.IntRange
import cn.bmob.v3.BmobUser

data class TaponUser(@IntRange(from = 0, to = 1) var type: Int = UserType.CUSTOMER.ordinal, // 用户类型，顾客或商家
                     var nickname: String = "",      // 昵称，用户名在注册时确定，不能修改，昵称可以修改
                     var introduction: String = "添加个人简介",
                     var avatarUrl: String = "" // 头像链接
): BmobUser() {

    enum class UserType {
        CUSTOMER,   // 顾客，0
        MERCHANT    // 商家，1
    }

    // 是否为顾客
    fun isCustomer() = type == UserType.CUSTOMER.ordinal

    // 是否为商家
    fun isMerchant() = type == UserType.MERCHANT.ordinal

}