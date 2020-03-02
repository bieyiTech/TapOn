package com.bieyitech.tapon.ui.person

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import cn.bmob.v3.BmobUser
import com.bieyitech.tapon.bmob.TaponUser

abstract class BasePersonTabPage {

    protected val taponUser: TaponUser = BmobUser.getCurrentUser(TaponUser::class.java)

    abstract fun getView(): View

}