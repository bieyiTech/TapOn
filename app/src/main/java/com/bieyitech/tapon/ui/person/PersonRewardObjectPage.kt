package com.bieyitech.tapon.ui.person

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.RewardObject
import com.bieyitech.tapon.data.CommonAdapterWithDataBinding
import com.bieyitech.tapon.databinding.ItemRewardObjectBinding
import com.bieyitech.tapon.databinding.PersonTabMyRewardBinding
import com.bieyitech.tapon.helpers.printLog
import com.bieyitech.tapon.helpers.showToast
import com.bieyitech.tapon.model.RewardModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * “我的”页面中的“我的奖品”标签页内容
 */
class PersonRewardObjectPage(private val personFragment: PersonFragment,
                             private val context: Context): BasePersonTabPage() {

    private val mViewBinding = PersonTabMyRewardBinding.inflate(LayoutInflater.from(context))

    init {
        fetchRewardObjects()
        mViewBinding.personRewardObjectSrl.setOnRefreshListener {// 下拉刷新
            fetchRewardObjects()
        }
    }

    override fun getView(): View = mViewBinding.root

    override fun refreshPage() {
        fetchRewardObjects()
    }

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
                    fillRewardObjectsContent(p0)
                }else{
                    fillRewardObjectsContent(mutableListOf())
                    context.apply {
                        showToast(if(p1?.errorCode == 9016) "网络不可用" else "未找到奖品")
                        printLog("未找到奖品：$p1")
                    }
                }
            }
        })
    }

    /**
     * 填充奖品列表
     */
    private fun fillRewardObjectsContent(objectList: MutableList<RewardObject>) {
        mViewBinding.personMyRewardObjectsRv.adapter = object : CommonAdapterWithDataBinding<RewardObject>(
            context, objectList, R.layout.item_reward_object
        ){
            override fun bindData(
                holder: CommonViewHolderWithDataBinding,
                data: RewardObject,
                position: Int
            ) {
                with(holder.viewBinding as ItemRewardObjectBinding){
                    rewardObject = data    // 数据绑定
                    rewardObjectImg.setImageResource(RewardModelFactory.getRewardImg(data.imgCode))

                    root.setOnClickListener {
                        ItemRewardObjectBinding.inflate(LayoutInflater.from(context)).apply {
                            rewardObject = data
                            rewardObjectIntroTv.isSingleLine = false
                            rewardObjectStoreNameTv.isSingleLine = false
                        }.let {
                            MaterialAlertDialogBuilder(context)
                                .setView(it.root)
                                .show()
                        }
                    }
                    root.setOnLongClickListener {
                        // 长按删除
                        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                            .setTitle("确认删除奖品[${data.name}]吗？")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok){ _, _ ->
                                data.deleteRewardObject(context){
                                    removeItem(data)
                                }
                            }.show()
                        true
                    }
                }
            }
        }
    }
}