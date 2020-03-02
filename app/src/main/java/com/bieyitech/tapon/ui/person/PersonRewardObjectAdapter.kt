package com.bieyitech.tapon.ui.person

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bieyitech.tapon.R
import com.bieyitech.tapon.bmob.RewardObject
import com.bieyitech.tapon.databinding.ItemRewardObjectBinding
import com.bieyitech.tapon.helpers.printLog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PersonRewardObjectAdapter(private val context: Context,
                                private val rewardObjectList: MutableList<RewardObject>):
    RecyclerView.Adapter<PersonRewardObjectAdapter.RewardObjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardObjectViewHolder =
        RewardObjectViewHolder(DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.item_reward_object, parent, false))

    override fun getItemCount(): Int = rewardObjectList.size

    override fun onBindViewHolder(holder: RewardObjectViewHolder, position: Int) {
        holder.viewBinding.rewardObject = rewardObjectList[position]    // 数据绑定
        holder.viewBinding.root.setOnClickListener {
            ItemRewardObjectBinding.inflate(LayoutInflater.from(context)).apply {
                rewardObject = rewardObjectList[position]
                rewardObjectIntroTv.isSingleLine = false
                rewardObjectStoreNameTv.isSingleLine = false
            }.let {
                MaterialAlertDialogBuilder(context)
                    .setView(it.root)
                    .show()
            }
        }
        holder.viewBinding.executePendingBindings()
    }

    class RewardObjectViewHolder(val viewBinding: ItemRewardObjectBinding)
        : RecyclerView.ViewHolder(viewBinding.root)
}