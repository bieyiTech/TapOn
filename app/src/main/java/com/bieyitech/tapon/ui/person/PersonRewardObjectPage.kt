package com.bieyitech.tapon.ui.person

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.bieyitech.tapon.bmob.RewardObject
import com.bieyitech.tapon.databinding.PersonTabMyRewardBinding
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast

class PersonRewardObjectPage(private val personFragment: PersonFragment,
                             private val context: Context): BasePersonTabPage() {

    private val mViewBinding = PersonTabMyRewardBinding.inflate(LayoutInflater.from(context))

    init {
        fetchRewardObjects()
        mViewBinding.personRewardObjectSrl.setOnRefreshListener {
            fetchRewardObjects()
        }
    }

    override fun getView(): View = mViewBinding.root

    /**
     * 查询奖品信息并填充
     */
    private fun fetchRewardObjects() {
        mViewBinding.personRewardObjectSrl.isRefreshing = true
        BmobQuery<RewardObject>().apply {
            addWhereEqualTo("user", taponUser)
            include("store")
        }.findObjects(object : FindListener<RewardObject>() {
            override fun done(p0: MutableList<RewardObject>?, p1: BmobException?) {
                mViewBinding.personRewardObjectSrl.isRefreshing = false
                if(p1 == null && p0 != null){
                    mViewBinding.personMyRewardObjectsRv.adapter = PersonRewardObjectAdapter(context, p0)
                }else{
                    context.apply {
                        showToast("未找到奖品")
                        printLog("未找到奖品：$p1")
                    }
                }
            }
        })
    }
}