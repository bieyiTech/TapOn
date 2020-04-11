package com.bieyitech.tapon.ui.person

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import cn.bmob.v3.BmobUser
import com.bieyitech.tapon.bmob.TaponUser

/**
 * “我的”页面中Tab标签页的基类
 */
abstract class BasePersonTabPage {

    // 获得缓存的用户信息
    protected val taponUser: TaponUser = BmobUser.getCurrentUser(TaponUser::class.java)

    // 抽象方法，获得根视图，用于ViewPager每项的初始化
    abstract fun getView(): View

    // 刷新页面
    open fun refreshPage(){}

    // 用于懒加载数据
    protected var dataInitialized = false
    fun initData() {
        if(!dataInitialized){
            initDataImpl()
            dataInitialized = true
        }
    }
    open protected fun initDataImpl() {}

}